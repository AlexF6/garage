package io.github.alexf6.garage;

import io.github.alexf6.garage.physics.CarInput;
import io.github.alexf6.garage.physics.CarPhysics;
import io.github.alexf6.garage.physics.CarTuning;
import io.github.alexf6.garage.physics.Suspension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Vehículo motorizado base y Jeep Willys de 4 asientos. Sigue el patrón del bote vanilla: el cliente del
 * conductor calcula la física y el servidor recibe la posición; el combustible y el color viven en el
 * servidor y se sincronizan. Las variantes ({@link MotorcycleEntity}, {@link MotocarroEntity}) heredan el
 * contrato completo (colisión, inventario, combustible, atropellos, red y desmonte) y solo parametrizan
 * su tuning, asientos, baúl, color y bocina.
 */
public class CarEntity extends VehicleEntity implements ContainerEntity, HasCustomInventoryScreen {

    public static final int SEATS = 4;
    public static final int DEFAULT_COLOR = 0xC82127; // rojo Willys
    private static final int CONTAINER_ROWS = 3;

    private static final float WHEEL_RADIUS = 0.375F;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final int FUEL_HUD_INTERVAL = 40; // la action bar dura ~3 s; se reenvía antes o si cambia

    private static final EntityDataAccessor<Integer> DATA_FUEL =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);

    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(containerRows() * 9, ItemStack.EMPTY);
    private ResourceKey<LootTable> lootTable;
    private long lootTableSeed;

    private CarInput input = CarInput.NONE;
    private double steerAngle; // ángulo real de dirección (solo en quien simula)
    private float deltaRotation;
    private int lastFuelPercent = -1;
    private int hornCooldown;

    // Animación de ruedas (solo visual): rodado en radianes y ángulo de giro de las delanteras
    private float wheelRoll;
    private float wheelRollO;
    private float steer;
    private float steerO;
    private double forwardSpeed; // observada (bloques/tick); la usan el sonido y la suspensión
    private final Suspension suspension = new Suspension();

    private Vec3 lastServerPos; // en el servidor el conductor manda la posición: la velocidad sale de aquí

    public CarEntity(EntityType<? extends CarEntity> type, Level level) {
        super(type, level);
    }

    // --- Parámetros de cada variante -----------------------------------------------------

    public CarTuning tuning() {
        return CarConfig.get().tuning();
    }

    protected int seats() {
        return SEATS;
    }

    public int defaultColor() {
        return DEFAULT_COLOR;
    }

    protected int containerRows() {
        return CONTAINER_ROWS;
    }

    protected float wheelRadius() {
        return WHEEL_RADIUS;
    }

    protected float headTurnLimit() {
        return 105.0F;
    }

    /** Grados por tick que el vehículo puede rotar en el aire o casi detenido al pulsar A/D (p. ej. en la moto). */
    protected float directTurnRate() {
        return 1.2F;
    }

    public float idleEnginePitch() {
        return 0.5F;
    }

    public float topEnginePitch() {
        return 1.4F;
    }

    // --- Datos sincronizados -------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FUEL, 0);
        builder.define(DATA_COLOR, defaultColor());
    }

    public int getFuel() {
        return this.entityData.get(DATA_FUEL);
    }

    public void setFuel(int fuel) {
        this.entityData.set(DATA_FUEL, Mth.clamp(fuel, 0, CarConfig.get().maxFuel()));
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setColor(int rgb) {
        this.entityData.set(DATA_COLOR, rgb & 0xFFFFFF);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        addChestVehicleSaveData(output);
        output.putInt("Fuel", getFuel());
        output.putInt("Color", getColor());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        readChestVehicleSaveData(input);
        setFuel(input.getIntOr("Fuel", 0));
        setColor(input.getIntOr("Color", defaultColor()));
    }

    // --- Entrada del conductor (la fija el cliente, ver GarageClient) ------------------------

    public void setInput(CarInput input) {
        this.input = input;
    }

    // --- Tick y física ---------------------------------------------------------------------

    @Override
    public void tick() {
        if (getHurtTime() > 0) setHurtTime(getHurtTime() - 1);
        if (getDamage() > 0.0F) setDamage(getDamage() - 1.0F);
        if (hornCooldown > 0) hornCooldown--;

        super.tick();

        LivingEntity driver = getControllingPassenger();
        if (isLocalInstanceAuthoritative()) {
            if (driver == null) input = CarInput.NONE;
            drive();
        }
        // Después de moverse: si no, en el cliente del conductor el giro y la distancia del tick valen 0
        deltaRotation = Mth.wrapDegrees(getYRot() - this.yRotO);
        tickVisuals();

        if (level() instanceof ServerLevel serverLevel) {
            serverTick(serverLevel, driver);
        }
    }

    private void drive() {
        CarTuning tuning = tuning();
        Vec3 motion = getDeltaMovement();
        boolean onGround = onGround();
        boolean inWater = isInWater();

        // Primero gira con la velocidad que traía; luego la velocidad se descompone en el rumbo nuevo, así
        // lo que el carro no alcanzó a girar queda como velocidad lateral (derrape) y las llantas la absorben
        double speed = forwardOf(motion.x, motion.z, getYRot());
        steerAngle = CarPhysics.nextSteer(steerAngle, input, speed, tuning);
        float yawStep = CarPhysics.yawDelta(speed, steerAngle, onGround && input.brake(), tuning);
        if (!onGround) {
            yawStep = yawStep * 0.75F + input.steering() * directTurnRate();
        } else if (Math.abs(speed) < 0.08 && input.steering() != 0) {
            yawStep += input.steering() * directTurnRate();
        }
        setYRot(getYRot() + yawStep);

        double yawRad = Math.toRadians(getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double forward = -motion.x * sin + motion.z * cos;
        double lateral = motion.x * cos + motion.z * sin;
        forward = CarPhysics.nextSpeed(forward, input, getFuel() > 0, onGround, inWater, tuning);
        lateral = CarPhysics.nextLateral(lateral, input, onGround, inWater, tuning);

        setDeltaMovement(-sin * forward + cos * lateral, inWater ? -0.04 : motion.y, cos * forward + sin * lateral);
        applyGravity();
        move(MoverType.SELF, getDeltaMovement());
    }

    /** Componente de un desplazamiento horizontal en la dirección a la que mira el vehículo. */
    private static double forwardOf(double dx, double dz, float yawDeg) {
        double yawRad = Math.toRadians(yawDeg);
        return -dx * Math.sin(yawRad) + dz * Math.cos(yawRad);
    }

    /**
     * Animación a partir del movimiento real (sirve igual en el conductor, en otros jugadores y con
     * interpolación): rodado = distancia / radio; dirección = inversa del modelo de giro; suspensión según
     * las aceleraciones observadas.
     */
    protected void tickVisuals() {
        CarTuning tuning = tuning();
        double forward = forwardOf(getX() - this.xo, getZ() - this.zo, getYRot());
        double longAccel = forward - this.forwardSpeed;
        this.forwardSpeed = forward;

        if (level().isClientSide()) {
            suspension.tick(longAccel, forward * Math.toRadians(deltaRotation), getY() - this.yo);
        }

        this.wheelRollO = this.wheelRoll;
        this.wheelRoll += (float) (forward / wheelRadius());
        if (Math.abs(this.wheelRoll) > TWO_PI) {
            // Resta vueltas completas a ambos valores: la interpolación sigue continua y el float no pierde precisión
            float turns = TWO_PI * (int) (this.wheelRoll / TWO_PI);
            this.wheelRoll -= turns;
            this.wheelRollO -= turns;
        }

        this.steerO = this.steer;
        double target = CarPhysics.steerFromMotion(deltaRotation, forward, tuning);
        if (Double.isNaN(target)) target = this.steer * 0.8; // detenido: volver al centro poco a poco
        this.steer += (float) (Mth.clamp(target, -tuning.maxSteerLow(), tuning.maxSteerLow()) - this.steer) * 0.5F;
    }

    public float getWheelRoll(float partialTick) {
        return Mth.lerp(partialTick, this.wheelRollO, this.wheelRoll);
    }

    public float getSteer(float partialTick) {
        return Mth.lerp(partialTick, this.steerO, this.steer);
    }

    public Suspension getSuspension() {
        return this.suspension;
    }

    public double getForwardSpeed() {
        return this.forwardSpeed;
    }

    public boolean isEngineRunning() {
        return getControllingPassenger() != null && getFuel() > 0;
    }

    private void serverTick(ServerLevel level, LivingEntity driver) {
        runOver(level, driver);
        tickFuel(driver);
    }

    /**
     * Aparta (y desde cierta velocidad lastima) a las criaturas que el vehículo tiene delante. Va en el servidor
     * porque el daño lo decide él; sin esto el vehículo choca contra ellas como contra una pared.
     */
    private void runOver(ServerLevel level, LivingEntity driver) {
        Vec3 pos = position();
        Vec3 motion = lastServerPos == null ? Vec3.ZERO : pos.subtract(lastServerPos);
        lastServerPos = pos;

        CarConfig.RunOver config = CarConfig.get().runOver();
        double speed = motion.horizontalDistance();
        if (!config.enabled() || speed < config.pushSpeed()) return;

        double dirX = motion.x / speed;
        double dirZ = motion.z / speed;
        double strength = config.knockback() * Math.min(speed / tuning().maxSpeed(), 1.0);
        float damage = (float) ((speed - config.damageSpeed()) * config.damagePerSpeed());
        AABB zone = getBoundingBox().expandTowards(motion.x, 0.0, motion.z).inflate(0.1, 0.0, 0.1);
        DamageSource source = damageSources().source(DamageTypes.MOB_ATTACK, this, driver);

        for (Entity entity : level.getEntities(this, zone, CarEntity::canBeRunOver)) {
            LivingEntity victim = (LivingEntity) entity;
            boolean hurts = damage > 0 && (config.hurtPlayers() || !(victim instanceof Player));
            if (hurts) victim.hurtServer(level, source, damage);
            // Hacia adelante y hacia el lado en que ya estaba, para que salga del camino y no quede debajo
            double offX = victim.getX() - pos.x;
            double offZ = victim.getZ() - pos.z;
            double sideSign = Math.signum(offX * dirZ - offZ * dirX);
            double pushX = dirX + dirZ * sideSign * 0.6;
            double pushZ = dirZ - dirX * sideSign * 0.6;
            victim.knockback(strength, -pushX, -pushZ, source, hurts ? damage : 0.0F);
        }
    }

    private static boolean canBeRunOver(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()
                || living.isInvulnerable() || living.getVehicle() != null) {
            return false;
        }
        return !(living instanceof Player player && player.isCreative());
    }

    /** Gasta combustible mientras el conductor acelera o retrocede y le muestra el nivel. */
    private void tickFuel(LivingEntity driver) {
        if (!(driver instanceof ServerPlayer player)) {
            lastFuelPercent = -1;
            return;
        }

        Input keys = player.getLastClientInput();
        if (getFuel() > 0 && (keys.forward() || keys.backward())) {
            setFuel(getFuel() - 1);
        }

        // Solo manda paquete si cambia el porcentaje o antes de que se borre la action bar
        int percent = getFuel() * 100 / CarConfig.get().maxFuel();
        if (percent != lastFuelPercent || this.tickCount % FUEL_HUD_INTERVAL == 0) {
            lastFuelPercent = percent;
            player.sendSystemMessage(Component.translatable("message.garage.fuel", percent), true);
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.08;
    }

    /** El vehículo amortigua las caídas: vanilla pasaría el golpe a los pasajeros (propagateFallToPassengers). */
    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public float maxUpStep() {
        return 1.0F; // sube bloques completos sin saltar
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 3);
    }

    // --- Colisión y golpes (igual que el bote) -----------------------------------------------

    @Override
    public boolean canCollideWith(Entity other) {
        return AbstractBoat.canVehicleCollide(this, other);
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public void push(Entity other) {
        if (!level().isClientSide()
                && !other.isPassenger()
                && getControllingPassenger() != null
                && canAddPassenger(other)
                && Math.abs(forwardSpeed) < CarConfig.get().runOver().pushSpeed()
                && other instanceof LivingEntity
                && !(other instanceof Player)
                && !other.isInWater()
                && other.getBbWidth() < getBbWidth()) {
            other.startRiding(this);
            return;
        }
        super.push(other);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public void animateHurt(float yaw) {
        setHurtDir(-getHurtDir());
        setHurtTime(10);
        setDamage(getDamage() * 11.0F);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.CAR;
    }

    @Override
    public ItemStack getPickResult() {
        return ModItems.vehicleStack(getDropItem(), getColor(), defaultColor());
    }

    /** Igual que VehicleEntity.destroy, pero el item soltado conserva el color. */
    @Override
    public void destroy(ServerLevel level, Item item) {
        kill(level);
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            ItemStack stack = ModItems.vehicleStack(item, getColor(), defaultColor());
            stack.set(DataComponents.CUSTOM_NAME, getCustomName());
            spawnAtLocation(level, stack);
        }
    }

    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        destroy(level, getDropItem());
        chestVehicleDestroyed(source, level, this);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide() && reason.shouldDestroy()) {
            Containers.dropContents(level(), this, this);
        }
        super.remove(reason);
    }

    // --- Interacciones: combustible, tinte, baúl y subir ---------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hit) {
        if (player.isSecondaryUseActive()) {
            InteractionResult result = interactWithContainerVehicle(player);
            if (result.consumesAction()) gameEvent(GameEvent.CONTAINER_OPEN, player);
            return result;
        }

        ItemStack held = player.getItemInHand(hand);

        if (held.is(ItemTags.FURNACE_MINECART_FUEL)) {
            if (getFuel() >= CarConfig.get().maxFuel()) return InteractionResult.PASS;
            if (!level().isClientSide()) {
                setFuel(getFuel() + CarConfig.get().fuelPerItem());
                held.consume(1, player);
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 0.8F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        DyeColor dye = held.get(DataComponents.DYE);
        if (dye != null) {
            int rgb = dye.getTextureDiffuseColor() & 0xFFFFFF;
            if (rgb == getColor()) return InteractionResult.PASS;
            if (!level().isClientSide()) {
                setColor(rgb);
                held.consume(1, player);
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.DYE_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        if (!canAddPassenger(player)) return InteractionResult.PASS;
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    // --- Asientos --------------------------------------------------------------------------

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < seats();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        super.positionRider(passenger, move);
        if (passenger instanceof LivingEntity) {
            // El pasajero gira junto con el vehículo, pero puede mirar alrededor
            passenger.setYRot(passenger.getYRot() + deltaRotation);
            passenger.setYHeadRot(passenger.getYHeadRot() + deltaRotation);
            clampRotation(passenger);
        }
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        clampRotation(passenger);
    }

    private void clampRotation(Entity passenger) {
        passenger.setYBodyRot(getYRot());
        float limit = headTurnLimit();
        float diff = Mth.wrapDegrees(passenger.getYRot() - getYRot());
        float clamped = Mth.clamp(diff, -limit, limit);
        passenger.yRotO += clamped - diff;
        passenger.setYRot(passenger.getYRot() + clamped - diff);
        passenger.setYHeadRot(passenger.getYRot());
    }

    /**
     * Baja al pasajero por la puerta de su lado, a la altura de su asiento; si ese lado está tapado prueba
     * el otro y, si tampoco, deja el comportamiento vanilla. Se llama cuando el pasajero ya salió de la lista
     * pero aún no se movió, así que su posición todavía dice en qué asiento iba.
     */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 center = position();
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 left = new Vec3(forward.z, 0.0, -forward.x); // el +X del modelo (lado del conductor)
        Vec3 rel = passenger.position().subtract(center);
        Vec3 seat = center.add(forward.scale(rel.dot(forward)));
        double side = rel.dot(left) < 0 ? -1.0 : 1.0;
        // La caja de colisión no gira con el vehículo: hay que salir de ella entera, más medio jugador
        double reach = getBbWidth() / 2.0 + passenger.getBbWidth() / 2.0 + 0.05;

        for (double s : new double[] {side, -side}) {
            Vec3 spot = findDismountSpot(passenger, exitPoint(seat.subtract(center), left.scale(s), reach).add(center));
            if (spot != null) return spot;
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    /** Dónde sale de un cuadrado de medio lado {@code reach} un rayo que parte de {@code from} (relativo al centro). */
    private static Vec3 exitPoint(Vec3 from, Vec3 dir, double reach) {
        double t = Double.MAX_VALUE;
        if (Math.abs(dir.x) > 1.0e-6) t = Math.min(t, (Math.signum(dir.x) * reach - from.x) / dir.x);
        if (Math.abs(dir.z) > 1.0e-6) t = Math.min(t, (Math.signum(dir.z) * reach - from.z) / dir.z);
        return from.add(dir.scale(t));
    }

    /** Suelo válido y espacio libre en (x, z): al nivel del vehículo, un bloque arriba o uno abajo. */
    private Vec3 findDismountSpot(LivingEntity passenger, Vec3 at) {
        BlockPos base = BlockPos.containing(at.x, getY(), at.z);
        for (BlockPos pos : new BlockPos[] {base, base.above(), base.below()}) {
            double floor = level().getBlockFloorHeight(pos);
            if (!DismountHelper.isBlockFloorValid(floor)) continue;
            Vec3 spot = new Vec3(at.x, pos.getY() + floor, at.z);
            for (Pose pose : passenger.getDismountPoses()) {
                if (DismountHelper.canDismountTo(level(), spot, passenger, pose)) {
                    passenger.setPose(pose);
                    return spot;
                }
            }
        }
        return null;
    }

    // --- Bocina ----------------------------------------------------------------------------

    public void honk(ServerPlayer player) {
        if (hornCooldown > 0 || getControllingPassenger() != player) return;
        hornCooldown = hornCooldownTicks();
        playHornSound();
    }

    protected int hornCooldownTicks() {
        return 20;
    }

    protected void playHornSound() {
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.NEUTRAL, 1.4F, 0.6F);
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.NEUTRAL, 1.4F, 0.75F);
    }

    // --- Baúl (patrón de AbstractChestBoat) ----------------------------------------------------

    @Override
    public void openCustomInventoryScreen(Player player) {
        player.openMenu(this);
        if (!player.level().isClientSide()) {
            gameEvent(GameEvent.CONTAINER_OPEN, player);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        unpackChestVehicleLootTable(inventory.player);
        int rows = containerRows();
        MenuType<ChestMenu> menuType = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            default -> MenuType.GENERIC_9x3;
        };
        return new ChestMenu(menuType, containerId, inventory, this, rows);
    }

    @Override
    public void clearContent() {
        clearChestVehicleContent();
    }

    @Override
    public int getContainerSize() {
        return containerRows() * 9;
    }

    @Override
    public ItemStack getItem(int slot) {
        return getChestVehicleItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return removeChestVehicleItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeChestVehicleItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        setChestVehicleItem(slot, stack);
    }

    @Override
    public SlotAccess getSlot(int slot) {
        return getChestVehicleSlot(slot);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return isChestVehicleStillValid(player);
    }

    @Override
    public ResourceKey<LootTable> getContainerLootTable() {
        return this.lootTable;
    }

    @Override
    public void setContainerLootTable(ResourceKey<LootTable> lootTable) {
        this.lootTable = lootTable;
    }

    @Override
    public long getContainerLootTableSeed() {
        return this.lootTableSeed;
    }

    @Override
    public void setContainerLootTableSeed(long seed) {
        this.lootTableSeed = seed;
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return this.itemStacks;
    }

    @Override
    public void clearItemStacks() {
        this.itemStacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
    }
}
