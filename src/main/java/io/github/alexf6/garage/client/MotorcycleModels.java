package io.github.alexf6.garage.client;

import io.github.alexf6.garage.GarageMod;
import io.github.alexf6.garage.client.MotorcycleRenderer.MotorcycleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Modelo de Motocicleta Doble Propósito / Enduro ligera (Yamaha XTZ 125) en dos capas sobre lienzo 256x256:
 * <ul>
 *   <li>{@code BODY}: plásticos monocromáticos teñibles (tanque de combustible, colín trasero,
 *       careta del faro y guardabarros delantero alto "pico de pato").</li>
 *   <li>{@code TRIM}: chasis tubular negro, bloque motor monocilíndrico aletado en aluminio, escape elevado
 *       con protector plateado, tapas laterales blancas, sillín corrido negro, parrilla portaequipaje,
 *       basculante con monoshock, horquilla telescópica con avance (rake de -24° = -0.42 rad), fuelles negros
 *       y ruedas de radios simétricas con tacos de enduro.</li>
 * </ul>
 * Origen en el suelo ({@code y = 24}), frente hacia {@code -Z} (convención de {@link CarModels}).
 */
public final class MotorcycleModels {

    public static final ModelLayerLocation BODY = layer("body");
    public static final ModelLayerLocation TRIM = layer("trim");

    private static final String LEAN_ROOT = "lean_root";
    private static final String CHASSIS = "chassis";
    private static final String STEERING = "steering";
    private static final String FRONT_WHEEL = "front_wheel";
    private static final String SWINGARM = "swingarm";
    private static final String REAR_WHEEL = "rear_wheel";

