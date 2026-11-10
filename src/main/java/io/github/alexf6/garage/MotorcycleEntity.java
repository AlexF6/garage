package io.github.alexf6.garage;

import io.github.alexf6.garage.physics.CarTuning;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Motocicleta Enduro (Yamaha XTZ 125) de 2 asientos en tándem y alforja de 9 casillas.
 * Hereda el manejo, el combustible, los atropellos y el contenedor de {@link CarEntity}, y añade la
 * inclinación lateral (tumbada) proporcional al giro y a la velocidad observada.
 */
public class MotorcycleEntity extends CarEntity {

    public static final int SEATS = 2;
    public static final int DEFAULT_COLOR = 0x1846C7; // Azul Yamaha Racing Blue (#1846C7)
    private static final int CONTAINER_ROWS = 1;      // Alforja de 9 casillas (9x1)
    private static final float WHEEL_RADIUS = 0.35F;

    private float lean;
    private float leanO;

    public MotorcycleEntity(EntityType<? extends MotorcycleEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public CarTuning tuning() {
        return CarTuning.ENDURO;
    }

    @Override
    protected int seats() {
        return SEATS;
    }

    @Override
    public int defaultColor() {
        return DEFAULT_COLOR;
    }

    @Override
    protected int containerRows() {
        return CONTAINER_ROWS;
    }

    @Override
    protected float wheelRadius() {
        return WHEEL_RADIUS;
    }

    @Override
    protected float headTurnLimit() {
        return 85.0F;
    }

    @Override
    protected float directTurnRate() {
        return 2.8F;
    }

    @Override
    public float idleEnginePitch() {
        return 0.7F;
    }

    @Override
    public float topEnginePitch() {
        return 1.7F;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.MOTORCYCLE;
    }

    @Override
    protected int hornCooldownTicks() {
        return 15;
    }

    @Override
    protected void playHornSound() {
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1.2F, 1.4F);
    }

    @Override
    protected void tickVisuals() {
        super.tickVisuals();
        // Inclinación lateral (tumbada) proporcional al giro y velocidad observada
        this.leanO = this.lean;
        float speedRatio = (float) Math.min(Math.abs(getForwardSpeed()) / tuning().maxSpeed(), 1.0);
        float targetLean = getSteer(1.0F) * speedRatio * 0.48F;
        this.lean += (targetLean - this.lean) * 0.3F;
    }

    public float getLean(float partialTick) {
        return Mth.lerp(partialTick, this.leanO, this.lean);
    }
}
