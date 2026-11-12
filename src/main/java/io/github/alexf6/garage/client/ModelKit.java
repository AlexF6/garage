package io.github.alexf6.garage.client;

import io.github.alexf6.garage.GarageMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
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

    /** Píxeles del modelo por bloque: el eje Y del modelo apunta hacia abajo y mide en píxeles, la física en bloques. */
    static final float PIXELS_PER_BLOCK = 16.0F;

    /** Hijo {@code name} de {@code parent}, o {@code null} si esta capa del modelo no lo tiene (BODY y TRIM se reparten las piezas). */
    static ModelPart childOrNull(ModelPart parent, String name) {
        return parent.hasChild(name) ? parent.getChild(name) : null;
    }

    /** Ruedas hijas de {@code root}, en el orden de {@code names}; {@code null} donde la capa no las tiene. */
    static ModelPart[] wheels(ModelPart root, String[] names) {
        ModelPart[] wheels = new ModelPart[names.length];
        for (int i = 0; i < names.length; i++) wheels[i] = childOrNull(root, names[i]);
        return wheels;
    }

    /** Hace rodar todas las ruedas y gira solo las {@code steered} primeras (las directrices). */
    static void spinWheels(ModelPart[] wheels, float roll, float steer, int steered) {
        for (int i = 0; i < wheels.length; i++) {
            if (wheels[i] == null) continue;
            wheels[i].xRot = roll;
            wheels[i].yRot = i < steered ? steer : 0.0F;
        }
    }

    /** Desplazamiento vertical de la suspensión (rebote) en píxeles del modelo; positivo hacia abajo. */
    static float heavePx(float bodyHeave) {
        return -bodyHeave * PIXELS_PER_BLOCK;
    }

    private ModelKit() {}
}
