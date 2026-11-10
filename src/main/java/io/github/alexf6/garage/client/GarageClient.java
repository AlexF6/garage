package io.github.alexf6.garage.client;

import io.github.alexf6.garage.CarEntity;
import io.github.alexf6.garage.GarageMod;
import io.github.alexf6.garage.HornPayload;
import io.github.alexf6.garage.ModEntities;
import io.github.alexf6.garage.physics.CarInput;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Input;

public class GarageClient implements ClientModInitializer {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, "main"));
    private static final KeyMapping HORN =
            KeyMappingHelper.registerKeyMapping(new KeyMapping("key.garage.horn", InputConstants.KEY_H, CATEGORY));

    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(CarModels.BODY, CarModels::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(CarModels.TRIM, CarModels::createTrimLayer);
        ModelLayerRegistry.registerModelLayer(MotorcycleModels.BODY, MotorcycleModels::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(MotorcycleModels.TRIM, MotorcycleModels::createTrimLayer);
        ModelLayerRegistry.registerModelLayer(MotocarroModels.BODY, MotocarroModels::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(MotocarroModels.TRIM, MotocarroModels::createTrimLayer);

        EntityRendererRegistry.register(ModEntities.CAR, CarRenderer::new);
        EntityRendererRegistry.register(ModEntities.MOTORCYCLE, MotorcycleRenderer::new);
        EntityRendererRegistry.register(ModEntities.MOTOCARRO, MotocarroRenderer::new);

        // Un sonido de motor por vehículo cargado; se detiene solo cuando el vehículo desaparece
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof CarEntity car) {
                Minecraft.getInstance().getSoundManager().play(new CarEngineSound(car));
            }
        });

        // Pasa las teclas del conductor al vehículo antes de cada tick
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player == null) return;

            if (player.getVehicle() instanceof CarEntity car && car.getControllingPassenger() == player) {
                Input keys = player.input.keyPresses;
                car.setInput(CarInput.of(keys.left(), keys.right(), keys.forward(), keys.backward(), keys.jump()));
                while (HORN.consumeClick()) {
                    ClientPlayNetworking.send(new HornPayload());
                }
            } else {
                while (HORN.consumeClick()) {
                    // descartar pulsaciones fuera del vehículo
                }
            }
        });
    }
}
