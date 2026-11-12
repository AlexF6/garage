package io.github.alexf6.garage.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.alexf6.garage.GarageMod;
import io.github.alexf6.garage.ModVehicles;
import io.github.alexf6.garage.MotorVehicleEntity;
import io.github.alexf6.garage.VehicleSpec;
import io.github.alexf6.garage.VehicleSpecs;
import io.github.alexf6.garage.config.ClientConfig;
import io.github.alexf6.garage.network.HornPayload;
import io.github.alexf6.garage.network.SyncedTunings;
import io.github.alexf6.garage.network.TuningSyncPayload;
import io.github.alexf6.garage.physics.VehicleInput;
import io.github.alexf6.garage.physics.VehicleTuning;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Input;

public class GarageClient implements ClientModInitializer {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, "main"));
    private static final KeyMapping HORN =
            KeyMappingHelper.registerKeyMapping(new KeyMapping("key.garage.horn", InputConstants.KEY_H, CATEGORY));

    /** Consume las pulsaciones pendientes de una tecla sin hacer nada con ellas. */
    private static void discardClicks(KeyMapping key) {
        while (key.consumeClick()) {
            // solo vaciar la cola
        }
    }

    @Override
    public void onInitializeClient() {
        ClientConfig.load(FabricLoader.getInstance().getConfigDir().resolve(ClientConfig.FILE_NAME));
        for (VehicleSpec spec : VehicleSpecs.ALL) {
            ClientVehicles.Model model = ClientVehicles.lookOf(spec);
            ModelLayerRegistry.registerModelLayer(model.look().body(), model.bodyLayer()::get);
            ModelLayerRegistry.registerModelLayer(model.look().trim(), model.trimLayer()::get);
            EntityRenderers.register(ModVehicles.entityType(spec),
                    context -> new VehicleRenderer(context, model.look()));
        }

        // El manejo de los vehículos lo manda el servidor al entrar y se descarta al salir
        ClientPlayNetworking.registerGlobalReceiver(TuningSyncPayload.TYPE, (payload, context) -> {
            Map<String, VehicleTuning> valid = new HashMap<>();
            payload.tunings().forEach((id, tuning) -> {
                String invalid = tuning.firstInvalid();
                if (invalid == null) {
                    valid.put(id, tuning);
                } else {
                    GarageMod.LOGGER.warn("El servidor mandó un tuning inválido para {} ({}); se usa el local", id, invalid);
                }
            });
            SyncedTunings.set(valid);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SyncedTunings.clear());

        // Un sonido de motor por vehículo cargado; se detiene solo cuando el vehículo desaparece
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof MotorVehicleEntity vehicle) {
                Minecraft.getInstance().getSoundManager().play(new EngineSound(vehicle));
            }
        });

        // Pasa las teclas del conductor al vehículo antes de cada tick
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player == null) return;

            if (player.getVehicle() instanceof MotorVehicleEntity vehicle && vehicle.getControllingPassenger() == player) {
                Input keys = player.input.keyPresses;
                vehicle.setInput(VehicleInput.of(keys.left(), keys.right(), keys.forward(), keys.backward(), keys.jump()));
                while (HORN.consumeClick()) {
                    ClientPlayNetworking.send(new HornPayload());
                }
            } else {
                discardClicks(HORN); // fuera del vehículo la bocina no suena: se descartan las pulsaciones
            }
        });
    }
}
