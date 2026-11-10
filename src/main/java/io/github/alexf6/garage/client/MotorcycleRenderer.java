package io.github.alexf6.garage.client;

import io.github.alexf6.garage.GarageMod;
import io.github.alexf6.garage.MotorcycleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class MotorcycleRenderer extends EntityRenderer<MotorcycleEntity, MotorcycleRenderer.MotorcycleRenderState> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, "textures/entity/motorcycle.png");

    private final MotorcycleModels.Part body;
    private final MotorcycleModels.Part trim;

    public MotorcycleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.body = new MotorcycleModels.Part(context.bakeLayer(MotorcycleModels.BODY));
        this.trim = new MotorcycleModels.Part(context.bakeLayer(MotorcycleModels.TRIM));
    }

    @Override
    public MotorcycleRenderState createRenderState() {
        return new MotorcycleRenderState();
    }

    @Override
    public void extractRenderState(MotorcycleEntity bike, MotorcycleRenderState state, float partialTick) {
        super.extractRenderState(bike, state, partialTick);
        state.yRot = bike.getYRot(partialTick);
        state.hurtDir = bike.getHurtDir();
        state.hurtTime = bike.getHurtTime() - partialTick;
        state.damage = Math.max(bike.getDamage() - partialTick, 0.0F);
        state.tint = 0xFF000000 | bike.getColor();
        state.wheelRoll = bike.getWheelRoll(partialTick);
        state.steer = bike.getSteer(partialTick);
        state.lean = bike.getLean(partialTick);
        state.bodyPitch = bike.getSuspension().pitch(partialTick);
        state.bodyHeave = bike.getSuspension().heave(partialTick);
        state.isEngineRunning = bike.isEngineRunning();
        state.speedRatio = (float) Math.min(Math.abs(bike.getForwardSpeed()) / bike.tuning().maxSpeed(), 1.0);
    }

    @Override
    public void submit(MotorcycleRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        if (state.hurtTime > 0.0F) {
            poseStack.rotateDegrees(Axis.ZP, Mth.sin(state.hurtTime) * state.damage / 10.0F * state.hurtDir);
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        // Capa BODY: carenado teñible con el color de la moto (azul por defecto)
        collector.submitModel(body, state, poseStack, body.renderType(TEXTURE), state.lightCoords,
                OverlayTexture.NO_OVERLAY, state.tint, null, state.outlineColor);
        // Capa TRIM: chasis, motor, asiento y ruedas sin teñir
        collector.submitModel(trim, state, poseStack, trim.renderType(TEXTURE), state.lightCoords,
                OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    public static class MotorcycleRenderState extends EntityRenderState {
        public float yRot;
        public int hurtDir;
        public float hurtTime;
        public float damage;
        public int tint = -1;
        public float wheelRoll;
        public float steer;
        public float lean;
        public float bodyPitch;
        public float bodyHeave;
        public boolean isEngineRunning;
        public float speedRatio;
    }
}
