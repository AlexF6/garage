package io.github.alexf6.garage.client;

import io.github.alexf6.garage.client.VehicleRenderer.VehicleRenderState;
import static io.github.alexf6.garage.client.ModelKit.at;
import static io.github.alexf6.garage.client.ModelKit.cube;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Modelo de Motocarro urbano (Bajaj RE / "Torito") en dos capas que comparten textura (256x256):
 * - BODY: carrocería tintable (guardabarros delantero, escudo frontal ahusado, tabique divisorio y caja trasera).
 * - TRIM: chasis, antifaz negro de doble faro, parabrisas con marco, capota de lona negra, interior y 3 ruedas.
 * Origen en el suelo (y = 24) y frente hacia -Z. Las piezas solo se tocan borde con borde sin solaparse
 * (cero z-fighting) y las ruedas cuelgan de la raíz para quedar siempre apoyadas en el suelo.
 */
public final class MotocarroModels {

    public static final ModelLayerLocation BODY = layer("body");
    public static final ModelLayerLocation TRIM = layer("trim");

    public static final String[] WHEELS = {"wheel_front", "wheel_bl", "wheel_br"};
    private static final int FRONT_WHEELS = 1; // las primeras de WHEELS: la delantera gira con el manillar
    private static final String CHASSIS = "chassis";
    private static final String STEERING = "steering";

    // Ralentí: vibración solo con el motor encendido y casi detenido
    private static final float IDLE_SPEED_RATIO = 0.05F;     // por debajo de esta fracción de la velocidad máxima
    private static final float IDLE_BUZZ_SPEED = 1.6F;       // rad por tick de edad
    private static final float IDLE_BUZZ_AMPLITUDE = 0.02F;  // rad de la vibración lateral
    private static final float IDLE_BOB_SPEED = 3.2F;        // rad por tick de edad
    private static final float IDLE_BOB_AMPLITUDE_PX = 0.12F; // píxeles del temblor vertical
    private static final float IDLE_PITCH_SHARE = 0.5F;      // parte de la vibración que pasa al cabeceo
    /** Balanceo (rad) hacia fuera de la curva con la dirección y la velocidad al máximo; crece con la velocidad². */
    private static final float BODY_ROLL_AT_FULL_STEER = 0.22F;

    private static ModelLayerLocation layer(String name) {
        return ModelKit.layer("motocarro", name);
    }

    /** Capa BODY: paneles de carrocería que se tiñen con el color del motocarro (blanco glaciar por defecto). */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition chassis = root.addOrReplaceChild(CHASSIS, CubeListBuilder.create(), at(0, 24, 0));

        // Guardabarros delantero sobre la rueda única (gira junto con la dirección en X=0, Z=-15)
        PartDefinition steering = chassis.addOrReplaceChild(STEERING, CubeListBuilder.create(), at(0, -5, -15));
        steering.addOrReplaceChild("fender_top", cube(0, 0).addBox(-2.5F, -7.0F, -4.5F, 5.0F, 2.0F, 9.0F), PartPose.ZERO);
        steering.addOrReplaceChild("fender_skirt", cube(30, 0).addBox(-2.5F, -5.0F, -5.5F, 5.0F, 3.0F, 3.0F), PartPose.ZERO);

