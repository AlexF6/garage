package io.github.alexf6.garage;

import io.github.alexf6.garage.physics.CarTuning;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Ajustes del mod en {@code config/garage.json}. Las claves que falten se completan con los valores por
 * defecto y el archivo se reescribe, así las opciones nuevas aparecen solas al actualizar el mod.
 * Se lee al arrancar el juego (o el servidor): para aplicar cambios hay que reiniciar.
 *
 * <p>En multijugador la física la calcula el cliente del conductor con <em>su</em> {@code tuning}; el
 * combustible y los atropellos los decide el servidor con el suyo.
 *
 * @param maxFuel      ticks de acelerador con el tanque lleno
 * @param fuelPerItem  ticks que suma cada unidad de combustible
 * @param engineVolume volumen del motor (0 lo apaga)
 */
public record CarConfig(int maxFuel, int fuelPerItem, float engineVolume, CarTuning tuning, RunOver runOver) {

    /**
     * Atropello y empuje de criaturas, en bloques/tick (0.1 b/t = 7.2 km/h).
     *
     * @param pushSpeed      desde esta velocidad el carro aparta a las criaturas que tiene delante
     * @param damageSpeed    desde esta velocidad además las lastima
     * @param damagePerSpeed daño por cada bloque/tick por encima de {@code damageSpeed}
     * @param knockback      fuerza del empuje a velocidad máxima
     * @param hurtPlayers    si también lastima a otros jugadores (siempre los empuja)
     */
    public record RunOver(boolean enabled, double pushSpeed, double damageSpeed, double damagePerSpeed,
                          double knockback, boolean hurtPlayers) {}

    public static final CarConfig DEFAULT = new CarConfig(12000, 1600, 0.6F, CarTuning.WILLYS,
            new RunOver(true, 0.1, 0.25, 25.0, 1.2, false));

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CarConfig current = DEFAULT;

    public static CarConfig get() {
        return current;
    }

    public static void load(Path file) {
        JsonObject merged = GSON.toJsonTree(DEFAULT).getAsJsonObject();
        try {
            if (Files.exists(file)) {
                mergeInto(merged, JsonParser.parseString(Files.readString(file)).getAsJsonObject());
            }
            current = validate(GSON.fromJson(merged, CarConfig.class));
            Files.writeString(file, GSON.toJson(current));
        } catch (IOException | RuntimeException e) {
            // Un archivo roto no debe impedir jugar: se usan los valores por defecto y no se sobrescribe
            GarageMod.LOGGER.warn("No se pudo leer {}; se usan los valores por defecto: {}", file, e.toString());
            current = DEFAULT;
        }
    }

    private static CarConfig validate(CarConfig config) {
        CarTuning tuning = config.tuning();
        String invalid = tuning == null ? "null" : tuning.firstInvalid();
        if (invalid != null) {
            GarageMod.LOGGER.warn("garage.json: valor inválido en tuning ({}); se usa el manejo por defecto", invalid);
            tuning = DEFAULT.tuning();
        }
        RunOver r = config.runOver() != null ? config.runOver() : DEFAULT.runOver();
        double pushSpeed = Math.clamp(r.pushSpeed(), 0.0, 10.0);
        double damageSpeed = Math.clamp(r.damageSpeed(), pushSpeed, 10.0);
        double damagePerSpeed = Math.clamp(r.damagePerSpeed(), 0.0, 1000.0);
        double knockback = Math.clamp(r.knockback(), 0.0, 10.0);
        RunOver runOver = new RunOver(r.enabled(), pushSpeed, damageSpeed, damagePerSpeed, knockback, r.hurtPlayers());
        return new CarConfig(Math.max(1, config.maxFuel()), Math.max(0, config.fuelPerItem()),
                Math.clamp(config.engineVolume(), 0.0F, 1.0F), tuning, runOver);
    }

    /** Copia las claves del usuario sobre los valores por defecto, entrando en los objetos anidados. */
    private static void mergeInto(JsonObject base, JsonObject user) {
        for (Map.Entry<String, JsonElement> entry : user.entrySet()) {
            JsonElement current = base.get(entry.getKey());
            if (current == null) continue; // clave desconocida (o de una versión vieja): se descarta
            if (current.isJsonObject() && entry.getValue().isJsonObject()) {
                mergeInto(current.getAsJsonObject(), entry.getValue().getAsJsonObject());
            } else {
                base.add(entry.getKey(), entry.getValue());
            }
        }
    }
}
