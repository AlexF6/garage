package io.github.alexf6.garage.client;

import io.github.alexf6.garage.GarageMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.resources.Identifier;

/** Atajos comunes de los modelos de cubos de los vehículos. */
final class ModelKit {

    /** Capa {@code layer} del modelo {@code model}, bajo el espacio de nombres del mod. */
    static ModelLayerLocation layer(String model, String layer) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, model), layer);
    }

    /** Constructor de cubos cuya textura empieza en el píxel (u, v) del lienzo. */
    static CubeListBuilder cube(int u, int v) {
        return CubeListBuilder.create().texOffs(u, v);
    }

    /** Posición de una pieza respecto a su padre, en píxeles del modelo. */
    static PartPose at(float x, float y, float z) {
        return PartPose.offset(x, y, z);
    }

    private ModelKit() {}
}
