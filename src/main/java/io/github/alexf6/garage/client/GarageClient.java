package io.github.alexf6.garage.client;

import io.github.alexf6.garage.MotorVehicleEntity;
import io.github.alexf6.garage.GarageMod;
import io.github.alexf6.garage.HornPayload;
import io.github.alexf6.garage.ModVehicles;
import io.github.alexf6.garage.VehicleSpec;
import io.github.alexf6.garage.VehicleSpecs;
import io.github.alexf6.garage.physics.VehicleInput;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.entity.EntityRenderers;
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
        for (VehicleSpec spec : VehicleSpecs.ALL) {
            ClientVehicles.Model model = ClientVehicles.lookOf(spec);
            ModelLayerRegistry.registerModelLayer(model.look().body(), model.bodyLayer()::get);
            ModelLayerRegistry.registerModelLayer(model.look().trim(), model.trimLayer()::get);
            EntityRenderers.register(ModVehicles.entityType(spec),
                    context -> new VehicleRenderer(context, model.look()));
        }

        // Un sonido de motor por vehículo cargado; se detiene solo cuando el vehículo desaparece
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof MotorVehicleEntity car) {
                Minecraft.getInstance().getSoundManager().play(new EngineSound(car));
            }
        });

        // Pasa las teclas del conductor al vehículo antes de cada tick
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player == null) return;

            if (player.getVehicle() instanceof MotorVehicleEntity car && car.getControllingPassenger() == player) {
                Input keys = player.input.keyPresses;
                car.setInput(VehicleInput.of(keys.left(), keys.right(), keys.forward(), keys.backward(), keys.jump()));
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
