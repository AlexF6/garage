package io.github.alexf6.garage;

import io.github.alexf6.garage.config.GarageConfig;

import io.github.alexf6.garage.physics.VehicleTuning;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Catálogo de vehículos del mod. Para añadir uno basta otra entrada aquí y en {@link #ALL}. */
public final class VehicleSpecs {

    public static final VehicleSpec CAR = VehicleSpec.builder("car")
            .size(2.4F, 2.2F)
            // Conductor, copiloto y dos atrás
            .seats(new Vec3(0.44, 0.75, 0.0), new Vec3(-0.44, 0.75, 0.0),
                    new Vec3(0.44, 0.75, -0.75), new Vec3(-0.44, 0.75, -0.75))
            .color(0xC82127) // rojo Willys
            .chestRows(3)
            .wheelRadius(0.375F)
            .headTurnLimit(105.0F)
            .directTurnRate(1.2F)
            .enginePitch(0.5F, 1.4F)
            .horn(20, SoundEvents.NOTE_BLOCK_PLING, 1.4F, 0.6F, 0.75F)
            .tuning(GarageConfig::carTuning)
            .build();

    public static final VehicleSpec MOTORCYCLE = VehicleSpec.builder("motorcycle")
            .size(0.85F, 1.25F)
            // Sillín de dos niveles: conductor (tope 1.05, z de +0.09 a -0.25) y pasajero (tope 1.12, hasta -0.59)
            .seats(new Vec3(0.0, 1.05, -0.08), new Vec3(0.0, 1.12, -0.42))
            .color(0x1846C7) // azul Yamaha Racing Blue
            .chestRows(1) // alforja de 9 casillas
            .wheelRadius(0.35F)
            .headTurnLimit(85.0F)
            .directTurnRate(2.8F)
            .enginePitch(0.7F, 1.7F)
            .leanPerSteer(0.48F)
            .horn(15, SoundEvents.NOTE_BLOCK_BELL, 1.2F, 1.4F)
            .tuning(() -> VehicleTuning.ENDURO)
            .build();

    public static final VehicleSpec MOTOCARRO = VehicleSpec.builder("motocarro")
            .size(1.35F, 1.70F)
            .seats(new Vec3(0.0, 0.70, 0.31), new Vec3(0.28, 0.75, -0.62), new Vec3(-0.28, 0.75, -0.62))
            .color(0xE8ECEF) // blanco glaciar
            .chestRows(2) // baúl de 18 casillas
            .wheelRadius(0.3125F) // rin de 10 px = radio 5 px (5/16)
            .headTurnLimit(105.0F)
            .directTurnRate(1.2F)
            .enginePitch(0.65F, 1.55F)
            .horn(20, SoundEvents.NOTE_BLOCK_BELL, 1.4F, 1.6F, 1.9F)
            .tuning(() -> VehicleTuning.MOTOCARRO)
            .build();

    public static final VehicleSpec MOTOCARGUERO = VehicleSpec.builder("motocarguero")
            .size(1.40F, 1.70F)
            .seats(new Vec3(0.0, 0.90, -0.17)) // sobre el sillín (tope a 0.907, centro en z -0.17)
            .color(0xE8ECEF) // blanco glaciar
            .chestRows(6) // platón de carga de 54 casillas (9x6)
            .wheelRadius(0.35F) // rin de carga de 0.35
            .headTurnLimit(85.0F)
            .directTurnRate(1.8F)
            .enginePitch(0.60F, 1.50F)
            .horn(20, SoundEvents.NOTE_BLOCK_BELL, 1.4F, 1.3F, 1.6F)
            .tuning(() -> VehicleTuning.MOTOCARGUERO)
            .build();

    /** En el orden en que se registran y aparecen en la pestaña creativa. */
    public static final List<VehicleSpec> ALL = List.of(CAR, MOTORCYCLE, MOTOCARRO, MOTOCARGUERO);

    private VehicleSpecs() {}
}
