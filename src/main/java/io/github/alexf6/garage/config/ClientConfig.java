package io.github.alexf6.garage.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
 * <p>Migración: {@code engineVolume} vivía en {@code garage.json}. Si todavía no existe este archivo, se parte del
 * valor que había allí.
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

    /**
     * @param legacyEngineVolume el {@code engineVolume} que tenía el {@code garage.json} anterior, o {@code null}
     */
    public static void load(Path file, Float legacyEngineVolume) {
        ClientConfig parsed = legacyEngineVolume != null ? new ClientConfig(legacyEngineVolume) : DEFAULT;
        try {
            if (Files.exists(file)) {
                try {
                    JsonObject user = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                    if (user.has("engineVolume") && !user.get("engineVolume").isJsonNull()) {
                        parsed = new ClientConfig(user.get("engineVolume").getAsFloat());
                    } else {
                        parsed = DEFAULT;
                    }
                } catch (JsonParseException | IllegalStateException | NumberFormatException
                         | CharacterCodingException e) {
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
        Path backup = ConfigFiles.backUp(file);
        if (backup == null) {
            GarageMod.LOGGER.warn("{} no es válido ({}) y no se pudo guardar una copia; se usan los valores por defecto",
                    file, cause.getMessage());
            return;
        }
        GarageMod.LOGGER.warn("{} no es válido ({}); se guardó como {} y se creó uno nuevo con los valores por defecto",
                file, cause.getMessage(), backup);
        write(file, DEFAULT);
    }

    private static void write(Path file, ClientConfig config) {
        try {
            ConfigFiles.writeAtomic(file, GSON.toJson(config));
        } catch (IOException e) {
            GarageMod.LOGGER.warn("No se pudo escribir {}: {}", file, e.toString());
        }
    }
}