    /** Ángulo de avance (rake) de la horquilla Enduro: -24° (-0.42 rad), inclina las barras hacia adelante (-Z). */
    private static final float FORK_RAKE = -0.42F;

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, "motorcycle"), name);
    }

    private static CubeListBuilder cube(int u, int v) {
        return CubeListBuilder.create().texOffs(u, v);
    }

    private static PartPose at(float x, float y, float z) {
        return PartPose.offset(x, y, z);
    }

    /**
     * Capa BODY: plásticos teñibles (color por defecto {@code #1846C7} Yamaha Racing Blue).
     * Incluye tanque de combustible, colín trasero, careta del faro y guardabarros pico de pato.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition leanRoot = root.addOrReplaceChild(LEAN_ROOT, CubeListBuilder.create(), at(0.0F, 24.0F, 0.0F));
        PartDefinition chassis = leanRoot.addOrReplaceChild(CHASSIS, CubeListBuilder.create(), PartPose.ZERO);

        // 1. Carrocería sobre el chasis: tanque y guardabarros / colín trasero azul
        chassis.addOrReplaceChild("bodywork", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -19.0F, -6.5F, 5.0F, 4.5F, 6.5F)     // Tanque de combustible
                .texOffs(26, 0).addBox(-2.0F, -18.5F, 9.5F, 4.0F, 2.0F, 6.0F),    // Guardabarros / colín trasero azul
                PartPose.ZERO);

        // 2. Tren delantero con ángulo de avance (rake -24° = -0.42 rad) en la pipa de dirección
        PartDefinition steering = chassis.addOrReplaceChild(STEERING, CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -17.5F, -7.5F, FORK_RAKE, 0.0F, 0.0F));

        // Careta azul del faro delantero (envuelve la óptica trapezoidal y el portanúmeros blanco)
        steering.addOrReplaceChild("headlight_cowl", cube(0, 28)
                .addBox(-2.5F, -4.5F, -2.4F, 5.0F, 6.0F, 2.0F),
                PartPose.ZERO);

        // Guardabarros delantero alto ("pico de pato" enduro) proyectado sobre la rueda de 21"
        PartDefinition frontFender = steering.addOrReplaceChild("front_fender", cube(16, 28)
                .addBox(-2.0F, 1.2F, -7.5F, 4.0F, 1.2F, 7.5F),
                PartPose.rotation(0.36F, 0.0F, 0.0F));
        frontFender.addOrReplaceChild("front_fender_tip", cube(40, 28)
                .addBox(-1.5F, 1.4F, -11.0F, 3.0F, 1.0F, 3.5F),
                PartPose.rotation(0.14F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 256, 256);
    }

    /**
     * Capa TRIM: chasis tubular, motor aletado en aluminio, calcomanías "XTZ" blancas/cian, tapas laterales blancas,
     * asiento corrido negro, parrilla trasera, escape elevado, horquilla con fuelles y ruedas de radios.
     */
    public static LayerDefinition createTrimLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition leanRoot = root.addOrReplaceChild(LEAN_ROOT, CubeListBuilder.create(), at(0.0F, 24.0F, 0.0F));
        PartDefinition chassis = leanRoot.addOrReplaceChild(CHASSIS, CubeListBuilder.create(), PartPose.ZERO);

        // 1. Chasis tubular negro, bloque motor monocilíndrico plateado y reposapiés
        PartDefinition frameEngine = chassis.addOrReplaceChild("frame_engine", CubeListBuilder.create()
                .texOffs(0, 44).addBox(-1.5F, -8.0F, -6.5F, 3.0F, 1.5F, 9.5F)     // Cuna inferior del chasis
                .texOffs(26, 44).addBox(-1.0F, -16.5F, -7.5F, 2.0F, 8.5F, 1.8F)   // Tubo frontal (downtube)
                .texOffs(36, 44).addBox(-1.8F, -15.5F, 0.0F, 3.6F, 2.5F, 9.5F)    // Subchasis trasero negro
                .texOffs(0, 58).addBox(-2.0F, -12.0F, -4.5F, 4.0F, 4.2F, 5.5F)    // Cárter motor aluminio
                .texOffs(20, 58).addBox(-1.5F, -15.5F, -4.0F, 3.0F, 3.5F, 4.0F)   // Cilindro aletado y culata
                .texOffs(36, 58).addBox(-4.5F, -7.2F, -1.0F, 9.0F, 1.0F, 1.2F)    // Reposapiés conductor
                .texOffs(58, 58).addBox(-3.5F, -11.0F, 4.5F, 7.0F, 1.0F, 1.0F)    // Reposapiés pasajero
                .texOffs(76, 58).addBox(-1.0F, -13.0F, 1.5F, 2.0F, 5.5F, 2.0F),   // Amortiguador Monoshock
                PartPose.ZERO);

        // Sistema de escape por el costado derecho (-X): colector negro y silenciador elevado con protector de aluminio
        frameEngine.addOrReplaceChild("exhaust_system", CubeListBuilder.create()
                .texOffs(0, 72).addBox(-2.8F, -12.5F, -5.5F, 1.1F, 1.5F, 7.5F)    // Colector negro desde el cilindro
                .texOffs(20, 72).addBox(-3.4F, -15.2F, 1.5F, 1.6F, 2.8F, 8.0F),   // Silenciador con escudo térmico plateado
                PartPose.rotation(0.16F, 0.0F, 0.0F));

        // 2. Detalles de carrocería sin teñir: lomo negro del tanque, tapas blancas y sillín
        PartDefinition bodywork = chassis.addOrReplaceChild("bodywork_trim", CubeListBuilder.create()
                .texOffs(44, 72).addBox(-2.0F, -19.8F, -6.0F, 4.0F, 1.0F, 5.5F),  // Lomo superior negro del tanque + tapa
                PartPose.ZERO);

        // Tapas laterales blancas (portanúmeros laterales característicos de la XTZ 125)
        bodywork.addOrReplaceChild("side_panel_l", cube(32, 86)
                .addBox(2.05F, -16.0F, -0.5F, 1.1F, 3.8F, 8.5F),
                PartPose.rotation(0.06F, 0.0F, 0.0F));
        bodywork.addOrReplaceChild("side_panel_r", cube(54, 86)
                .addBox(-3.15F, -16.0F, -0.5F, 1.1F, 3.8F, 8.5F),
                PartPose.rotation(0.06F, 0.0F, 0.0F));

        // Sillín corrido ergonómico negro de dos niveles (conductor + pasajero)
        bodywork.addOrReplaceChild("seat", CubeListBuilder.create()
                .texOffs(0, 102).addBox(-2.1F, -16.8F, -1.5F, 4.2F, 2.2F, 5.5F)   // Sección delantera conductor
                .texOffs(22, 102).addBox(-2.2F, -17.6F, 4.0F, 4.4F, 2.6F, 5.5F),  // Sección trasera pasajero (termina exacto en Z=9.5 donde inicia parrilla)
                PartPose.rotation(0.05F, 0.0F, 0.0F));

        // Parrilla portaequipaje trasera metálica negra, faro trasero rojo, direccionales y guardafango
        bodywork.addOrReplaceChild("rear_rack", CubeListBuilder.create()
                .texOffs(46, 102).addBox(-2.6F, -19.45F, 9.5F, 5.2F, 0.95F, 6.0F) // Parrilla superior asentada sobre el colín (-18.5F)
                .texOffs(70, 102).addBox(-1.5F, -17.8F, 15.51F, 3.0F, 1.8F, 1.0F) // Luz de freno roja (stop) sobre cara posterior
                .texOffs(80, 102).addBox(2.2F, -17.4F, 14.2F, 1.4F, 1.0F, 1.0F)   // Direccional trasera izq
                .texOffs(80, 102).addBox(-3.6F, -17.4F, 14.2F, 1.4F, 1.0F, 1.0F), // Direccional trasera der
                PartPose.ZERO);
        bodywork.addOrReplaceChild("mudflap", cube(88, 102)
                .addBox(-1.5F, -16.5F, 14.8F, 3.0F, 4.0F, 0.8F),
                PartPose.rotation(0.28F, 0.0F, 0.0F));

        // 3. Basculante trasero de aluminio (swingarm) y Rueda Trasera de radios (12x12, idéntica a la delantera)
        PartDefinition swingarm = chassis.addOrReplaceChild(SWINGARM, CubeListBuilder.create()
                .texOffs(0, 116).addBox(1.30F, -1.0F, 0.0F, 1.0F, 2.0F, 11.5F)    // Brazo izquierdo aluminio
                .texOffs(26, 116).addBox(-2.30F, -1.0F, 0.0F, 1.0F, 2.0F, 11.5F), // Brazo derecho aluminio "YAMAHA"
                at(0.0F, -6.0F, 1.0F));

        // Ruedas redondeadas de radios (12x12, radio 6 px): banda de rodadura con tacos y tapas con radios cruzados
        CubeListBuilder wheelTread0 = cube(192, 24)
                .addBox(-0.95F, -6.0F, -2.5F, 1.9F, 12.0F, 5.0F)
                .addBox(-0.90F, -2.5F, -6.0F, 1.8F, 5.0F, 12.0F);
        CubeListBuilder wheelTread45 = cube(192, 24)
                .addBox(-0.85F, -6.0F, -2.5F, 1.7F, 12.0F, 5.0F)
                .addBox(-0.80F, -2.5F, -6.0F, 1.6F, 5.0F, 12.0F);
        CubeListBuilder wheelHubs = cube(0, 200)
                .addBox(1.05F, -6.0F, -6.0F, 0.0F, 12.0F, 12.0F)
                .addBox(-1.05F, -6.0F, -6.0F, 0.0F, 12.0F, 12.0F);

        PartDefinition rearWheel = swingarm.addOrReplaceChild(REAR_WHEEL, wheelTread0, at(0.0F, 0.0F, 10.5F));
        rearWheel.addOrReplaceChild("tread_45", wheelTread45, PartPose.rotation((float) Math.PI / 4, 0.0F, 0.0F));
        rearWheel.addOrReplaceChild("hubs", wheelHubs, PartPose.ZERO);

        // 4. Tren Delantero y Dirección con inclinación de avance (Rake de -24° = -0.42 rad)
        PartDefinition steering = chassis.addOrReplaceChild(STEERING, CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -17.5F, -7.5F, FORK_RAKE, 0.0F, 0.0F));

        // Horquillas telescópicas: tijas, barras superiores cromadas, fuelles negros de goma y botellas plateadas
        steering.addOrReplaceChild("forks", CubeListBuilder.create()
                .texOffs(0, 132).addBox(-2.6F, -1.0F, -0.8F, 5.2F, 1.2F, 1.8F)    // Tija superior
                .texOffs(16, 132).addBox(-2.65F, 1.95F, -0.95F, 5.3F, 1.25F, 2.1F) // Tija inferior (anchura 5.3F abraza los fuelles sin z-fighting)
                .texOffs(32, 132).addBox(1.35F, -1.5F, -0.5F, 1.1F, 4.7F, 1.1F)   // Barra cromada sup izq (hasta base tija)
                .texOffs(32, 132).addBox(-2.45F, -1.5F, -0.5F, 1.1F, 4.7F, 1.1F)  // Barra cromada sup der (hasta base tija)
                .texOffs(40, 132).addBox(1.20F, 3.2F, -0.7F, 1.4F, 4.1F, 1.5F)    // Fuelle de goma negro izq (inicia bajo la tija inferior)
                .texOffs(48, 132).addBox(-2.60F, 3.2F, -0.7F, 1.4F, 4.1F, 1.5F)   // Fuelle de goma negro der (inicia bajo la tija inferior)
                .texOffs(56, 132).addBox(1.25F, 7.3F, -0.6F, 1.3F, 5.8F, 1.4F)    // Botella inferior aluminio izq
                .texOffs(64, 132).addBox(-2.55F, 7.3F, -0.6F, 1.3F, 5.8F, 1.4F),  // Botella inferior aluminio der
                PartPose.ZERO);

        // Óptica trapezoidal del faro, portanúmeros blanco frontal y direccionales ámbar
        steering.addOrReplaceChild("headlight_trim", CubeListBuilder.create()
                .texOffs(0, 148).addBox(-2.0F, -5.2F, -2.55F, 4.0F, 2.5F, 1.0F)   // Portanúmeros frontal blanco
                .texOffs(12, 148).addBox(-1.5F, -2.5F, -2.65F, 3.0F, 3.0F, 1.0F)  // Óptica cristalina del faro
                .texOffs(22, 148).addBox(2.5F, -2.2F, -1.8F, 1.8F, 1.2F, 1.0F)    // Direccional ámbar izq
                .texOffs(30, 148).addBox(-4.3F, -2.2F, -1.8F, 1.8F, 1.2F, 1.0F),  // Direccional ámbar der
                PartPose.ZERO);

        // Manillar con puños, maniguetas y espejos retrovisores altos (compensando el rake para quedar horizontales)
        steering.addOrReplaceChild("handlebars", CubeListBuilder.create()
                .texOffs(0, 158).addBox(-1.8F, -3.0F, -0.2F, 3.6F, 2.0F, 1.0F)    // Torretas y velocímetro
                .texOffs(12, 158).addBox(-7.0F, -4.0F, -0.2F, 14.0F, 1.0F, 1.0F)  // Barra de manillar y puños negros
                .texOffs(44, 158).addBox(-6.5F, -3.8F, -1.2F, 2.5F, 0.6F, 1.0F)   // Manigueta de freno der
                .texOffs(44, 158).addBox(4.0F, -3.8F, -1.2F, 2.5F, 0.6F, 1.0F)    // Manigueta de embrague izq
                .texOffs(54, 158).addBox(4.8F, -7.2F, -0.1F, 0.8F, 3.2F, 0.8F)    // Varilla espejo izq
                .texOffs(54, 158).addBox(-5.6F, -7.2F, -0.1F, 0.8F, 3.2F, 0.8F)   // Varilla espejo der
                .texOffs(60, 158).addBox(4.5F, -9.2F, -0.2F, 2.8F, 2.0F, 0.8F)    // Espejo retrovisor izq
                .texOffs(60, 158).addBox(-7.3F, -9.2F, -0.2F, 2.8F, 2.0F, 0.8F),  // Espejo retrovisor der
                PartPose.rotation(-FORK_RAKE, 0.0F, 0.0F));

        // Rueda delantera de radios (12x12), hija de steering en (0, 12.6, 0) para girar y rodar con la horquilla
        PartDefinition frontWheel = steering.addOrReplaceChild(FRONT_WHEEL, wheelTread0, at(0.0F, 12.6F, 0.0F));
        frontWheel.addOrReplaceChild("tread_45", wheelTread45, PartPose.rotation((float) Math.PI / 4, 0.0F, 0.0F));
        frontWheel.addOrReplaceChild("hubs", wheelHubs, PartPose.ZERO);

        return LayerDefinition.create(mesh, 256, 256);
    }

    /**
     * Submodelo para una capa ({@code BODY} o {@code TRIM}).
     * Ejecuta la cinemática de la Yamaha XTZ 125: rodadura continua de ruedas, giro de la horquilla con
     * su ángulo de avance (-24°), pulsación monocilíndrica en ralentí y tumbada dinámica en curva.
     */
    public static final class Part extends EntityModel<MotorcycleRenderState> {
        private final ModelPart leanRoot;
        private final ModelPart chassis;
        private final ModelPart steering;
        private final ModelPart frontWheel;
        private final ModelPart rearWheel;

        public Part(ModelPart root) {
            super(root);
            this.leanRoot = root.getChild(LEAN_ROOT);
            this.chassis = leanRoot.getChild(CHASSIS);
            this.steering = chassis.hasChild(STEERING) ? chassis.getChild(STEERING) : null;
            this.frontWheel = (this.steering != null && this.steering.hasChild(FRONT_WHEEL))
                    ? this.steering.getChild(FRONT_WHEEL)
                    : null;
            ModelPart swingarm = chassis.hasChild(SWINGARM) ? chassis.getChild(SWINGARM) : null;
            this.rearWheel = (swingarm != null && swingarm.hasChild(REAR_WHEEL))
                    ? swingarm.getChild(REAR_WHEEL)
                    : null;
        }

        @Override
        public void setupAnim(MotorcycleRenderState state) {
            super.setupAnim(state);

            // a) Tumbada dinámica (lean angle) sobre la línea de contacto con el suelo (Y = 24)
            this.leanRoot.zRot = state.lean;

            // b) Suspensión de largo recorrido (hunde la horquilla delantera al frenar) y ralentí monocilíndrico 4T
            float heavePx = -state.bodyHeave * 16.0F;
            if (state.isEngineRunning && state.speedRatio < 0.05F) {
                float idle = Mth.sin(state.ageInTicks * 1.4F) * 0.02F;
                this.chassis.y = heavePx + (Mth.cos(state.ageInTicks * 2.8F) * 0.08F);
                this.chassis.yRot = idle;
                this.chassis.xRot = state.bodyPitch + idle * 0.4F;
            } else {
                this.chassis.y = heavePx;
                this.chassis.yRot = 0.0F;
                this.chassis.xRot = state.bodyPitch;
            }

            // c) Giro del manillar y la horquilla (conservando el avance FORK_RAKE = -0.42 rad)
            if (this.steering != null) {
                this.steering.xRot = FORK_RAKE;
                this.steering.yRot = state.steer;
            }

            // d) Rodadura continua de ambas ruedas vinculada al avance real
            if (this.frontWheel != null) {
                this.frontWheel.xRot = state.wheelRoll;
            }
            if (this.rearWheel != null) {
                this.rearWheel.xRot = state.wheelRoll;
            }
        }
    }

    private MotorcycleModels() {}
}
