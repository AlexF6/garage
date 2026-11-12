package io.github.alexf6.garage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexf6.garage.physics.VehicleTuning;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GarageConfigTest {

    @TempDir
    Path dir;

    @AfterEach
    void reset() throws Exception {
        GarageConfig.load(dir.resolve("vacio.json"));
    }

    @Test
    void sinArchivoUsaLosValoresPorDefectoYLoCrea() throws Exception {
        Path file = dir.resolve("garage.json");
        GarageConfig.load(file);
        assertEquals(GarageConfig.DEFAULT, GarageConfig.get());
        assertTrue(Files.readString(file).contains("\"driftGrip\""));
    }

    @Test
    void completaLasClavesQueFaltan() throws Exception {
        Path file = dir.resolve("garage.json");
        Files.writeString(file, "{\"maxFuel\": 500, \"tuning\": {\"maxSpeed\": 0.9}, \"runOver\": {\"enabled\": false}}");
        GarageConfig.load(file);
        GarageConfig config = GarageConfig.get();
        assertEquals(500, config.maxFuel());
        assertEquals(0.9, config.tuning().maxSpeed());
        assertEquals(GarageConfig.DEFAULT.tuning().wheelBase(), config.tuning().wheelBase());
        assertFalse(config.runOver().enabled());
        assertEquals(GarageConfig.DEFAULT.runOver().knockback(), config.runOver().knockback());
        assertTrue(Files.readString(file).contains("\"wheelBase\""), "reescribe el archivo con todo");
    }

    @Test
    void tuningInvalidoVuelveAlManejoPorDefecto() throws Exception {
        Path file = dir.resolve("garage.json");
        Files.writeString(file, "{\"maxFuel\": 700, \"tuning\": {\"wheelBase\": 0}}");
        GarageConfig.load(file);
        assertEquals(GarageConfig.DEFAULT.tuning(), GarageConfig.get().tuning());
        assertEquals(700, GarageConfig.get().maxFuel());
    }

    @Test
    void archivoRotoSeGuardaComoBakYSeRegenera() throws Exception {
        Path file = dir.resolve("garage.json");
        Files.writeString(file, "{ esto no es json");
        GarageConfig.load(file);
        assertEquals(GarageConfig.DEFAULT, GarageConfig.get());
        assertEquals("{ esto no es json", Files.readString(dir.resolve("garage.json.bak")));
        assertTrue(Files.readString(file).contains("\"maxFuel\""), "regenera el archivo con los valores por defecto");
    }

    @Test
    void archivoVacioOQueNoEsUnObjetoSeTrataComoRoto() throws Exception {
        for (String content : new String[] {"", "[1, 2]", "\"hola\""}) {
            Path file = dir.resolve("garage.json");
            Files.writeString(file, content);
            GarageConfig.load(file);
            assertEquals(GarageConfig.DEFAULT, GarageConfig.get(), content);
            assertEquals(content, Files.readString(dir.resolve("garage.json.bak")), content);
        }
    }

    @Test
    void tipoIncorrectoSeTrataComoRoto() throws Exception {
        for (String content : new String[] {"{\"maxFuel\": \"mucho\"}", "{\"runOver\": 5}"}) {
            Path file = dir.resolve("garage.json");
            Files.writeString(file, content);
            GarageConfig.load(file);
            assertEquals(GarageConfig.DEFAULT, GarageConfig.get(), content);
            assertEquals(content, Files.readString(dir.resolve("garage.json.bak")), content);
        }
    }

    @Test
    void seccionesNulasUsanLosValoresPorDefecto() throws Exception {
        Path file = dir.resolve("garage.json");
        Files.writeString(file, "{\"tuning\": null, \"runOver\": null}");
        GarageConfig.load(file);
        assertEquals(GarageConfig.DEFAULT, GarageConfig.get());
    }

    @Test
    void combustibleFueraDeRangoSeAcotaYSeAvisa() throws Exception {
        Path file = dir.resolve("garage.json");
        Files.writeString(file, "{\"maxFuel\": 2000000000, \"fuelPerItem\": 2000000000, \"engineVolume\": 5.0}");
        GarageConfig.load(file);
        GarageConfig config = GarageConfig.get();
        assertEquals(GarageConfig.MAX_FUEL_LIMIT, config.maxFuel());
        assertEquals(GarageConfig.MAX_FUEL_LIMIT, config.fuelPerItem());
        assertEquals(1.0F, config.engineVolume());
        assertTrue(Files.readString(file).contains("1000000"), "el archivo se reescribe con el valor corregido");
    }

    @Test
    void enteroFueraDeRangoDeIntSeSaturaEnVezDeDarLaVuelta() throws Exception {
        Path file = dir.resolve("garage.json");
        Files.writeString(file, "{\"maxFuel\": 99999999999, \"fuelPerItem\": -99999999999}");
        GarageConfig.load(file);
        assertEquals(GarageConfig.MAX_FUEL_LIMIT, GarageConfig.get().maxFuel());
        assertEquals(0, GarageConfig.get().fuelPerItem());
        assertFalse(Files.exists(dir.resolve("garage.json.bak")), "no es un archivo roto");
    }

    @Test
    void sanitizeAnotaCadaCorreccion() {
        List<String> fixes = new ArrayList<>();
        GarageConfig raw = new GarageConfig(0, -3, Float.NaN, GarageConfig.DEFAULT.tuning(),
                new GarageConfig.RunOver(true, 99.0, 0.0, 5000.0, -1.0, false));
        GarageConfig fixed = GarageConfig.sanitize(raw, fixes);
        assertEquals(1, fixed.maxFuel());
        assertEquals(0, fixed.fuelPerItem());
        assertEquals(GarageConfig.DEFAULT.engineVolume(), fixed.engineVolume());
        assertEquals(10.0, fixed.runOver().pushSpeed());
        assertEquals(10.0, fixed.runOver().damageSpeed(), "damageSpeed nunca baja de pushSpeed");
        assertEquals(1000.0, fixed.runOver().damagePerSpeed());
        assertEquals(0.0, fixed.runOver().knockback());
        assertEquals(7, fixes.size(), fixes.toString());
    }

    @Test
    void sanitizeNoAvisaConUnaConfigValida() {
        List<String> fixes = new ArrayList<>();
        assertEquals(GarageConfig.DEFAULT, GarageConfig.sanitize(GarageConfig.DEFAULT, fixes));
        assertTrue(fixes.isEmpty());
    }

    @Test
    void elManejoSincronizadoGanaALaConfigLocalHastaDesconectar() {
        VehicleTuning remote = VehicleTuning.ENDURO;
        GarageConfig.setSyncedTuning(remote);
        try {
            assertEquals(remote, GarageConfig.carTuning());
        } finally {
            GarageConfig.clearSyncedTuning();
        }
        assertEquals(GarageConfig.DEFAULT.tuning(), GarageConfig.carTuning());
    }

    @Test
    void runOverConValoresFueraDeRangoSeCorrige() throws Exception {
        Path file = dir.resolve("garage.json");
        Files.writeString(file, "{\"runOver\": {\"pushSpeed\": -1.0, \"damageSpeed\": -2.0, \"damagePerSpeed\": -5.0, \"knockback\": 99.0}}");
        GarageConfig.load(file);
        GarageConfig.RunOver r = GarageConfig.get().runOver();
        assertEquals(0.0, r.pushSpeed());
        assertEquals(0.0, r.damageSpeed());
        assertEquals(0.0, r.damagePerSpeed());
        assertEquals(10.0, r.knockback());
    }
}
