package io.github.alexf6.garage.client;

import io.github.alexf6.garage.CarEntity;
import io.github.alexf6.garage.GarageMod;
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

public class CarRenderer extends EntityRenderer<CarEntity, CarRenderer.CarRenderState> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, "textures/entity/car.png");

    private final CarModels.Part body;
    private final CarModels.Part trim;

    public CarRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.9F;
        this.body = new CarModels.Part(context.bakeLayer(CarModels.BODY));
        this.trim = new CarModels.Part(context.bakeLayer(CarModels.TRIM));
    }

    @Override
    public CarRenderState createRenderState() {
        return new CarRenderState();
    }

    @Override
    public void extractRenderState(CarEntity car, CarRenderState state, float partialTick) {
        super.extractRenderState(car, state, partialTick);
        state.yRot = car.getYRot(partialTick);
        state.hurtDir = car.getHurtDir();
        state.hurtTime = car.getHurtTime() - partialTick;
        state.damage = Math.max(car.getDamage() - partialTick, 0.0F);
        state.tint = 0xFF000000 | car.getColor();
        state.wheelRoll = car.getWheelRoll(partialTick);
        state.steer = car.getSteer(partialTick);
        state.bodyPitch = car.getSuspension().pitch(partialTick);
        state.bodyRoll = car.getSuspension().roll(partialTick);
        state.bodyHeave = car.getSuspension().heave(partialTick);
        state.speedRatio = (float) Math.min(Math.abs(car.getForwardSpeed()) / car.tuning().maxSpeed(), 1.0);
    }

    @Override
    public void submit(CarRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        if (state.hurtTime > 0.0F) {
            poseStack.rotateDegrees(Axis.ZP, Mth.sin(state.hurtTime) * state.damage / 10.0F * state.hurtDir);
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        // La carrocería lleva el color del carro; vidrios, ruedas y luces quedan sin teñir
        collector.submitModel(body, state, poseStack, body.renderType(TEXTURE), state.lightCoords,
                OverlayTexture.NO_OVERLAY, state.tint, null, state.outlineColor);
        collector.submitModel(trim, state, poseStack, trim.renderType(TEXTURE), state.lightCoords,
                OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    public static class CarRenderState extends EntityRenderState {
        public float yRot;
        public int hurtDir;
        public float hurtTime;
        public float damage;
        public int tint = -1;
        public float wheelRoll;
        public float steer;
        public float bodyPitch;
        public float bodyRoll;
        public float bodyHeave;
        public float speedRatio;
    }
}
