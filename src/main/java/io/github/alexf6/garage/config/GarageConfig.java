package io.github.alexf6.garage.config;

import io.github.alexf6.garage.GarageMod;
import io.github.alexf6.garage.network.SyncedTunings;
import io.github.alexf6.garage.network.TuningSyncPayload;

import io.github.alexf6.garage.physics.RunOverRules;
import io.github.alexf6.garage.physics.VehicleTuning;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ajustes del mod en {@code config/garage.json}. Las claves que falten se completan con los valores por
 * defecto y el archivo se reescribe, así las opciones nuevas aparecen solas al actualizar el mod.
 * Se lee al arrancar el juego (o el servidor): para aplicar cambios hay que reiniciar.
 *
 * <p>Los valores fuera de rango se corrigen al límite y se avisan en el log. Un archivo ilegible como JSON se
 * guarda como {@code garage.json.bak} y se regenera con los valores por defecto.
 *
 * <p>En multijugador el servidor manda al conectar el manejo de cada vehículo ({@link TuningSyncPayload}) y el
 * cliente lo usa en lugar del suyo ({@link SyncedTunings}); las motos usan perfiles fijos del mod.
 *
 * @param maxFuel      ticks de acelerador con el tanque lleno, de 1 a {@value #MAX_FUEL_LIMIT}
 * @param fuelPerItem  ticks que suma cada unidad de combustible, de 0 a {@code maxFuel}
 */
public record GarageConfig(int maxFuel, int fuelPerItem, VehicleTuning tuning, RunOverRules runOver) {

    /** Tope de {@code maxFuel} (ticks, unas 14 h de acelerador): mantiene los cálculos de nivel dentro de {@code int}. */
    public static final int MAX_FUEL_LIMIT = 1_000_000;
    /** Tope de las velocidades de atropello (bloques/tick). */
    public static final double RUN_OVER_SPEED_LIMIT = 10.0;
    /** Tope del daño por bloque/tick sobre el umbral de daño. */
    public static final double DAMAGE_PER_SPEED_LIMIT = 1000.0;
    /** Tope de la fuerza del empuje. */
    public static final double KNOCKBACK_LIMIT = 10.0;

    public static final GarageConfig DEFAULT = new GarageConfig(12000, 1600, VehicleTuning.WILLYS,
            new RunOverRules(true, 0.1, 0.25, 25.0, 1.2, false));

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Se escribe una sola vez al arrancar (load); el resto del tiempo solo se lee. Lo que el cliente recibe del
    // servidor vive aparte, en SyncedTunings, para que el cliente nunca pise la config del servidor integrado.
    private static volatile GarageConfig current = DEFAULT;

    public static GarageConfig get() {
        return current;
    }

    /** Manejo del Jeep según esta config (la del servidor, o la local si se juega sin conexión). */
    public static VehicleTuning carTuning() {
        return current.tuning();
    }

    public static void load(Path file) {
        JsonObject merged = GSON.toJsonTree(DEFAULT).getAsJsonObject();
        GarageConfig parsed;
        try {
            if (Files.exists(file)) {
                String text;
                try {
                    text = Files.readString(file);
                } catch (CharacterCodingException e) {
                    // Bytes que no son UTF-8: el archivo está dañado, no es un problema de lectura
                    backUpAndRegenerate(file, e);
                    return;
                }
                try {
                    JsonObject user = JsonParser.parseString(text).getAsJsonObject();
                    migrateLegacyEngineVolume(file, user);
                    mergeInto(merged, user);
                    saturateInt(merged, "maxFuel");
                    saturateInt(merged, "fuelPerItem");
                    parsed = GSON.fromJson(merged, GarageConfig.class);
                } catch (JsonParseException | IllegalStateException e) {
                    backUpAndRegenerate(file, e);
                    return;
                }
            } else {
                parsed = DEFAULT;
            }
        } catch (IOException e) {
            // No se pudo leer (permisos, disco): se juega con los valores por defecto y no se toca el archivo
            GarageMod.LOGGER.warn("No se pudo leer {}; se usan los valores por defecto: {}", file, e.toString());
            current = DEFAULT;
            return;
        }

        List<String> fixes = new ArrayList<>();
        current = sanitize(parsed, fixes);
        fixes.forEach(fix -> GarageMod.LOGGER.warn("garage.json: {}", fix));
        write(file, current);
    }

    /** Archivo corrupto: se conserva como {@code .bak} para que el jugador lo recupere y se crea uno nuevo. */
    private static void backUpAndRegenerate(Path file, Exception cause) {
        current = DEFAULT;
        Path backup = ConfigFiles.backUp(file);
        if (backup == null) {
            // Sin copia de seguridad no se sobrescribe el original
            GarageMod.LOGGER.warn("{} no es válido ({}) y no se pudo guardar una copia; se usan los valores por defecto",
                    file, cause.getMessage());
            return;
        }
        GarageMod.LOGGER.warn("{} no es válido ({}); se guardó como {} y se creó uno nuevo con los valores por defecto",
                file, cause.getMessage(), backup);
        write(file, DEFAULT);
    }

    private static void write(Path file, GarageConfig config) {
        try {
            ConfigFiles.writeAtomic(file, GSON.toJson(config));
        } catch (IOException e) {
            GarageMod.LOGGER.warn("No se pudo escribir {}: {}", file, e.toString());
        }
    }

    /**
     * Corrige al rango válido todo lo que se salga de él y anota cada corrección en {@code fixes}.
     * Las secciones {@code tuning} y {@code runOver} ausentes o nulas se sustituyen por las de defecto.
     */
    static GarageConfig sanitize(GarageConfig config, List<String> fixes) {
        VehicleTuning tuning = config.tuning();
        String invalid = tuning == null ? "falta la sección" : tuning.firstInvalid();
        if (invalid != null) {
            fixes.add("tuning inválido (" + invalid + "); se usa el manejo por defecto");
            tuning = DEFAULT.tuning();
        }
        RunOverRules r = config.runOver();
        if (r == null) {
            fixes.add("falta la sección runOver; se usan los valores por defecto");
            r = DEFAULT.runOver();
        }
        double pushSpeed = clamp("runOver.pushSpeed", r.pushSpeed(), 0.0, RUN_OVER_SPEED_LIMIT,
                DEFAULT.runOver().pushSpeed(), fixes);
        double damageSpeed = clamp("runOver.damageSpeed", r.damageSpeed(), pushSpeed, RUN_OVER_SPEED_LIMIT,
                Math.max(pushSpeed, DEFAULT.runOver().damageSpeed()), fixes);
        double damagePerSpeed = clamp("runOver.damagePerSpeed", r.damagePerSpeed(), 0.0, DAMAGE_PER_SPEED_LIMIT,
                DEFAULT.runOver().damagePerSpeed(), fixes);
        double knockback = clamp("runOver.knockback", r.knockback(), 0.0, KNOCKBACK_LIMIT,
                DEFAULT.runOver().knockback(), fixes);
        RunOverRules runOver = new RunOverRules(r.enabled(), pushSpeed, damageSpeed, damagePerSpeed, knockback, r.hurtPlayers());

        int maxFuel = (int) clamp("maxFuel", config.maxFuel(), 1, MAX_FUEL_LIMIT, DEFAULT.maxFuel(), fixes);
        int fuelPerItem = (int) clamp("fuelPerItem", config.fuelPerItem(), 0, maxFuel, DEFAULT.fuelPerItem(), fixes);
        return new GarageConfig(maxFuel, fuelPerItem, tuning, runOver);
    }

    private static double clamp(String key, double value, double min, double max, double fallback, List<String> fixes) {
        if (Double.isNaN(value)) {
            fixes.add(key + " no es un número; se usa " + fallback);
            return Math.clamp(fallback, min, max);
        }
        double clamped = Math.clamp(value, min, max);
        if (clamped != value) {
            fixes.add(key + " = " + value + " está fuera de [" + min + ", " + max + "]; se usa " + clamped);
        }
        return clamped;
    }

    /** Un número fuera del rango de {@code int} se satura (y luego {@link #sanitize} lo acota) en vez de dar la vuelta. */
    private static void saturateInt(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            json.addProperty(key, (int) Math.clamp(element.getAsDouble(), Integer.MIN_VALUE, Integer.MAX_VALUE));
        }
    }

    /**
     * {@code engineVolume} vivía en este archivo y ahora es de {@link ClientConfig}: antes de reescribirlo sin esa
     * clave, el valor se pasa a {@code garage-client.json}, junto a este archivo, si todavía no existe.
     */
    private static void migrateLegacyEngineVolume(Path file, JsonObject user) {
        JsonElement legacy = user.get("engineVolume");
        if (legacy != null && legacy.isJsonPrimitive() && legacy.getAsJsonPrimitive().isNumber()) {
            ClientConfig.migrate(file.resolveSibling(ClientConfig.FILE_NAME), legacy.getAsFloat());
        }
    }

    /** Copia las claves del usuario sobre los valores por defecto, entrando en los objetos anidados. */
    private static void mergeInto(JsonObject base, JsonObject user) {
        for (Map.Entry<String, JsonElement> entry : user.entrySet()) {
            JsonElement current = base.get(entry.getKey());
            if (current == null) continue; // clave desconocida (o de una versión vieja): se descarta
            if (entry.getValue().isJsonNull() && !current.isJsonObject()) continue; // "clave": null = valor por defecto
            if (current.isJsonObject() && entry.getValue().isJsonObject()) {
                mergeInto(current.getAsJsonObject(), entry.getValue().getAsJsonObject());
            } else {
                base.add(entry.getKey(), entry.getValue());
            }
        }
    }
}
