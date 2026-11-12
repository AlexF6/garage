package io.github.alexf6.garage.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.alexf6.garage.GarageMod;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Pasos que mueven ajustes de una versión anterior del mod a donde viven ahora. Se ejecutan al cargar la config
 * antigua, antes de reescribirla, para no perder lo que el jugador tenía.
 */
final class LegacyConfigMigration {

    /**
     * {@code engineVolume} era una clave de {@code garage.json} y ahora es de {@link ClientConfig}: se copia a
     * {@code garage-client.json}, junto a {@code garageJson}, si ese archivo todavía no existe. No hay nada que hacer
     * si la clave falta o no es un número; si el archivo nuevo ya existe, manda el suyo.
     *
     * <p>Un {@code garage.json} que ni siquiera se puede leer como JSON no llega aquí: su volumen queda solo en el
     * {@code .bak}.
     *
     * @return {@code false} si había que migrar y no se pudo: entonces no se debe reescribir {@code garage.json},
     *         para que la clave siga ahí en el próximo arranque
     */
    static boolean migrateEngineVolume(Path garageJson, JsonObject user) {
        JsonElement legacy = user.get("engineVolume");
        if (legacy == null || !legacy.isJsonPrimitive() || !legacy.getAsJsonPrimitive().isNumber()) return true;

        Path target = garageJson.resolveSibling(ClientConfig.FILE_NAME);
        if (Files.exists(target)) return true;
        if (ClientConfig.create(target, legacy.getAsFloat())) {
            GarageMod.LOGGER.info("engineVolume pasó de {} a {}", garageJson.getFileName(), ClientConfig.FILE_NAME);
            return true;
        }
        GarageMod.LOGGER.warn("No se pudo crear {}; {} se deja como está para no perder engineVolume",
                target, garageJson.getFileName());
        return false;
    }

    private LegacyConfigMigration() {}
}
