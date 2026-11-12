package io.github.alexf6.garage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Cada vehículo de {@link VehicleSpecs#ALL} debe traer todo lo que el juego le pide fuera del código: traducciones,
 * modelos e ítems, texturas y receta. Sin este test, lo que falta solo se descubre al jugar (ítem sin nombre, textura
 * magenta, receta inexistente).
 */
class VehicleResourcesTest {

    private static final List<String> LANGS = List.of("en_us", "es_es", "es_ar");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static InputStream open(String path) {
        return VehicleResourcesTest.class.getClassLoader().getResourceAsStream(path);
    }

    private static JsonObject json(String path) throws Exception {
        try (InputStream in = open(path)) {
            assertNotNull(in, "falta " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void assertExists(String path) throws Exception {
        try (InputStream in = open(path)) {
            assertNotNull(in, "falta " + path);
            assertTrue(in.available() > 0, "vacío: " + path);
        }
    }

    @Test
    void cadaVehiculoTieneSusArchivosDeRecursos() throws Exception {
        for (VehicleSpec spec : VehicleSpecs.ALL) {
            String id = spec.id();
            assertExists("assets/garage/items/" + id + ".json");
            assertExists("assets/garage/models/item/" + id + ".json");
            assertExists("assets/garage/textures/item/" + id + ".png");
            assertExists("assets/garage/textures/item/" + id + "_trim.png");
            assertExists("assets/garage/textures/entity/" + id + ".png");
            assertExists("data/garage/recipe/" + id + ".json");
        }
    }

    @Test
    void elItemYSuRecetaApuntanAlMismoVehiculo() throws Exception {
        for (VehicleSpec spec : VehicleSpecs.ALL) {
            String id = spec.id();
            JsonObject item = json("assets/garage/items/" + id + ".json");
            assertEquals("garage:item/" + id, item.getAsJsonObject("model").get("model").getAsString(), id);
            JsonObject model = json("assets/garage/models/item/" + id + ".json").getAsJsonObject("textures");
            assertEquals("garage:item/" + id, model.get("layer0").getAsString(), id);
            assertEquals("garage:item/" + id + "_trim", model.get("layer1").getAsString(), id);
            JsonObject recipe = json("data/garage/recipe/" + id + ".json");
            assertEquals("garage:" + id, recipe.getAsJsonObject("result").get("id").getAsString(), id);
        }
    }

    @Test
    void cadaVehiculoTieneNombreEnTodosLosIdiomas() throws Exception {
        for (String lang : LANGS) {
            JsonObject strings = json("assets/garage/lang/" + lang + ".json");
            for (VehicleSpec spec : VehicleSpecs.ALL) {
                for (String key : new String[] {"item.garage." + spec.id(), "entity.garage." + spec.id()}) {
                    assertTrue(strings.has(key), lang + " no tiene " + key);
                    assertFalse(strings.get(key).getAsString().isBlank(), lang + ": " + key + " vacío");
                }
            }
        }
    }

    @Test
    void todosLosIdiomasTienenLasMismasClaves() throws Exception {
        Set<String> base = json("assets/garage/lang/en_us.json").keySet();
        for (String lang : LANGS) {
            Set<String> keys = new HashSet<>(json("assets/garage/lang/" + lang + ".json").keySet());
            Set<String> missing = new HashSet<>(base);
            missing.removeAll(keys);
            Set<String> extra = new HashSet<>(keys);
            extra.removeAll(base);
            assertTrue(missing.isEmpty(), lang + " no tiene: " + missing);
            assertTrue(extra.isEmpty(), lang + " tiene de más: " + extra);
        }
    }

    @Test
    void lasTraduccionesDelModNoEstanVacias() throws Exception {
        for (String lang : LANGS) {
            JsonObject strings = json("assets/garage/lang/" + lang + ".json");
            String key = "modmenu.descriptionTranslation.garage";
            assertTrue(strings.has(key), lang + " sin descripción del mod");
            assertFalse(strings.get(key).getAsString().isBlank(), lang + ": descripción vacía");
        }
    }
}
