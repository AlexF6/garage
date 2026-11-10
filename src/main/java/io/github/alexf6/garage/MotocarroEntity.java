package io.github.alexf6.garage;

import io.github.alexf6.garage.physics.CarTuning;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Motocarro / Autorickshaw urbano (referencia Bajaj RE Compact 4S / "Torito") de 3 plazas:
 * 1 conductor delantero centrado frente al manillar y 2 pasajeros en la banca posterior.
 * Configuración Delta (1 rueda delantera directriz, 2 traseras de tracción) y baúl de 18 casillas (9x2).
 */
public class MotocarroEntity extends CarEntity {

    public static final int SEATS = 3;
    public static final int DEFAULT_COLOR = 0xE8ECEF; // Blanco Glaciar
    private static final int CONTAINER_ROWS = 2;      // Baúl de 2 filas (9x2 = 18 casillas)
    private static final float WHEEL_RADIUS = 0.3125F; // rin de 10 px = radio 5 px (5/16)

    public MotocarroEntity(EntityType<? extends MotocarroEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public CarTuning tuning() {
        return CarTuning.MOTOCARRO;
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
    public float idleEnginePitch() {
        return 0.65F;
    }

    @Override
    public float topEnginePitch() {
        return 1.55F;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.MOTOCARRO;
    }

    @Override
    protected void playHornSound() {
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1.4F, 1.6F);
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1.4F, 1.9F);
    }
}
