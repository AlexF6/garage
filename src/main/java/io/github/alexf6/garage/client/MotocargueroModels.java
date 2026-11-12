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
import net.minecraft.util.Mth;

/**
 * Modelo de Motocarguero / Trimoto de Carga 200cc en dos capas que comparten textura (256x256):
 * <ul>
 *   <li>{@code BODY}: partes de chapa y carenado teñibles (tanque de combustible con espacio para gráfico "200"
 *       y guardabarros delantero sobre la horquilla).</li>
 *   <li>{@code TRIM}: chasis tubular reforzado de acero negro, bloque motor monocilíndrico 200cc con aletas,
 *       escape con silenciador, horquilla telescópica reforzada con resortes externos, manillar ancho con espejos,
 *       faro frontal, sillín del conductor, tabique vertical con rejilla de seguridad y respaldo acolchado,
 *       platón de carga rectangular de gran capacidad con barandillas laterales y compuerta trasera,
 *       y 3 ruedas de carga pesada con rines de acero en configuración delta.</li>
 * </ul>
 * Origen en el suelo ({@code y = 24}) y frente hacia {@code -Z} (convención del mod Garage).
 * Las caras de las ruedas y cajas adyacentes están escalonadas en 0.05F para eliminar todo z-fighting.
 */
public final class MotocargueroModels {

    public static final ModelLayerLocation BODY = layer("body");
    public static final ModelLayerLocation TRIM = layer("trim");

    private static final String CHASSIS = "chassis";
    private static final String STEERING = "steering";
    private static final String FRONT_WHEEL = "wheel_front";
    private static final String[] REAR_WHEELS = {"wheel_bl", "wheel_br"};

    /** Inclinación (rake) de la horquilla delantera de carga: -13° (-0.22 rad) hacia el frente (-Z). */
    private static final float FORK_RAKE = -0.22F;

    private static final ModelKit.IdleVibration IDLE = new ModelKit.IdleVibration(1.5F, 0.02F, 3.0F, 0.10F, 0.4F);
    private static final float IDLE_ROLL_SHARE = 0.5F;
    /** Balanceo centrífugo hacia el exterior de la curva en virajes rápidos. */
    private static final float BODY_ROLL_AT_FULL_STEER = 0.18F;

    private static ModelLayerLocation layer(String name) {
        return ModelKit.layer("motocarguero", name);
    }

    /**
     * Capa BODY: piezas teñibles con el color del vehículo (blanco glaciar por defecto).
     * Incluye el tanque de combustible de motocicleta y el guardabarros envolvente delantero.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition chassis = root.addOrReplaceChild(CHASSIS, CubeListBuilder.create(), at(0, 24, 0));

        // 1. Tanque de combustible de moto (en chasis central frente al conductor) con hueco para logo "200"
        chassis.addOrReplaceChild("fuel_tank", cube(0, 0)
                .addBox(-3.0F, -18.5F, -8.0F, 6.0F, 5.0F, 8.0F), PartPose.ZERO);
        // Rebordes laterales del tanque (rodilleras aerodinámicas de chapa)
        chassis.addOrReplaceChild("tank_knee_l", cube(28, 0)
                .addBox(2.75F, -17.5F, -6.5F, 0.75F, 3.5F, 5.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("tank_knee_r", cube(28, 0)
                .addBox(-3.50F, -17.5F, -6.5F, 0.75F, 3.5F, 5.0F), PartPose.ZERO);

        // 2. Tren delantero en dirección: guardabarros delantero envolvente y careta superior
        PartDefinition steering = chassis.addOrReplaceChild(STEERING, CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -17.0F, -10.0F, FORK_RAKE, 0.0F, 0.0F));

        // Guardabarros delantero alto curvado sobre la rueda única
        steering.addOrReplaceChild("fender_top", cube(40, 0)
                .addBox(-2.5F, 4.2F, -4.5F, 5.0F, 1.3F, 9.0F), PartPose.ZERO);
        steering.addOrReplaceChild("fender_skirt", cube(68, 0)
                .addBox(-2.5F, 5.5F, 2.5F, 5.0F, 4.0F, 2.0F), PartPose.ZERO);
        steering.addOrReplaceChild("fender_beak", cube(82, 0)
                .addBox(-2.0F, 4.6F, -6.5F, 4.0F, 1.0F, 2.0F), PartPose.ZERO);

        // Careta del faro delantero
        steering.addOrReplaceChild("headlight_cowl", cube(94, 0)
                .addBox(-2.5F, -3.0F, -2.5F, 5.0F, 4.0F, 1.5F), PartPose.ZERO);

        return LayerDefinition.create(mesh, 256, 256);
    }

    /**
     * Capa TRIM: chasis tubular negro, motor 200cc, escape, horquilla reforzada, manillar,
     * faro, sillín, mampara de seguridad con rejilla y respaldo, platón de carga de acero
     * con barandillas y compuerta, y 3 ruedas pesadas.
     */
    public static LayerDefinition createTrimLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition chassis = root.addOrReplaceChild(CHASSIS, CubeListBuilder.create(), at(0, 24, 0));

