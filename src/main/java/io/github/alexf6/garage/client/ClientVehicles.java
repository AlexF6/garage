package io.github.alexf6.garage.client;

import io.github.alexf6.garage.GarageMod;
import io.github.alexf6.garage.VehicleSpec;
import io.github.alexf6.garage.VehicleSpecs;
import io.github.alexf6.garage.client.VehicleRenderer.VehicleRenderState;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

/**
 * Aspecto de cada {@link VehicleSpec} en el cliente: una entrada por vehículo, con su modelo y textura.
 * Si falta la entrada de un vehículo, {@link #lookOf} falla al arrancar y no al dibujarlo.
 */
final class ClientVehicles {

    /** Modelo de un vehículo: sus dos capas y cómo se animan. */
    record Model(VehicleRenderer.Look look, Supplier<LayerDefinition> bodyLayer, Supplier<LayerDefinition> trimLayer) {}

    private static final Map<String, Model> MODELS = Map.ofEntries(
            entry(VehicleSpecs.CAR, 0.9F, CarModels.BODY, CarModels.TRIM, CarModels.Part::new,
                    CarModels::createBodyLayer, CarModels::createTrimLayer),
            entry(VehicleSpecs.MOTORCYCLE, 0.5F, MotorcycleModels.BODY, MotorcycleModels.TRIM,
                    MotorcycleModels.Part::new, MotorcycleModels::createBodyLayer, MotorcycleModels::createTrimLayer),
            entry(VehicleSpecs.MOTOCARRO, 0.75F, MotocarroModels.BODY, MotocarroModels.TRIM,
                    MotocarroModels.Part::new, MotocarroModels::createBodyLayer, MotocarroModels::createTrimLayer),
            entry(VehicleSpecs.MOTOCARGUERO, 0.8F, MotocargueroModels.BODY, MotocargueroModels.TRIM,
                    MotocargueroModels.Part::new, MotocargueroModels::createBodyLayer,
                    MotocargueroModels::createTrimLayer));

    /** La textura de entidad es {@code textures/entity/<id>.png}: sale del id del spec, no se repite a mano. */
    private static Map.Entry<String, Model> entry(VehicleSpec spec, float shadowRadius, ModelLayerLocation body,
                                                  ModelLayerLocation trim,
                                                  Function<ModelPart, EntityModel<VehicleRenderState>> part,
                                                  Supplier<LayerDefinition> bodyLayer,
                                                  Supplier<LayerDefinition> trimLayer) {
        VehicleRenderer.Look look = new VehicleRenderer.Look(texture(spec.id()), shadowRadius, body, trim, part);
        return Map.entry(spec.id(), new Model(look, bodyLayer, trimLayer));
    }

    static Model lookOf(VehicleSpec spec) {
        Model model = MODELS.get(spec.id());
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
