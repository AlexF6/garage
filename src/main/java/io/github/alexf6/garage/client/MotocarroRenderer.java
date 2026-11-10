package io.github.alexf6.garage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.alexf6.garage.GarageMod;
import io.github.alexf6.garage.MotocarroEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class MotocarroRenderer extends EntityRenderer<MotocarroEntity, MotocarroRenderer.MotocarroRenderState> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, "textures/entity/motocarro.png");

    private final MotocarroModels.Part body;
    private final MotocarroModels.Part trim;

    public MotocarroRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.75F;
        this.body = new MotocarroModels.Part(context.bakeLayer(MotocarroModels.BODY));
        this.trim = new MotocarroModels.Part(context.bakeLayer(MotocarroModels.TRIM));
    }

    @Override
    public MotocarroRenderState createRenderState() {
        return new MotocarroRenderState();
    }

    @Override
    public void extractRenderState(MotocarroEntity motocarro, MotocarroRenderState state, float partialTick) {
        super.extractRenderState(motocarro, state, partialTick);
        state.yRot = motocarro.getYRot(partialTick);
        state.hurtDir = motocarro.getHurtDir();
        state.hurtTime = motocarro.getHurtTime() - partialTick;
        state.damage = Math.max(motocarro.getDamage() - partialTick, 0.0F);
        state.tint = 0xFF000000 | motocarro.getColor();
        state.wheelRoll = motocarro.getWheelRoll(partialTick);
        state.steer = motocarro.getSteer(partialTick);
        state.bodyPitch = motocarro.getSuspension().pitch(partialTick);
        state.bodyHeave = motocarro.getSuspension().heave(partialTick);
        state.isEngineRunning = motocarro.isEngineRunning();
        state.speedRatio = (float) Math.min(Math.abs(motocarro.getForwardSpeed()) / motocarro.tuning().maxSpeed(), 1.0);
    }

    @Override
    public void submit(MotocarroRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        if (state.hurtTime > 0.0F) {
            poseStack.rotateDegrees(Axis.ZP, Mth.sin(state.hurtTime) * state.damage / 10.0F * state.hurtDir);
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        // Capa de carrocería teñida con el color del vehículo
        collector.submitModel(body, state, poseStack, body.renderType(TEXTURE), state.lightCoords,
                OverlayTexture.NO_OVERLAY, state.tint, null, state.outlineColor);

        // Capa de chasis, lona, vidrios, interior y ruedas (sin teñir)
        collector.submitModel(trim, state, poseStack, trim.renderType(TEXTURE), state.lightCoords,
                OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class MotocarroRenderState extends EntityRenderState {
        public float yRot;
        public int hurtDir;
        public float hurtTime;
        public float damage;
        public int tint = -1;
        public float wheelRoll;
        public float steer;
        public float bodyPitch;
        public float bodyHeave;
        public boolean isEngineRunning;
        public float speedRatio;
    }
}