        // 1. Bastidor y chasis de acero pesado
        chassis.addOrReplaceChild("backbone_tube", cube(0, 16)
                .addBox(-1.5F, -15.5F, -9.0F, 3.0F, 2.5F, 9.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("lower_cradle", cube(24, 16)
                .addBox(-2.5F, -6.5F, -7.5F, 5.0F, 1.5F, 13.5F), PartPose.ZERO);
        chassis.addOrReplaceChild("downtube_l", cube(62, 16)
                .addBox(1.5F, -14.0F, -8.5F, 1.0F, 8.0F, 1.5F), PartPose.ZERO);
        chassis.addOrReplaceChild("downtube_r", cube(62, 16)
                .addBox(-2.5F, -14.0F, -8.5F, 1.0F, 8.0F, 1.5F), PartPose.ZERO);
        chassis.addOrReplaceChild("rear_chassis_l", cube(68, 16)
                .addBox(6.5F, -6.5F, 5.0F, 2.0F, 2.0F, 20.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("rear_chassis_r", cube(68, 16)
                .addBox(-8.5F, -6.5F, 5.0F, 2.0F, 2.0F, 20.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("rear_crossmember", cube(112, 16)
                .addBox(-8.5F, -6.5F, 23.0F, 17.0F, 2.0F, 2.0F), PartPose.ZERO);

        // Eje trasero rígido, diferencial y suspensión de ballestas
        chassis.addOrReplaceChild("rear_axle", cube(0, 38)
                .addBox(-12.0F, -7.0F, 14.0F, 24.0F, 2.0F, 2.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("differential", cube(52, 38)
                .addBox(-2.5F, -8.0F, 13.0F, 5.0F, 4.0F, 4.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("leaf_spring_l", cube(70, 38)
                .addBox(6.5F, -7.5F, 10.0F, 2.0F, 1.0F, 10.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("leaf_spring_r", cube(70, 38)
                .addBox(-8.5F, -7.5F, 10.0F, 2.0F, 1.0F, 10.0F), PartPose.ZERO);

        // 2. Motor 200cc monocilíndrico con aletas, carburador, escape y estribos
        chassis.addOrReplaceChild("engine_crankcase", cube(0, 48)
                .addBox(-2.5F, -10.5F, -5.5F, 5.0F, 4.5F, 6.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("engine_cylinder", cube(22, 48)
                .addBox(-2.0F, -14.0F, -4.5F, 4.0F, 3.5F, 4.5F), PartPose.ZERO);
        chassis.addOrReplaceChild("carburetor", cube(40, 48)
                .addBox(-1.5F, -12.5F, 0.5F, 3.0F, 2.5F, 3.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("exhaust_header", cube(52, 48)
                .addBox(2.2F, -11.5F, -4.8F, 1.2F, 4.5F, 1.5F), PartPose.ZERO);
        chassis.addOrReplaceChild("exhaust_muffler", cube(58, 48)
                .addBox(2.1F, -6.0F, -3.3F, 1.6F, 1.8F, 12.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("footpeg_l", cube(86, 48)
                .addBox(3.0F, -6.0F, -3.0F, 3.5F, 1.0F, 1.5F), PartPose.ZERO);
        chassis.addOrReplaceChild("footpeg_r", cube(86, 48)
                .addBox(-6.5F, -6.0F, -3.0F, 3.5F, 1.0F, 1.5F), PartPose.ZERO);

        // 3. Sillín del conductor y soporte
        chassis.addOrReplaceChild("driver_saddle", cube(0, 64)
                .addBox(-2.5F, -14.5F, 0.0F, 5.0F, 2.5F, 5.5F), PartPose.ZERO);
        chassis.addOrReplaceChild("saddle_support", cube(22, 64)
                .addBox(-2.0F, -12.0F, 1.0F, 4.0F, 3.0F, 4.0F), PartPose.ZERO);

        // 4. Mampara separadora vertical con respaldo acolchado y rejilla protectora
        chassis.addOrReplaceChild("backrest", cube(38, 64)
                .addBox(-2.5F, -18.0F, 5.0F, 5.0F, 4.0F, 1.5F), PartPose.ZERO);
        chassis.addOrReplaceChild("bulkhead_top", cube(52, 64)
                .addBox(-10.0F, -23.0F, 5.5F, 20.0F, 1.5F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("bulkhead_post_l", cube(94, 64)
                .addBox(9.0F, -22.0F, 5.5F, 1.0F, 14.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("bulkhead_post_r", cube(94, 64)
                .addBox(-10.0F, -22.0F, 5.5F, 1.0F, 14.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("bulkhead_bars", cube(98, 64)
                .addBox(-8.0F, -21.5F, 5.6F, 16.0F, 13.5F, 0.8F), PartPose.ZERO);

        // 5. Platón de carga de acero (caja de 54 slots): piso acanalado, barandillas y compuerta
        chassis.addOrReplaceChild("bed_floor", cube(0, 80)
                .addBox(-9.5F, -8.0F, 6.5F, 19.0F, 1.5F, 19.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("bed_front_wall", cube(76, 80)
                .addBox(-9.5F, -13.0F, 6.0F, 19.0F, 5.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("bed_side_wall_l", cube(0, 102)
                .addBox(9.5F, -13.0F, 6.5F, 1.0F, 5.0F, 19.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("bed_side_wall_r", cube(0, 102)
                .addBox(-10.5F, -13.0F, 6.5F, 1.0F, 5.0F, 19.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("bed_rail_l", cube(40, 102)
                .addBox(9.5F, -17.0F, 6.5F, 1.0F, 1.5F, 19.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("bed_rail_r", cube(40, 102)
                .addBox(-10.5F, -17.0F, 6.5F, 1.0F, 1.5F, 19.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("bed_stakes_l", cube(80, 102)
                .addBox(9.6F, -15.5F, 12.0F, 0.8F, 2.5F, 8.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("bed_stakes_r", cube(80, 102)
                .addBox(-10.4F, -15.5F, 12.0F, 0.8F, 2.5F, 8.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("tailgate_panel", cube(0, 128)
                .addBox(-9.5F, -13.0F, 25.5F, 19.0F, 5.0F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("tailgate_rail", cube(40, 128)
                .addBox(-9.5F, -17.0F, 25.5F, 19.0F, 1.5F, 1.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("tailgate_latches", cube(80, 128)
                .addBox(-8.0F, -15.5F, 25.6F, 16.0F, 2.5F, 0.6F), PartPose.ZERO);

        // Guardabarros traseros de acero sobre las ruedas gemelas y luces de cola
        chassis.addOrReplaceChild("fender_rear_l", cube(0, 140)
                .addBox(10.5F, -13.5F, 9.5F, 3.5F, 5.5F, 11.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("fender_rear_r", cube(0, 140)
                .addBox(-14.0F, -13.5F, 9.5F, 3.5F, 5.5F, 11.0F), PartPose.ZERO);
        chassis.addOrReplaceChild("taillights", cube(30, 140)
                .addBox(-9.0F, -6.5F, 25.6F, 18.0F, 2.0F, 1.0F), PartPose.ZERO);

        // 6. Tren de dirección delantero: horquilla reforzada, resortes dobles, faro, manillar y espejos
        PartDefinition steering = chassis.addOrReplaceChild(STEERING, CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -17.0F, -10.0F, FORK_RAKE, 0.0F, 0.0F));

        steering.addOrReplaceChild("triple_clamp_top", cube(0, 160)
                .addBox(-3.5F, -0.5F, -1.0F, 7.0F, 1.5F, 2.0F), PartPose.ZERO);
        steering.addOrReplaceChild("triple_clamp_bottom", cube(0, 160)
                .addBox(-3.5F, 3.5F, -1.0F, 7.0F, 1.5F, 2.0F), PartPose.ZERO);
        steering.addOrReplaceChild("fork_leg_l", cube(18, 160)
                .addBox(2.2F, 0.0F, -0.8F, 1.4F, 12.0F, 1.6F), PartPose.ZERO);
        steering.addOrReplaceChild("fork_leg_r", cube(18, 160)
                .addBox(-3.6F, 0.0F, -0.8F, 1.4F, 12.0F, 1.6F), PartPose.ZERO);
        steering.addOrReplaceChild("fork_spring_l", cube(26, 160)
                .addBox(2.1F, 4.5F, -0.9F, 1.6F, 6.0F, 1.8F), PartPose.ZERO);
        steering.addOrReplaceChild("fork_spring_r", cube(26, 160)
                .addBox(-3.7F, 4.5F, -0.9F, 1.6F, 6.0F, 1.8F), PartPose.ZERO);

        steering.addOrReplaceChild("headlight", cube(34, 160)
                .addBox(-2.0F, -2.5F, -3.8F, 4.0F, 4.0F, 2.0F), PartPose.ZERO);
        steering.addOrReplaceChild("front_blinkers", cube(46, 160)
                .addBox(-4.8F, -1.5F, -2.0F, 9.6F, 1.5F, 1.0F), PartPose.ZERO);

        // Manillar compensando el rake para quedar horizontal
        steering.addOrReplaceChild("gauge_cluster", cube(68, 160)
                .addBox(-1.5F, -2.5F, -0.5F, 3.0F, 2.0F, 1.5F), PartPose.rotation(-FORK_RAKE, 0.0F, 0.0F));
        steering.addOrReplaceChild("handlebars", cube(78, 160)
                .addBox(-7.5F, -3.5F, -0.5F, 15.0F, 1.2F, 1.2F), PartPose.rotation(-FORK_RAKE, 0.0F, 0.0F));
        steering.addOrReplaceChild("levers", cube(110, 160)
                .addBox(-6.5F, -3.2F, -1.6F, 13.0F, 0.6F, 1.1F), PartPose.rotation(-FORK_RAKE, 0.0F, 0.0F));
        steering.addOrReplaceChild("mirror_l", cube(138, 160)
                .addBox(5.0F, -7.5F, -0.5F, 2.5F, 4.0F, 0.8F), PartPose.rotation(-FORK_RAKE, 0.0F, 0.0F));
        steering.addOrReplaceChild("mirror_r", cube(138, 160)
                .addBox(-7.5F, -7.5F, -0.5F, 2.5F, 4.0F, 0.8F), PartPose.rotation(-FORK_RAKE, 0.0F, 0.0F));

        // 7. Ruedas pesadas (1 delantera en la dirección, 2 traseras en el eje posterior)
        // Cajas escalonadas por 0.05F para evitar caras coplanares (z-fighting)
        CubeListBuilder wheelTread0 = cube(192, 0)
                .addBox(-1.95F, -6.0F, -2.5F, 3.9F, 12.0F, 5.0F)
                .addBox(-1.90F, -2.5F, -6.0F, 3.8F, 5.0F, 12.0F);
        CubeListBuilder wheelTread45 = cube(192, 0)
                .addBox(-1.85F, -6.0F, -2.5F, 3.7F, 12.0F, 5.0F)
                .addBox(-1.80F, -2.5F, -6.0F, 3.6F, 5.0F, 12.0F);
        CubeListBuilder wheelHubs = cube(0, 200)
                .addBox(2.05F, -6.0F, -6.0F, 0.0F, 12.0F, 12.0F)
                .addBox(-2.05F, -6.0F, -6.0F, 0.0F, 12.0F, 12.0F);

        // Rueda delantera: montada bajo steering para girar con el manillar
        PartDefinition frontWheel = steering.addOrReplaceChild(FRONT_WHEEL, wheelTread0, at(0.0F, 11.5F, 0.0F));
        frontWheel.addOrReplaceChild("tread_45", wheelTread45, PartPose.rotation((float) Math.PI / 4, 0.0F, 0.0F));
        frontWheel.addOrReplaceChild("hubs", wheelHubs, PartPose.ZERO);

        // Ruedas traseras gemelas: apoyadas en el suelo (y = 18 px respecto a root, radio 6 px -> base en y = 24)
        PartDefinition wheelBL = root.addOrReplaceChild(REAR_WHEELS[0], wheelTread0, at(12.25F, 18.0F, 15.0F));
        wheelBL.addOrReplaceChild("tread_45", wheelTread45, PartPose.rotation((float) Math.PI / 4, 0.0F, 0.0F));
        wheelBL.addOrReplaceChild("hubs", wheelHubs, PartPose.ZERO);

        PartDefinition wheelBR = root.addOrReplaceChild(REAR_WHEELS[1], wheelTread0, at(-12.25F, 18.0F, 15.0F));
        wheelBR.addOrReplaceChild("tread_45", wheelTread45, PartPose.rotation((float) Math.PI / 4, 0.0F, 0.0F));
        wheelBR.addOrReplaceChild("hubs", wheelHubs, PartPose.ZERO);

        return LayerDefinition.create(mesh, 256, 256);
    }

    /**
     * Submodelo para una capa ({@code BODY} o {@code TRIM}).
     * Aplica la cinemática de triciclo de carga: dirección delantera, rodadura de 3 ruedas,
     * balanceo centrífugo en curvas, cabeceo de suspensión y vibración de ralentí del motor 200cc.
     */
    public static final class Part extends EntityModel<VehicleRenderState> {
        private final ModelPart chassis;
        private final ModelPart steering;
        private final ModelPart[] wheels;

        public Part(ModelPart root) {
            super(root);
            this.chassis = root.getChild(CHASSIS);
            this.steering = ModelKit.childOrNull(chassis, STEERING);
            // La delantera cuelga de la dirección (gira con ella); las traseras, de la raíz
            this.wheels = new ModelPart[] {
                    this.steering != null ? ModelKit.childOrNull(this.steering, FRONT_WHEEL) : null,
                    ModelKit.childOrNull(root, REAR_WHEELS[0]),
                    ModelKit.childOrNull(root, REAR_WHEELS[1])};
        }

        @Override
        public void setupAnim(VehicleRenderState state) {
            super.setupAnim(state);

            // Dirección: manillar, horquilla, faro, guardabarros y rueda delantera giran juntos
            if (this.steering != null) {
                this.steering.yRot = state.steer;
            }

            // Rodadura de las 3 ruedas; ninguna gira por sí misma (la delantera ya lo hace con la dirección)
            ModelKit.spinWheels(wheels, state.wheelRoll, 0.0F, 0);

            // Suspensión, ralentí y balanceo centrífugo de triciclo delta
            float heavePx = ModelKit.heavePx(state.bodyHeave);
            if (state.isEngineRunning && state.speedRatio < ModelKit.IDLE_SPEED_RATIO) {
                float idleBuzz = IDLE.sway(state.ageInTicks);
                this.chassis.y = 24.0F + heavePx + IDLE.bobPx(state.ageInTicks);
                this.chassis.xRot = state.bodyPitch + idleBuzz * IDLE.pitchShare();
                this.chassis.zRot = idleBuzz * IDLE_ROLL_SHARE;
            } else {
                this.chassis.y = 24.0F + heavePx;
                this.chassis.xRot = state.bodyPitch;
                this.chassis.zRot = -state.steer * (state.speedRatio * state.speedRatio) * BODY_ROLL_AT_FULL_STEER;
            }
        }
    }

    private MotocargueroModels() {}
}
