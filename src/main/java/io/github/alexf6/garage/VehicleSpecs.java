package io.github.alexf6.garage;

import io.github.alexf6.garage.physics.VehicleTuning;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Catálogo de vehículos del mod. Para añadir uno basta otra entrada aquí y en {@link #ALL}. */
public final class VehicleSpecs {

    // Asientos (x, altura del cojín, z), con el frente del vehículo en +Z: conductor, copiloto y dos atrás.
    public static final VehicleSpec CAR = new VehicleSpec("car", 2.4F, 2.2F,
            List.of(new Vec3(0.44, 0.75, 0.0), new Vec3(-0.44, 0.75, 0.0),
                    new Vec3(0.44, 0.75, -0.75), new Vec3(-0.44, 0.75, -0.75)),
            0xC82127, // rojo Willys
            3, 0.375F, 105.0F, 1.2F, 0.5F, 1.4F, 0.0F,
            new VehicleSpec.Horn(20, SoundEvents.NOTE_BLOCK_PLING, 1.4F, List.of(0.6F, 0.75F)),
            GarageConfig::carTuning);

    public static final VehicleSpec MOTORCYCLE = new VehicleSpec("motorcycle", 0.85F, 1.25F,
            // El sillín del modelo va de z = +0.09 (conductor) a -0.59 (pasajero); se sientan en su mitad trasera
            List.of(new Vec3(0.0, 0.62, -0.2), new Vec3(0.0, 0.72, -0.55)),
            0x1846C7, // azul Yamaha Racing Blue
            1, // alforja de 9 casillas
            0.35F, 85.0F, 2.8F, 0.7F, 1.7F, 0.48F,
            new VehicleSpec.Horn(15, SoundEvents.NOTE_BLOCK_BELL, 1.2F, List.of(1.4F)),
            () -> VehicleTuning.ENDURO);

    public static final VehicleSpec MOTOCARRO = new VehicleSpec("motocarro", 1.35F, 1.70F,
            List.of(new Vec3(0.0, 0.70, 0.31), new Vec3(0.28, 0.75, -0.62), new Vec3(-0.28, 0.75, -0.62)),
            0xE8ECEF, // blanco glaciar
            2, // baúl de 18 casillas
            0.3125F, // rin de 10 px = radio 5 px (5/16)
            105.0F, 1.2F, 0.65F, 1.55F, 0.0F,
            new VehicleSpec.Horn(20, SoundEvents.NOTE_BLOCK_BELL, 1.4F, List.of(1.6F, 1.9F)),
            () -> VehicleTuning.MOTOCARRO);

    public static final VehicleSpec MOTOCARGUERO = new VehicleSpec("motocarguero", 1.40F, 1.70F,
            List.of(new Vec3(0.0, 0.90, -0.17)), // sobre el sillín (tope a 0.907, centro en z -0.17)
            0xE8ECEF, // blanco glaciar
            6, // platón de carga de 54 casillas (9x6)
            0.35F, // rin de carga de 0.35
            85.0F, 1.8F, 0.60F, 1.50F, 0.0F,
            new VehicleSpec.Horn(20, SoundEvents.NOTE_BLOCK_BELL, 1.4F, List.of(1.3F, 1.6F)),
            () -> VehicleTuning.MOTOCARGUERO);

    /** En el orden en que se registran y aparecen en la pestaña creativa. */
    public static final List<VehicleSpec> ALL = List.of(CAR, MOTORCYCLE, MOTOCARRO, MOTOCARGUERO);

    private VehicleSpecs() {}
}
