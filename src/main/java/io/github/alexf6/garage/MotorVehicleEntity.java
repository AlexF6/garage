package io.github.alexf6.garage;

import io.github.alexf6.garage.physics.VehicleInput;
import io.github.alexf6.garage.physics.VehiclePhysics;
import io.github.alexf6.garage.physics.VehicleTuning;
import io.github.alexf6.garage.physics.VehicleMath;
import io.github.alexf6.garage.physics.Suspension;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Vehículo motorizado: una sola clase para el Jeep, la moto y el motocarro; lo que cambia de uno a otro lo
 * dicta su {@link VehicleSpec}. Sigue el patrón del bote vanilla: el cliente del conductor calcula la física y
 * el servidor recibe la posición; el combustible y el color viven en el servidor y se sincronizan.
 */
public class MotorVehicleEntity extends ChestVehicleEntity {

    private static final int FUEL_HUD_INTERVAL = 40; // la action bar dura ~3 s; se reenvía antes o si cambia
    private static final float LEAN_SMOOTHING = 0.3F;
    private static final double DEFAULT_GRAVITY = 0.08; // bloques/tick²
    private static final float STEP_HEIGHT = 1.0F; // sube bloques completos sin saltar
    private static final int INTERPOLATION_STEPS = 3;
    private static final int PARKED_DRIVE_INTERVAL = 10; // ticks entre comprobaciones de un vehículo aparcado
    private static final double PARKED_MOTION_SQR = 1.0e-6; // (bloques/tick)²

    private static final EntityDataAccessor<Integer> DATA_FUEL =
            SynchedEntityData.defineId(MotorVehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(MotorVehicleEntity.class, EntityDataSerializers.INT);

    private final VehicleSpec spec;
    private final RunOverPusher runOverPusher = new RunOverPusher();

    private VehicleInput input = VehicleInput.NONE;
    private double steerAngle; // ángulo real de dirección (solo en quien simula)
    private float deltaRotation;
    private int lastFuelPercent = -1;
    private int hornCooldown;

    // Animación (solo cliente): rodado de las ruedas, giro de las delanteras e inclinación lateral, en radianes
    private float wheelRoll;
    private float wheelRollO;
    private float steer;
    private float steerO;
    private float lean;
    private float leanO;
    private double forwardSpeed; // observada (bloques/tick); la usan el sonido, la suspensión y el empuje
    private final Suspension suspension = new Suspension();

    public MotorVehicleEntity(EntityType<? extends MotorVehicleEntity> type, Level level, VehicleSpec spec) {
        super(type, level, spec.containerRows());
        this.spec = spec;
        // Los datos sincronizados se definen en el constructor de Entity, antes de que exista el spec
        this.entityData.set(DATA_COLOR, spec.defaultColor());
    }

    public VehicleSpec spec() {
        return spec;
    }

    public VehicleTuning tuning() {
        return spec.tuning().get();
    }

    public int defaultColor() {
        return spec.defaultColor();
    }

    /** Velocidad observada como fracción de la máxima, en [0, 1]. */
    public double speedRatio() {
        return VehicleMath.speedRatio(forwardSpeed, tuning().maxSpeed());
    }

    // --- Datos sincronizados -------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FUEL, 0);
        builder.define(DATA_COLOR, 0); // el color de verdad lo fija el constructor, cuando ya hay spec
    }

    public int getFuel() {
        return this.entityData.get(DATA_FUEL);
    }

    public void setFuel(int fuel) {
        this.entityData.set(DATA_FUEL, Mth.clamp(fuel, 0, GarageConfig.get().maxFuel()));
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setColor(int rgb) {
        this.entityData.set(DATA_COLOR, rgb & 0xFFFFFF);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Fuel", getFuel());
        output.putInt("Color", getColor());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setFuel(input.getIntOr("Fuel", 0));
        setColor(input.getIntOr("Color", defaultColor()));
    }

    // --- Entrada del conductor (la fija el cliente, ver GarageClient) ------------------------

    public void setInput(VehicleInput input) {
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
            if (driver == null) input = VehicleInput.NONE;
            // Aparcado y quieto: basta comprobar el suelo cada medio segundo en vez de cada tick
            if (!isParked(driver) || this.tickCount % PARKED_DRIVE_INTERVAL == 0) drive();
        }
        // Después de moverse: si no, en el cliente del conductor el giro y la distancia del tick valen 0
        deltaRotation = Mth.wrapDegrees(getYRot() - this.yRotO);
        observeMotion();

        if (level() instanceof ServerLevel serverLevel) {
            serverTick(serverLevel, driver);
        }
    }

    /** Sin conductor, apoyado en el suelo y sin moverse: no hay física que calcular salvo que le quiten el suelo. */
    private boolean isParked(LivingEntity driver) {
        return driver == null && onGround() && getDeltaMovement().lengthSqr() < PARKED_MOTION_SQR;
    }

