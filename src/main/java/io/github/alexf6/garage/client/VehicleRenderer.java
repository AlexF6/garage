package io.github.alexf6.garage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.alexf6.garage.MotorVehicleEntity;
import io.github.alexf6.garage.physics.VehicleAnimation;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Renderer de cualquier vehículo del mod. Cada uno aporta su aspecto ({@link Look}): textura, capas del modelo
 * y sombra. La carrocería (capa BODY) lleva el color del vehículo; el resto (capa TRIM: chasis, vidrios, ruedas,
 * luces) se dibuja sin teñir.
 */
public class VehicleRenderer extends EntityRenderer<MotorVehicleEntity, VehicleRenderer.VehicleRenderState> {

    /**
     * @param body       capa de la carrocería (se tiñe)
     * @param trim       capa del resto (sin teñir)
     * @param partFactory crea el modelo animado de una capa a partir de sus partes ya horneadas
     */
    public record Look(Identifier texture, float shadowRadius, ModelLayerLocation body, ModelLayerLocation trim,
                       Function<ModelPart, EntityModel<VehicleRenderState>> partFactory) {}

    private final Identifier texture;
    private final EntityModel<VehicleRenderState> body;
    private final EntityModel<VehicleRenderState> trim;

    public VehicleRenderer(EntityRendererProvider.Context context, Look look) {
        super(context);
        this.shadowRadius = look.shadowRadius();
        this.texture = look.texture();
        this.body = look.partFactory().apply(context.bakeLayer(look.body()));
        this.trim = look.partFactory().apply(context.bakeLayer(look.trim()));
    }

    @Override
    public VehicleRenderState createRenderState() {
        return new VehicleRenderState();
    }

    @Override
    public void extractRenderState(MotorVehicleEntity vehicle, VehicleRenderState state, float partialTick) {
        super.extractRenderState(vehicle, state, partialTick);
        state.yRot = vehicle.getYRot(partialTick);
        state.hurtDir = vehicle.getHurtDir();
        state.hurtTime = vehicle.getHurtTime() - partialTick;
        state.damage = Math.max(vehicle.getDamage() - partialTick, 0.0F);
        state.tint = 0xFF000000 | vehicle.getColor();
        VehicleAnimation animation = vehicle.animation();
        state.wheelRoll = animation.wheelRoll(partialTick);
        state.steer = animation.steer(partialTick);
        state.lean = animation.lean(partialTick);
        state.bodyPitch = animation.suspension().pitch(partialTick);
        state.bodyRoll = animation.suspension().roll(partialTick);
        state.bodyHeave = animation.suspension().heave(partialTick);
        state.isEngineRunning = vehicle.isEngineRunning();
        state.speedRatio = (float) vehicle.speedRatio();
    }

    @Override
    public void submit(VehicleRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        if (state.hurtTime > 0.0F) {
            poseStack.rotateDegrees(Axis.ZP, Mth.sin(state.hurtTime) * state.damage / 10.0F * state.hurtDir);
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        collector.submitModel(body, state, poseStack, body.renderType(texture), state.lightCoords,
                OverlayTexture.NO_OVERLAY, state.tint, null, state.outlineColor);
        collector.submitModel(trim, state, poseStack, trim.renderType(texture), state.lightCoords,
                OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    public static class VehicleRenderState extends EntityRenderState {
        public float yRot;
        public int hurtDir;
        public float hurtTime;
        public float damage;
        public int tint = -1;
        public float wheelRoll;
        public float steer;
        public float lean;
        public float bodyPitch;
        public float bodyRoll;
        public float bodyHeave;
        public boolean isEngineRunning;
        public float speedRatio;
    }
}
