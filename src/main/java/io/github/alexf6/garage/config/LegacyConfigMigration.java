package io.github.alexf6.garage.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.alexf6.garage.GarageMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Pasos que mueven ajustes de una versión anterior del mod a donde viven ahora. Se ejecutan al cargar la config
 * antigua, antes de reescribirla, para no perder lo que el jugador tenía. Cada paso funciona sea cual sea la config
 * que arranque primero.
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
        Float legacy = legacyEngineVolume(user);
        if (legacy == null) return true;

        Path target = garageJson.resolveSibling(ClientConfig.FILE_NAME);
        if (Files.exists(target)) return true;
        if (ClientConfig.create(target, legacy)) {
            GarageMod.LOGGER.info("engineVolume pasó de {} a {}", garageJson.getFileName(), ClientConfig.FILE_NAME);
            return true;
        }
        GarageMod.LOGGER.warn("No se pudo crear {}; {} se deja como está para no perder engineVolume",
                target, garageJson.getFileName());
        return false;
    }

    /**
     * El {@code engineVolume} que {@code garage.json} todavía guarda, para que el cliente lo use al crear su propia
     * config si arranca antes que la del servidor. Nunca falla: un archivo ausente, ilegible o sin la clave da
     * {@code null}. Solo lee; quien reescribe {@code garage.json} es {@link GarageConfig#load}.
     */
    static Float readLegacyEngineVolume(Path garageJson) {
        try {
            return legacyEngineVolume(JsonParser.parseString(Files.readString(garageJson)).getAsJsonObject());
        } catch (IOException | JsonParseException | IllegalStateException e) {
            return null;
        }
    }

    /** El volumen de la clave antigua, o {@code null} si falta o no es un número. */
    private static Float legacyEngineVolume(JsonObject user) {
        JsonElement legacy = user.get("engineVolume");
        if (legacy == null || !legacy.isJsonPrimitive() || !legacy.getAsJsonPrimitive().isNumber()) return null;
        return legacy.getAsFloat();
    }

    private LegacyConfigMigration() {}
}
