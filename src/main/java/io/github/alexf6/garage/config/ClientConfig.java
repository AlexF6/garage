package io.github.alexf6.garage.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.alexf6.garage.GarageMod;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Ajustes que solo importan al cliente, en {@code config/garage-client.json}: no dependen del servidor ni se
 * sincronizan. Se carga una sola vez al arrancar el cliente; el servidor dedicado nunca la lee.
 *
 * <p>Mismas reglas que {@link GarageConfig}: lo que falta toma el valor por defecto, lo que se sale de rango se
 * corrige y se avisa, y un archivo corrupto se guarda como {@code .bak} y se regenera.
 *
 * <p>Migración: {@code engineVolume} vivía en {@code garage.json}. Al cargar esa config, {@link LegacyConfigMigration}
 * crea este archivo con el valor que había allí si todavía no existe; así no depende del orden en que arranquen el
 * servidor y el cliente. El cliente es el único que lee este archivo, aunque un servidor dedicado que aún tenga la
 * clave antigua lo crea una vez durante esa migración.
 *
 * @param engineVolume volumen del motor, de 0 (apagado) a 1
 */
public record ClientConfig(float engineVolume) {

    public static final ClientConfig DEFAULT = new ClientConfig(0.6F);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Se escribe una sola vez al arrancar el cliente (load); el resto del tiempo solo se lee
    private static volatile ClientConfig current = DEFAULT;

    public static ClientConfig get() {
        return current;
    }

    /** Nombre del archivo dentro de {@code config/}. */
    public static final String FILE_NAME = GarageMod.MOD_ID + "-client.json";

    public static void load(Path file) {
        ClientConfig parsed = DEFAULT;
        try {
            if (Files.exists(file)) {
                try {
                    parsed = parse(JsonParser.parseString(Files.readString(file)).getAsJsonObject());
                } catch (JsonParseException | IllegalStateException | CharacterCodingException e) {
                    backUpAndRegenerate(file, e);
                    return;
                }
            }
        } catch (IOException e) {
            GarageMod.LOGGER.warn("No se pudo leer {}; se usan los valores por defecto: {}", file, e.toString());
            current = DEFAULT;
            return;
        }

        List<String> fixes = new ArrayList<>();
        current = sanitize(parsed, fixes);
        fixes.forEach(fix -> GarageMod.LOGGER.warn("garage-client.json: {}", fix));
        write(file, current);
    }

    /**
     * Crea {@code file} con {@code volume} (acotado al rango y avisando si hizo falta). No comprueba si ya existe:
     * de eso se ocupa {@link LegacyConfigMigration}.
     *
     * @return {@code true} si quedó escrito
     */
    static boolean create(Path file, float volume) {
        List<String> fixes = new ArrayList<>();
        ClientConfig config = sanitize(new ClientConfig(volume), fixes);
        fixes.forEach(fix -> GarageMod.LOGGER.warn("{} (migrado de garage.json): {}", FILE_NAME, fix));
        return write(file, config);
    }

    /** Lee el volumen; falta o {@code null} = por defecto; cualquier otro tipo que no sea un número es un archivo dañado. */
    private static ClientConfig parse(JsonObject user) {
        JsonElement volume = user.get("engineVolume");
        if (volume == null || volume.isJsonNull()) return DEFAULT;
        if (!volume.isJsonPrimitive() || !volume.getAsJsonPrimitive().isNumber()) {
            throw new IllegalStateException("engineVolume debe ser un número, no " + volume);
        }
        return new ClientConfig(volume.getAsFloat());
    }

    /** Corrige al rango válido y anota cada corrección en {@code fixes}. */
    static ClientConfig sanitize(ClientConfig config, List<String> fixes) {
        float volume = config.engineVolume();
        if (Float.isNaN(volume)) {
            fixes.add("engineVolume no es un número; se usa " + DEFAULT.engineVolume());
            return DEFAULT;
        }
        float clamped = Math.clamp(volume, 0.0F, 1.0F);
        if (clamped != volume) {
            fixes.add("engineVolume = " + volume + " está fuera de [0.0, 1.0]; se usa " + clamped);
        }
        return new ClientConfig(clamped);
    }

    private static void backUpAndRegenerate(Path file, Exception cause) {
        current = DEFAULT;
        if (ConfigFiles.backUpCorrupt(file, cause)) write(file, DEFAULT);
    }

    private static boolean write(Path file, ClientConfig config) {
        return ConfigFiles.write(file, GSON.toJson(config));
    }
}
