package io.github.alexf6.garage.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientConfigTest {

    @TempDir
    Path dir;

    @AfterEach
    void reset() {
        ClientConfig.load(dir.resolve("vacio.json"), null);
    }

    @Test
    void sinArchivoUsaElValorPorDefectoYLoCrea() throws Exception {
        Path file = dir.resolve("garage-client.json");
        ClientConfig.load(file, null);
        assertEquals(ClientConfig.DEFAULT, ClientConfig.get());
        assertTrue(Files.readString(file).contains("engineVolume"));
    }

    @Test
    void sinArchivoSeMigraElVolumenDeLaConfigAntigua() throws Exception {
        Path file = dir.resolve("garage-client.json");
        ClientConfig.load(file, 0.3F);
        assertEquals(0.3F, ClientConfig.get().engineVolume());
        assertTrue(Files.readString(file).contains("0.3"), "queda escrito en el archivo nuevo");
    }

    @Test
    void siYaExisteElArchivoNuevoGanaElVolumenAntiguo() throws Exception {
        Path file = dir.resolve("garage-client.json");
        Files.writeString(file, "{\"engineVolume\": 0.9}");
        ClientConfig.load(file, 0.1F);
        assertEquals(0.9F, ClientConfig.get().engineVolume());
    }

    @Test
    void unArchivoSinLaClaveUsaElValorPorDefecto() throws Exception {
        Path file = dir.resolve("garage-client.json");
        Files.writeString(file, "{}");
        ClientConfig.load(file, 0.1F);
        assertEquals(ClientConfig.DEFAULT, ClientConfig.get());
    }

    @Test
    void unVolumenFueraDeRangoSeAcotaYSeAvisa() throws Exception {
        Path file = dir.resolve("garage-client.json");
        Files.writeString(file, "{\"engineVolume\": 5.0}");
        ClientConfig.load(file, null);
        assertEquals(1.0F, ClientConfig.get().engineVolume());
        List<String> fixes = new ArrayList<>();
        assertEquals(0.0F, ClientConfig.sanitize(new ClientConfig(-2.0F), fixes).engineVolume());
        assertEquals(1, fixes.size());
    }

    @Test
    void nanVuelveAlValorPorDefecto() {
        List<String> fixes = new ArrayList<>();
        assertEquals(ClientConfig.DEFAULT, ClientConfig.sanitize(new ClientConfig(Float.NaN), fixes));
        assertEquals(1, fixes.size());
        assertTrue(ClientConfig.sanitize(ClientConfig.DEFAULT, new ArrayList<>()).equals(ClientConfig.DEFAULT));
    }

    @Test
    void unArchivoCorruptoSeGuardaComoBakYSeRegenera() throws Exception {
        Path file = dir.resolve("garage-client.json");
        Files.writeString(file, "{ esto no es json");
        ClientConfig.load(file, null);
        assertEquals(ClientConfig.DEFAULT, ClientConfig.get());
        assertEquals("{ esto no es json", Files.readString(dir.resolve("garage-client.json.bak")));
        assertTrue(Files.readString(file).contains("engineVolume"));
    }

    @Test
    void bytesQueNoSonUtf8Ni_UnTipoIncorrectoSeTratanComoCorruptos() throws Exception {
        Path file = dir.resolve("garage-client.json");
        byte[] garbage = {'{', (byte) 0xC3, (byte) 0x28, '}'};
        Files.write(file, garbage);
        ClientConfig.load(file, null);
        assertArrayEquals(garbage, Files.readAllBytes(dir.resolve("garage-client.json.bak")));
        Files.writeString(file, "{\"engineVolume\": \"fuerte\"}");
        ClientConfig.load(file, null);
        assertEquals(ClientConfig.DEFAULT, ClientConfig.get());
        assertEquals("{\"engineVolume\": \"fuerte\"}", Files.readString(dir.resolve("garage-client.json.bak")));
    }

    @Test
    void laEscrituraNoDejaArchivosTemporales() throws Exception {
        ClientConfig.load(dir.resolve("garage-client.json"), null);
        try (var files = Files.list(dir)) {
            assertEquals(List.of("garage-client.json"), files.map(p -> p.getFileName().toString()).sorted().toList());
        }
        assertFalse(Files.exists(dir.resolve("garage-client.json.bak")));
    }
}
