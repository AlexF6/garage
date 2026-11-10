package io.github.alexf6.garage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CarConfigTest {

    @TempDir
    Path dir;

    @AfterEach
    void reset() throws Exception {
        CarConfig.load(dir.resolve("vacio.json"));
    }

    @Test
    void sinArchivoUsaLosValoresPorDefectoYLoCrea() throws Exception {
        Path file = dir.resolve("garage.json");
        CarConfig.load(file);
        assertEquals(CarConfig.DEFAULT, CarConfig.get());
        assertTrue(Files.readString(file).contains("\"driftGrip\""));
    }

    @Test
    void completaLasClavesQueFaltan() throws Exception {
        Path file = dir.resolve("garage.json");
        Files.writeString(file, "{\"maxFuel\": 500, \"tuning\": {\"maxSpeed\": 0.9}, \"runOver\": {\"enabled\": false}}");
        CarConfig.load(file);
        CarConfig config = CarConfig.get();
        assertEquals(500, config.maxFuel());
        assertEquals(0.9, config.tuning().maxSpeed());
        assertEquals(CarConfig.DEFAULT.tuning().wheelBase(), config.tuning().wheelBase());
        assertFalse(config.runOver().enabled());
        assertEquals(CarConfig.DEFAULT.runOver().knockback(), config.runOver().knockback());
        assertTrue(Files.readString(file).contains("\"wheelBase\""), "reescribe el archivo con todo");
    }

    @Test
    void tuningInvalidoVuelveAlManejoPorDefecto() throws Exception {
        Path file = dir.resolve("garage.json");
        Files.writeString(file, "{\"maxFuel\": 700, \"tuning\": {\"wheelBase\": 0}}");
        CarConfig.load(file);
        assertEquals(CarConfig.DEFAULT.tuning(), CarConfig.get().tuning());
        assertEquals(700, CarConfig.get().maxFuel());
    }

    @Test
    void archivoRotoNoSeSobrescribe() throws Exception {
        Path file = dir.resolve("garage.json");
        Files.writeString(file, "{ esto no es json");
        CarConfig.load(file);
        assertEquals(CarConfig.DEFAULT, CarConfig.get());
        assertEquals("{ esto no es json", Files.readString(file));
    }

    @Test
    void runOverConValoresFueraDeRangoSeCorrige() throws Exception {
        Path file = dir.resolve("garage.json");
        Files.writeString(file, "{\"runOver\": {\"pushSpeed\": -1.0, \"damageSpeed\": -2.0, \"damagePerSpeed\": -5.0, \"knockback\": 99.0}}");
        CarConfig.load(file);
        CarConfig.RunOver r = CarConfig.get().runOver();
        assertEquals(0.0, r.pushSpeed());
        assertEquals(0.0, r.damageSpeed());
        assertEquals(0.0, r.damagePerSpeed());
        assertEquals(10.0, r.knockback());
    }
}
