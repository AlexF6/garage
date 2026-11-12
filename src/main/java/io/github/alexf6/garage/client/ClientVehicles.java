package io.github.alexf6.garage.client;

import io.github.alexf6.garage.GarageMod;
import io.github.alexf6.garage.VehicleSpec;
import io.github.alexf6.garage.VehicleSpecs;
import java.util.Map;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import java.util.function.Supplier;

/**
 * Aspecto de cada {@link VehicleSpec} en el cliente: una entrada por vehículo, con su modelo y textura.
 * Si falta la entrada de un vehículo, {@link #lookOf} falla al arrancar y no al dibujarlo.
 */
final class ClientVehicles {

    /** Modelo de un vehículo: sus dos capas y cómo se animan. */
    record Model(VehicleRenderer.Look look, Supplier<LayerDefinition> bodyLayer, Supplier<LayerDefinition> trimLayer) {}

    private static final Map<VehicleSpec, Model> MODELS = Map.of(
            VehicleSpecs.CAR, new Model(new VehicleRenderer.Look(texture("car"), 0.9F,
                    CarModels.BODY, CarModels.TRIM, CarModels.Part::new),
                    CarModels::createBodyLayer, CarModels::createTrimLayer),
            VehicleSpecs.MOTORCYCLE, new Model(new VehicleRenderer.Look(texture("motorcycle"), 0.5F,
                    MotorcycleModels.BODY, MotorcycleModels.TRIM, MotorcycleModels.Part::new),
                    MotorcycleModels::createBodyLayer, MotorcycleModels::createTrimLayer),
            VehicleSpecs.MOTOCARRO, new Model(new VehicleRenderer.Look(texture("motocarro"), 0.75F,
                    MotocarroModels.BODY, MotocarroModels.TRIM, MotocarroModels.Part::new),
                    MotocarroModels::createBodyLayer, MotocarroModels::createTrimLayer));

    static Model lookOf(VehicleSpec spec) {
        Model model = MODELS.get(spec);
        if (model == null) {
            throw new IllegalStateException("Falta el modelo de cliente de " + spec.id() + " en ClientVehicles");
        }
        return model;
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, "textures/entity/" + name + ".png");
    }

    private ClientVehicles() {}
}
