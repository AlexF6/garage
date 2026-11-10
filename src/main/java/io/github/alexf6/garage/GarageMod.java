package io.github.alexf6.garage;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GarageMod implements ModInitializer {
    public static final String MOD_ID = "garage";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CarConfig.load(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
        ModEntities.init();
        ModItems.init();

        PayloadTypeRegistry.serverboundPlay().register(HornPayload.TYPE, HornPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HornPayload.TYPE, (payload, context) -> {
            if (context.player().getVehicle() instanceof CarEntity car) {
                car.honk(context.player());
            }
        });

        LOGGER.info("GarageMod inicializado correctamente.");
    }
}
