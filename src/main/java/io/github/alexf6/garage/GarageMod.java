package io.github.alexf6.garage;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GarageMod implements ModInitializer {
    public static final String MOD_ID = "garage";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        GarageConfig.load(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
        ModVehicles.register();

        PayloadTypeRegistry.serverboundPlay().register(HornPayload.TYPE, HornPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TuningSyncPayload.TYPE, TuningSyncPayload.CODEC);
        // El manejo del Jeep lo dicta el servidor: cada cliente lo recibe al entrar
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.getPlayer(), TuningSyncPayload.TYPE)) {
                ServerPlayNetworking.send(handler.getPlayer(), new TuningSyncPayload(GarageConfig.get().tuning()));
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(HornPayload.TYPE, (payload, context) -> {
            if (context.player().getVehicle() instanceof MotorVehicleEntity vehicle) {
                vehicle.honk(context.player());
            }
        });

        LOGGER.info("GarageMod inicializado correctamente.");
    }
}