        // Escudo frontal ahusado característico del Bajaj RE: nariz central estrecha y mejillas laterales
        chassis.addOrReplaceChild("nose_front", cube(0, 14).addBox(-6.0F, -20.0F, -18.0F, 12.0F, 11.0F, 3.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("cowl_left", cube(32, 14).addBox(6.0F, -20.0F, -16.0F, 4.0F, 14.0F, 5.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("cowl_right", cube(52, 14).addBox(-10.0F, -20.0F, -16.0F, 4.0F, 14.0F, 5.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("cowl_inner", cube(72, 14).addBox(-6.0F, -20.0F, -15.0F, 12.0F, 3.0F, 4.0F), PartPose.ZERO);

        // Tabique divisorio tras el conductor y estribos bajos de entrada de pasajeros
        chassis.addOrReplaceChild("divider", cube(0, 36).addBox(-10.0F, -14.0F, -1.0F, 20.0F, 8.0F, 2.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("sill_l", cube(46, 36).addBox(8.0F, -8.0F, 1.0F, 2.0F, 2.0F, 5.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("sill_r", cube(46, 36).addBox(-10.0F, -8.0F, 1.0F, 2.0F, 2.0F, 5.0F), PartPose.ZERO);

        // Carrocería trasera, compartimento del motor y guardabarros traseros
        chassis.addOrReplaceChild("rear_side_l", cube(0, 48).addBox(8.0F, -16.0F, 6.0F, 2.0F, 10.0F, 12.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("rear_side_r", cube(30, 48).addBox(-10.0F, -16.0F, 6.0F, 2.0F, 10.0F, 12.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("rear_back", cube(60, 48).addBox(-8.0F, -16.0F, 16.0F, 16.0F, 10.0F, 2.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("engine_deck", cube(98, 48).addBox(-8.0F, -11.0F, 6.0F, 16.0F, 5.0F, 10.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("fender_bl", cube(0, 72).addBox(10.0F, -12.0F, 7.0F, 2.0F, 6.0F, 10.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("fender_br", cube(26, 72).addBox(-12.0F, -12.0F, 7.0F, 2.0F, 6.0F, 10.0F), PartPose.ZERO);

        return LayerDefinition.create(mesh, 256, 256);
    }

    /** Capa TRIM: piso, antifaz de faros, parabrisas, capota de lona negra, asientos, manillar y ruedas. */
    public static LayerDefinition createTrimLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition chassis = root.addOrReplaceChild(CHASSIS, CubeListBuilder.create(), at(0, 24, 0));

        // Piso metálico inferior
        chassis.addOrReplaceChild("floor", cube(0, 92).addBox(-10.0F, -6.0F, -15.0F, 20.0F, 2.0F, 33.0F), PartPose.ZERO);

        // Antifaz negro frontal con doble faro redondo, emblema RE y direccionales ámbar
        chassis.addOrReplaceChild("visor_center", cube(108, 92).addBox(-6.0F, -17.0F, -19.0F, 12.0F, 5.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("visor_wing_l", cube(136, 92).addBox(6.0F, -17.0F, -17.0F, 3.0F, 4.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("visor_wing_r", cube(146, 92).addBox(-9.0F, -17.0F, -17.0F, 3.0F, 4.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("blinker_l", cube(156, 92).addBox(5.0F, -19.0F, -17.0F, 3.0F, 1.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("blinker_r", cube(166, 92).addBox(-8.0F, -19.0F, -17.0F, 3.0F, 1.0F, 1.0F), PartPose.ZERO);

        // Marco del parabrisas, vidrio con limpiaparabrisas y espejos retrovisores (sin solapamientos)
        chassis.addOrReplaceChild("ws_post_l", cube(192, 0).addBox(8.0F, -31.0F, -15.0F, 2.0F, 11.0F, 2.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("ws_post_r", cube(192, 0).addBox(-10.0F, -31.0F, -15.0F, 2.0F, 11.0F, 2.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("ws_top_bar", cube(192, 0).addBox(-8.0F, -31.0F, -15.0F, 16.0F, 1.0F, 2.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("ws_bot_bar", cube(192, 0).addBox(-8.0F, -21.0F, -15.0F, 16.0F, 1.0F, 2.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("ws_glass", cube(108, 102).addBox(-8.0F, -30.0F, -14.5F, 16.0F, 9.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("mirror_l", cube(192, 0).addBox(10.0F, -26.0F, -15.0F, 2.0F, 3.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("mirror_r", cube(192, 0).addBox(-12.0F, -26.0F, -15.0F, 2.0F, 3.0F, 1.0F), PartPose.ZERO);

        // Capota de lona negra (techo, cortina trasera con ventana, laterales traseros y parales centrales)
        chassis.addOrReplaceChild("roof_top", cube(0, 130).addBox(-10.5F, -33.0F, -16.0F, 21.0F, 2.0F, 35.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("curtain_back", cube(114, 130).addBox(-10.0F, -31.0F, 17.0F, 20.0F, 15.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("curtain_l", cube(158, 130).addBox(9.0F, -31.0F, 9.0F, 1.0F, 15.0F, 8.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("curtain_r", cube(178, 130).addBox(-10.0F, -31.0F, 9.0F, 1.0F, 15.0F, 8.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("post_mid_l", cube(192, 0).addBox(9.0F, -31.0F, -1.0F, 1.0F, 17.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("post_mid_r", cube(192, 0).addBox(-10.0F, -31.0F, -1.0F, 1.0F, 17.0F, 1.0F), PartPose.ZERO);

        // 3 puestos individuales: 1 delantero centrado (conductor) y 2 traseros separados (pasajeros)
        chassis.addOrReplaceChild("driver_pedestal", cube(192, 0).addBox(-3.0F, -10.0F, -7.0F, 6.0F, 4.0F, 5.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("driver_cushion", cube(0, 170).addBox(-3.5F, -12.0F, -8.0F, 7.0F, 2.0F, 6.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("driver_back", cube(28, 170).addBox(-3.5F, -18.0F, -2.0F, 7.0F, 6.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("cushion_bl", cube(46, 170).addBox(1.0F, -13.0F, 6.0F, 7.0F, 2.0F, 8.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("cushion_br", cube(46, 170).addBox(-8.0F, -13.0F, 6.0F, 7.0F, 2.0F, 8.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("back_bl", cube(80, 170).addBox(1.0F, -20.0F, 14.0F, 7.0F, 7.0F, 2.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("back_br", cube(80, 170).addBox(-8.0F, -20.0F, 14.0F, 7.0F, 7.0F, 2.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("seat_divider", cube(192, 0).addBox(-0.5F, -16.0F, 7.0F, 1.0F, 5.0F, 7.0F), PartPose.ZERO);

        // Luces traseras y guardafangos
        chassis.addOrReplaceChild("taillight_l", cube(140, 170).addBox(6.0F, -14.0F, 18.0F, 3.0F, 2.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("taillight_r", cube(150, 170).addBox(-9.0F, -14.0F, 18.0F, 3.0F, 2.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("mudflap_l", cube(192, 0).addBox(9.5F, -6.0F, 16.0F, 3.0F, 3.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("mudflap_r", cube(192, 0).addBox(-12.5F, -6.0F, 16.0F, 3.0F, 3.0F, 1.0F), PartPose.ZERO);

        // Columna de dirección, manillar y horquilla delantera (pivote en X=0, Z=-15)
        PartDefinition steering = chassis.addOrReplaceChild(STEERING, CubeListBuilder.create(), at(0, -5, -15));
        steering.addOrReplaceChild("fork_col", cube(192, 0).addBox(-1.0F, -14.0F, 2.5F, 2.0F, 9.0F, 2.0F), PartPose.ZERO);
        steering.addOrReplaceChild("handlebar", cube(192, 0).addBox(-5.0F, -15.0F, 3.0F, 10.0F, 1.0F, 1.0F), PartPose.ZERO);
        steering.addOrReplaceChild("fork_arm_l", cube(192, 0).addBox(1.5F, -5.0F, -0.5F, 1.0F, 6.0F, 2.0F), PartPose.ZERO);
        steering.addOrReplaceChild("fork_arm_r", cube(192, 0).addBox(-2.5F, -5.0F, -0.5F, 1.0F, 6.0F, 2.0F), PartPose.ZERO);

        // 3 ruedas redondas de 16 lados (cuelgan de root en y = 19, radio = 5 px → apoyadas en y = 24).
        // Cada caja de la banda tiene un ancho X ligeramente escalonado para que ninguna cara lateral sea coplanar.
        CubeListBuilder tread0 = cube(192, 0)
                .addBox(-1.45F, -5.0F, -2.0F, 2.9F, 10.0F, 4.0F)
                .addBox(-1.40F, -2.0F, -5.0F, 2.8F, 4.0F, 10.0F);
        CubeListBuilder tread45 = cube(192, 0)
                .addBox(-1.35F, -5.0F, -2.0F, 2.7F, 10.0F, 4.0F)
                .addBox(-1.30F, -2.0F, -5.0F, 2.6F, 4.0F, 10.0F);
        CubeListBuilder hubs = cube(0, 200)
                .addBox(1.55F, -5.0F, -5.0F, 0.0F, 10.0F, 10.0F)
                .addBox(-1.55F, -5.0F, -5.0F, 0.0F, 10.0F, 10.0F);

        float[][] wheelPos = {{0.0F, -15.0F}, {10.5F, 12.0F}, {-10.5F, 12.0F}};
        for (int i = 0; i < WHEELS.length; i++) {
            PartDefinition wheel = root.addOrReplaceChild(WHEELS[i], tread0, at(wheelPos[i][0], 19.0F, wheelPos[i][1]));
            wheel.addOrReplaceChild("tread_45", tread45, PartPose.rotation((float) Math.PI / 4, 0.0F, 0.0F));
            wheel.addOrReplaceChild("hubs", hubs, PartPose.ZERO);
        }

        return LayerDefinition.create(mesh, 256, 256);
    }

    /**
     * Submodelo para una capa (BODY o TRIM).
     * Aplica la cinemática de triciclo Delta, balanceo centrífugo, cabeceo de suspensión y ralentí.
     */
    public static final class Part extends EntityModel<VehicleRenderState> {
        private final ModelPart chassis;
        private final ModelPart steering;
        private final ModelPart[] wheels;

        public Part(ModelPart root) {
            super(root);
            this.chassis = root.getChild(CHASSIS);
            this.steering = ModelKit.childOrNull(chassis, STEERING);
            this.wheels = ModelKit.wheels(root, WHEELS);
        }

        @Override
        public void setupAnim(VehicleRenderState state) {
            super.setupAnim(state);

            // Ruedas en el suelo: las 3 ruedan y la delantera (índice 0) gira con el manillar
            ModelKit.spinWheels(wheels, state.wheelRoll, state.steer, FRONT_WHEELS);

            // Dirección del manillar y guardabarros delantero
            if (this.steering != null) {
                this.steering.yRot = state.steer;
            }

            // Suspensión, ralentí (solo con motor encendido) y balanceo centrífugo hacia el exterior de la curva
            float heavePx = ModelKit.heavePx(state.bodyHeave);
            if (state.isEngineRunning && state.speedRatio < IDLE_SPEED_RATIO) {
                float idleBuzz = (float) Math.sin(state.ageInTicks * IDLE_BUZZ_SPEED) * IDLE_BUZZ_AMPLITUDE;
                this.chassis.y = 24.0F + heavePx + ((float) Math.cos(state.ageInTicks * IDLE_BOB_SPEED) * IDLE_BOB_AMPLITUDE_PX);
                this.chassis.xRot = state.bodyPitch + idleBuzz * IDLE_PITCH_SHARE;
                this.chassis.zRot = idleBuzz;
            } else {
                this.chassis.y = 24.0F + heavePx;
                this.chassis.xRot = state.bodyPitch;
                this.chassis.zRot = -state.steer * (state.speedRatio * state.speedRatio) * BODY_ROLL_AT_FULL_STEER;
            }
        }
    }

    private MotocarroModels() {}
}
