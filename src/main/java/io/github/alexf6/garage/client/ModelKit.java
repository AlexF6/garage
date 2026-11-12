package io.github.alexf6.garage.client;

import io.github.alexf6.garage.GarageMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

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

    /** Por debajo de esta fracción de la velocidad máxima el motor encendido "ralentiza" y el modelo vibra. */
    static final float IDLE_SPEED_RATIO = 0.05F;

    /**
     * Vibración de ralentí de un motor: un vaivén (radianes) y un temblor vertical (píxeles) que dependen de la edad
     * de la entidad. Cada modelo decide en qué ejes los aplica.
     *
     * @param swaySpeed       radianes por tick de edad del vaivén
     * @param swayAmplitude   amplitud del vaivén (radianes)
     * @param bobSpeed        radianes por tick de edad del temblor vertical
     * @param bobAmplitudePx  amplitud del temblor vertical (píxeles del modelo)
     * @param pitchShare      parte del vaivén que pasa al cabeceo del chasis
     */
    record IdleVibration(float swaySpeed, float swayAmplitude, float bobSpeed, float bobAmplitudePx, float pitchShare) {
        float sway(float ageInTicks) {
            return Mth.sin(ageInTicks * swaySpeed) * swayAmplitude;
        }

        float bobPx(float ageInTicks) {
            return Mth.cos(ageInTicks * bobSpeed) * bobAmplitudePx;
        }
    }

    /** Desplazamiento vertical de la suspensión (rebote) en píxeles del modelo; positivo hacia abajo. */
    static float heavePx(float bodyHeave) {
        return -bodyHeave * PIXELS_PER_BLOCK;
    }

    /**
     * Añade a {@code parent} una rueda de radios: la banda de rodadura recta, otra girada 45° (para que parezca
     * redonda con cubos) y los bujes.
     */
    static PartDefinition addWheel(PartDefinition parent, String name, PartPose pose, CubeListBuilder tread0,
                                   CubeListBuilder tread45, CubeListBuilder hubs) {
        PartDefinition wheel = parent.addOrReplaceChild(name, tread0, pose);
        wheel.addOrReplaceChild("tread_45", tread45, PartPose.rotation((float) Math.PI / 4, 0.0F, 0.0F));
        wheel.addOrReplaceChild("hubs", hubs, PartPose.ZERO);
        return wheel;
    }

    private ModelKit() {}
}