    private void drive() {
        VehicleTuning tuning = tuning();
        Vec3 motion = getDeltaMovement();
        boolean onGround = onGround();
        boolean inWater = isInWater();

        // Primero gira con la velocidad que traía; luego la velocidad se descompone en el rumbo nuevo, así
        // lo que el carro no alcanzó a girar queda como velocidad lateral (derrape) y las llantas la absorben
        double speed = VehicleMath.forwardOf(motion.x, motion.z, getYRot());
        steerAngle = VehiclePhysics.nextSteer(steerAngle, input, speed, tuning);
        float yawStep = VehiclePhysics.yawDelta(speed, steerAngle, onGround && input.brake(), tuning);
        yawStep = VehicleMath.directYaw(yawStep, speed, onGround, input.steering(), spec.directTurnRate());
        setYRot(getYRot() + yawStep);

        double yawRad = Math.toRadians(getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double forward = -motion.x * sin + motion.z * cos;
        double lateral = motion.x * cos + motion.z * sin;
        forward = VehiclePhysics.nextSpeed(forward, input, getFuel() > 0, onGround, inWater, tuning);
        lateral = VehiclePhysics.nextLateral(lateral, input, onGround, inWater, tuning);

        setDeltaMovement(-sin * forward + cos * lateral, inWater ? -0.04 : motion.y, cos * forward + sin * lateral);
        applyGravity();
        move(MoverType.SELF, getDeltaMovement());
    }

    /**
     * Velocidad observada a partir del movimiento real (sirve igual en el conductor, en otros jugadores y con
     * interpolación). En el cliente además anima las ruedas, la dirección, la suspensión y la inclinación.
     */
    private void observeMotion() {
        double forward = VehicleMath.forwardOf(getX() - this.xo, getZ() - this.zo, getYRot());
        double longAccel = forward - this.forwardSpeed;
        this.forwardSpeed = forward;
        if (level().isClientSide()) {
            animate(forward, longAccel);
        }
    }

    /**
     * Animación visual: rodado = distancia / radio; dirección = inversa del modelo de giro; suspensión según
     * las aceleraciones observadas; inclinación lateral proporcional al giro y a la velocidad.
     */
    private void animate(double forward, double longAccel) {
        VehicleTuning tuning = tuning();
        suspension.tick(longAccel, forward * Math.toRadians(deltaRotation), getY() - this.yo);

        this.wheelRollO = this.wheelRoll;
        this.wheelRoll += (float) (forward / spec.wheelRadius());
        // Resta vueltas completas a ambos valores: la interpolación sigue continua y el float no pierde precisión
        float turns = VehicleMath.fullTurns(this.wheelRoll);
        this.wheelRoll -= turns;
        this.wheelRollO -= turns;

        this.steerO = this.steer;
        double target = VehiclePhysics.steerFromMotion(deltaRotation, forward, tuning);
        this.steer = VehicleMath.nextVisualSteer(this.steer, target, tuning.maxSteerLow());

        if (spec.leanPerSteer() > 0.0F) {
            this.leanO = this.lean;
            float targetLean = this.steer * (float) speedRatio() * spec.leanPerSteer();
            this.lean += (targetLean - this.lean) * LEAN_SMOOTHING;
        }
    }

    public float getWheelRoll(float partialTick) {
        return Mth.lerp(partialTick, this.wheelRollO, this.wheelRoll);
    }

    public float getSteer(float partialTick) {
        return Mth.lerp(partialTick, this.steerO, this.steer);
    }

    /** Inclinación lateral (radianes) de las motos; 0 en los vehículos que no se inclinan. */
    public float getLean(float partialTick) {
        return Mth.lerp(partialTick, this.leanO, this.lean);
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
        runOverPusher.tick(level, this, driver);
        tickFuel(driver);
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
        int percent = VehicleMath.fuelPercent(getFuel(), GarageConfig.get().maxFuel());
        if (percent != lastFuelPercent || this.tickCount % FUEL_HUD_INTERVAL == 0) {
            lastFuelPercent = percent;
            player.sendSystemMessage(Component.translatable("message.garage.fuel", percent), true);
        }
    }

    @Override
    protected double getDefaultGravity() {
        return DEFAULT_GRAVITY;
    }

    /** El vehículo amortigua las caídas: vanilla pasaría el golpe a los pasajeros (propagateFallToPassengers). */
    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public float maxUpStep() {
        return STEP_HEIGHT;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, INTERPOLATION_STEPS);
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
                && Math.abs(forwardSpeed) < GarageConfig.get().runOver().pushSpeed()
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
        return ModVehicles.item(spec);
    }

    @Override
    public ItemStack getPickResult() {
        return ModVehicles.stack(getDropItem(), spec, getColor());
    }

    /** Igual que VehicleEntity.destroy, pero el item soltado conserva el color. */
    @Override
    public void destroy(ServerLevel level, Item item) {
        kill(level);
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            ItemStack stack = ModVehicles.stack(item, spec, getColor());
            stack.set(DataComponents.CUSTOM_NAME, getCustomName());
            spawnAtLocation(level, stack);
        }
    }

    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        destroy(level, getDropItem());
        chestVehicleDestroyed(source, level, this);
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
            // El tope del tanque es de la config del servidor: el cliente no lo adivina, solo balancea el brazo
            if (level().isClientSide()) return InteractionResult.SUCCESS;
            if (getFuel() >= GarageConfig.get().maxFuel()) return InteractionResult.PASS;
            setFuel(getFuel() + GarageConfig.get().fuelPerItem());
            held.consume(1, player);
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 0.8F, 1.0F);
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
        return getPassengers().size() < spec.seats().size();
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
        float limit = spec.headTurnLimit();
        float diff = Mth.wrapDegrees(passenger.getYRot() - getYRot());
        float clamped = Mth.clamp(diff, -limit, limit);
        passenger.yRotO += clamped - diff;
        passenger.setYRot(passenger.getYRot() + clamped - diff);
        passenger.setYHeadRot(passenger.getYRot());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 spot = DismountLocator.find(this, passenger);
        return spot != null ? spot : super.getDismountLocationForPassenger(passenger);
    }

    // --- Bocina ----------------------------------------------------------------------------

    public void honk(ServerPlayer player) {
        if (hornCooldown > 0 || getControllingPassenger() != player) return;
        VehicleSpec.Horn horn = spec.horn();
        hornCooldown = horn.cooldownTicks();
        for (float pitch : horn.pitches()) {
            level().playSound(null, getX(), getY(), getZ(), horn.sound().value(), SoundSource.NEUTRAL,
                    horn.volume(), pitch);
        }
    }
}
