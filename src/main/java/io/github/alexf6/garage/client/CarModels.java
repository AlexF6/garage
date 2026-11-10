package io.github.alexf6.garage.client;

import io.github.alexf6.garage.GarageMod;
import io.github.alexf6.garage.client.CarRenderer.CarRenderState;
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
 * Jeep Willys "Yipao" cafetero hecho con cubos, en dos capas que comparten textura (256x256):
 * la carrocería (se tiñe con el color del carro, rojo por defecto) y el resto (chasis, carpa, bultos de
 * café, bandera, ruedas, asientos) que no se tiñe. Origen en el suelo (y = 24) y frente hacia -Z.
 * Escala aprox. 1.4x la ficha de diseño original para que quepan 4 jugadores sentados bajo la carpa.
 */
public final class CarModels {

    public static final ModelLayerLocation BODY = layer("body");
    public static final ModelLayerLocation TRIM = layer("trim");

    public static final String[] WHEELS = {"wheel_fl", "wheel_fr", "wheel_bl", "wheel_br"};
    /** Todo lo que no son ruedas cuelga de aquí (pivote en el suelo) para que la suspensión no mueva las ruedas. */
    private static final String CHASSIS = "chassis";
    private static final String FLAG = "flag_cloth";

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, "car"), name);
    }

    private static CubeListBuilder cube(int u, int v) {
        return CubeListBuilder.create().texOffs(u, v);
    }

    private static PartPose at(float x, float y, float z) {
        return PartPose.offset(x, y, z);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition chassis = root.addOrReplaceChild(CHASSIS, CubeListBuilder.create(), at(0, 24, 0));

        // Tina de la cabina abierta: piso, costados, pared trasera y tablero.
        // Regla contra el z-fighting: las piezas solo se tocan, nunca se solapan (sin caras coplanares visibles).
        chassis.addOrReplaceChild("floor", cube(0, 0).addBox(-13, -10, -8, 26, 2, 32), PartPose.ZERO);
        CubeListBuilder wall = cube(118, 0).addBox(0, -20, -7, 2, 10, 31);
        chassis.addOrReplaceChild("wall_l", wall, at(11, 0, 0));
        chassis.addOrReplaceChild("wall_r", wall, at(-13, 0, 0));
        chassis.addOrReplaceChild("rear_wall", cube(188, 0).addBox(-13, -20, 24, 26, 10, 2), PartPose.ZERO);
        chassis.addOrReplaceChild("dash", cube(188, 14).addBox(-13, -20, -10, 26, 10, 3), PartPose.ZERO);

        // Capó, parrilla y guardabarros delanteros
        chassis.addOrReplaceChild("hood", cube(0, 36).addBox(-8, -21, -24, 16, 13, 14), PartPose.ZERO);
        chassis.addOrReplaceChild("grille", cube(130, 52).addBox(-8, -21, -25, 16, 13, 1), PartPose.ZERO);
        CubeListBuilder fender = cube(68, 36).addBox(0, -19, -24, 8, 3, 14);
        chassis.addOrReplaceChild("fender_l", fender, at(8, 0, 0));
        chassis.addOrReplaceChild("fender_r", fender, at(-16, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
    }

    public static LayerDefinition createTrimLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition chassis = root.addOrReplaceChild(CHASSIS, CubeListBuilder.create(), at(0, 24, 0));

        // Chasis, bómper, faros y placa amarilla
        chassis.addOrReplaceChild("frame", cube(0, 68).addBox(-9, -8, -24, 18, 3, 46), PartPose.ZERO);
        chassis.addOrReplaceChild("bumper", cube(130, 44).addBox(-14, -10, -28, 28, 3, 3), PartPose.ZERO);
        CubeListBuilder headlight = cube(166, 52).addBox(0, 0, 0, 4, 4, 2);
        chassis.addOrReplaceChild("headlight_l", headlight, at(8, -21, -26));
        chassis.addOrReplaceChild("headlight_r", headlight, at(-12, -21, -26));
        chassis.addOrReplaceChild("plate", cube(180, 52).addBox(1, -13, -26, 5, 3, 1), PartPose.ZERO);

        // Marco del parabrisas y parales de la carpa (misma región negra); los parales arrancan
        // encima del tablero y de los costados para no solaparse con ellos
        CubeListBuilder black = cube(194, 28);
        chassis.addOrReplaceChild("bar_top", black.addBox(-12, -32, -10, 24, 1, 1), PartPose.ZERO);
        chassis.addOrReplaceChild("bar_bottom", cube(194, 28).addBox(-12, -21, -10, 24, 1, 1), PartPose.ZERO);
        chassis.addOrReplaceChild("post_fl", cube(194, 28).addBox(12, -34, -10, 1, 14, 1), PartPose.ZERO);
        chassis.addOrReplaceChild("post_fr", cube(194, 28).addBox(-13, -34, -10, 1, 14, 1), PartPose.ZERO);
        chassis.addOrReplaceChild("post_bl", cube(194, 28).addBox(12, -34, 23, 1, 14, 1), PartPose.ZERO);
        chassis.addOrReplaceChild("post_br", cube(194, 28).addBox(-13, -34, 23, 1, 14, 1), PartPose.ZERO);

        // Carpa de lona, faldones laterales con el rótulo y bultos de café
        chassis.addOrReplaceChild("roof", cube(0, 118).addBox(-15, -35, -11, 30, 1, 38), PartPose.ZERO);
        CubeListBuilder curtain = cube(138, 118).addBox(0, -34, 12, 1, 12, 12);
        chassis.addOrReplaceChild("curtain_l", curtain, at(13, 0, 0));
        chassis.addOrReplaceChild("curtain_r", curtain, at(-14, 0, 0));
        chassis.addOrReplaceChild("sack_lower", cube(0, 160).addBox(-10, -39, 0, 20, 4, 17), PartPose.ZERO);
        chassis.addOrReplaceChild("sack_upper", cube(76, 160).addBox(-8, -43, 2, 16, 4, 14), PartPose.ZERO);

        // Bandera de Colombia en la esquina trasera de la carpa
        chassis.addOrReplaceChild("flag_pole", cube(138, 144).addBox(0, -57, 0, 1, 22, 1), at(12, 0, 22));
        // Pivote en el eje del asta y arriba del paño: al ondear gira alrededor del asta sin despegarse
        chassis.addOrReplaceChild(FLAG, cube(146, 144).addBox(-0.5F, 0, -12.5F, 1, 8, 12), at(12.5F, -57, 22.5F));

        // Volante (lado del conductor) y 4 asientos: cojín + respaldo
        chassis.addOrReplaceChild("steering_wheel", cube(174, 144).addBox(-3, -3, 0, 6, 6, 1), at(7, -23, -5));
        CubeListBuilder cushion = cube(190, 144).addBox(0, -12, 0, 8, 2, 6);
        CubeListBuilder back = cube(190, 154).addBox(0, -22, 0, 8, 10, 2);
        chassis.addOrReplaceChild("cushion_fl", cushion, at(3, 0, -3));
        chassis.addOrReplaceChild("cushion_fr", cushion, at(-11, 0, -3));
        chassis.addOrReplaceChild("cushion_bl", cushion, at(3, 0, 9));
        chassis.addOrReplaceChild("cushion_br", cushion, at(-11, 0, 9));
        chassis.addOrReplaceChild("back_fl", back, at(3, 0, 3));
        chassis.addOrReplaceChild("back_fr", back, at(-11, 0, 3));
        chassis.addOrReplaceChild("back_bl", back, at(3, 0, 15));
        chassis.addOrReplaceChild("back_br", back, at(-11, 0, 15));

        // Luces traseras
        CubeListBuilder taillight = cube(222, 144).addBox(0, -17, 26, 3, 3, 1);
        chassis.addOrReplaceChild("taillight_l", taillight, at(8, 0, 0));
        chassis.addOrReplaceChild("taillight_r", taillight, at(-11, 0, 0));

        // Ruedas redondeadas: banda de rodadura en cruz + la misma cruz rotada 45° (perfil de 16 lados) y una
        // tapa plana a cada lado con la cara blanca; el pivote va en el eje para rodar y girar.
        // Anchos escalonados en X para evitar caras laterales coplanares (z-fighting).
        CubeListBuilder tread0 = cube(194, 28)
                .addBox(-1.95F, -6, -2.5F, 3.9F, 12, 5)
                .addBox(-1.90F, -2.5F, -6, 3.8F, 5, 12);
        CubeListBuilder tread45 = cube(194, 28)
                .addBox(-1.85F, -6, -2.5F, 3.7F, 12, 5)
                .addBox(-1.80F, -2.5F, -6, 3.6F, 5, 12);
        CubeListBuilder hubs = cube(0, 184)
                .addBox(2.05F, -6, -6, 0, 12, 12)
                .addBox(-2.05F, -6, -6, 0, 12, 12);
        float[][] wheelPos = {{14, -14}, {-14, -14}, {14, 14}, {-14, 14}};
        for (int i = 0; i < WHEELS.length; i++) {
            PartDefinition wheel = root.addOrReplaceChild(WHEELS[i], tread0, at(wheelPos[i][0], 18, wheelPos[i][1]));
            wheel.addOrReplaceChild("tread_45", tread45, PartPose.rotation((float) Math.PI / 4, 0, 0));
            wheel.addOrReplaceChild("hubs", hubs, PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 256, 256);
    }

    /**
     * Una de las dos capas. La carrocería cabecea, se inclina y rebota con la suspensión; en la capa de
     * detalles además las ruedas ruedan, las delanteras giran y la bandera ondea (más rápido, más fuerte).
     */
    public static final class Part extends EntityModel<CarRenderState> {
        private static final float FLAG_SPEED = 0.35F;   // rad por tick
        private static final float FLAG_SWING_IDLE = 0.12F;
        private static final float FLAG_SWING_FAST = 0.3F;

        private final ModelPart chassis;
        private final ModelPart flag;
        private final ModelPart[] wheels = new ModelPart[4];

        public Part(ModelPart root) {
            super(root);
            this.chassis = root.getChild(CHASSIS);
            this.flag = chassis.hasChild(FLAG) ? chassis.getChild(FLAG) : null;
            for (int i = 0; i < wheels.length; i++) {
                wheels[i] = root.hasChild(WHEELS[i]) ? root.getChild(WHEELS[i]) : null;
            }
        }

        @Override
        public void setupAnim(CarRenderState state) {
            super.setupAnim(state);
            chassis.xRot = state.bodyPitch;
            chassis.zRot = state.bodyRoll;
            chassis.y -= state.bodyHeave * 16.0F; // bloques → píxeles; el eje Y del modelo apunta hacia abajo

            if (flag != null) {
                float swing = Mth.lerp(state.speedRatio, FLAG_SWING_IDLE, FLAG_SWING_FAST);
                flag.yRot = Mth.sin(state.ageInTicks * FLAG_SPEED) * swing;
            }

            for (int i = 0; i < wheels.length; i++) {
                if (wheels[i] == null) continue;
                wheels[i].xRot = state.wheelRoll;
                wheels[i].yRot = i < 2 ? state.steer : 0.0F; // solo las delanteras giran
            }
        }
    }

    private CarModels() {}
}
