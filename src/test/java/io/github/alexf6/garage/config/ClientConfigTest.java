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
    void reset() throws Exception {
        ClientConfig.load(Files.createDirectories(dir.resolve("limpia")).resolve("garage-client.json"));
    }

    @Test
    void sinArchivoUsaElValorPorDefectoYLoCrea() throws Exception {
        Path file = dir.resolve("garage-client.json");
        ClientConfig.load(file);
        assertEquals(ClientConfig.DEFAULT, ClientConfig.get());
        assertTrue(Files.readString(file).contains("engineVolume"));
    }

    @Test
    void createEscribeElArchivoConElValorDado() throws Exception {
        Path file = dir.resolve("garage-client.json");
        assertTrue(ClientConfig.create(file, 0.3F));
        ClientConfig.load(file);
        assertEquals(0.3F, ClientConfig.get().engineVolume());
    }

    @Test
    void createAcotaUnValorFueraDeRango() throws Exception {
        Path file = dir.resolve("garage-client.json");
        ClientConfig.create(file, 7.0F);
        ClientConfig.load(file);
        assertEquals(1.0F, ClientConfig.get().engineVolume());
    }

    @Test
    void createDevuelveFalseSiNoPuedeEscribir() throws Exception {
        Path file = dir.resolve("garage-client.json");
        Files.createDirectories(dir.resolve("garage-client.json.tmp/ocupado")); // el temporal es un directorio
        assertFalse(ClientConfig.create(file, 0.3F));
        assertFalse(Files.exists(file));
    }

    @Test
    void unArchivoSinLaClaveUsaElValorPorDefecto() throws Exception {
        Path file = dir.resolve("garage-client.json");
        Files.writeString(file, "{}");
        ClientConfig.load(file);
        assertEquals(ClientConfig.DEFAULT, ClientConfig.get());
    }

    @Test
    void unVolumenFueraDeRangoSeAcotaYSeAvisa() throws Exception {
        Path file = dir.resolve("garage-client.json");
        Files.writeString(file, "{\"engineVolume\": 5.0}");
        ClientConfig.load(file);
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
    }

    @Test
    void unVolumenValidoNoGeneraAvisos() {
        List<String> fixes = new ArrayList<>();
        assertEquals(new ClientConfig(0.4F), ClientConfig.sanitize(new ClientConfig(0.4F), fixes));
        assertTrue(fixes.isEmpty());
    }

    @Test
    void unArchivoCorruptoSeGuardaComoBakYSeRegenera() throws Exception {
        Path file = dir.resolve("garage-client.json");
        Files.writeString(file, "{ esto no es json");
        ClientConfig.load(file);
        assertEquals(ClientConfig.DEFAULT, ClientConfig.get());
        assertEquals("{ esto no es json", Files.readString(dir.resolve("garage-client.json.bak")));
        assertTrue(Files.readString(file).contains("engineVolume"));
    }

    @Test
    void bytesQueNoSonUtf8SeTratanComoCorruptos() throws Exception {
        Path file = dir.resolve("garage-client.json");
        byte[] garbage = {'{', (byte) 0xC3, (byte) 0x28, '}'};
        Files.write(file, garbage);
        ClientConfig.load(file);
        assertEquals(ClientConfig.DEFAULT, ClientConfig.get());
        assertArrayEquals(garbage, Files.readAllBytes(dir.resolve("garage-client.json.bak")));
    }

    @Test
    void unValorQueNoEsUnNumeroSeTrataComoCorrupto() throws Exception {
        for (String content : new String[] {"{\"engineVolume\": \"fuerte\"}", "{\"engineVolume\": {}}",
                "{\"engineVolume\": []}", "{\"engineVolume\": true}", "[1]", ""}) {
            Path file = dir.resolve("garage-client.json");
            Files.deleteIfExists(dir.resolve("garage-client.json.bak"));
            Files.writeString(file, content);
            ClientConfig.load(file);
            assertEquals(ClientConfig.DEFAULT, ClientConfig.get(), content);
            assertEquals(content, Files.readString(dir.resolve("garage-client.json.bak")), content);
            assertTrue(Files.readString(file).contains("engineVolume"), "regenerado: " + content);
        }
    }

    @Test
    void unaClaveNulaUsaElValorPorDefecto() throws Exception {
        Path file = dir.resolve("garage-client.json");
        Files.writeString(file, "{\"engineVolume\": null}");
        ClientConfig.load(file);
        assertEquals(ClientConfig.DEFAULT, ClientConfig.get());
        assertFalse(Files.exists(dir.resolve("garage-client.json.bak")));
    }

    @Test
    void laEscrituraNoDejaArchivosTemporales() throws Exception {
        ClientConfig.load(dir.resolve("garage-client.json"));
        try (var files = Files.list(dir)) {
            assertEquals(List.of("garage-client.json"), files.map(p -> p.getFileName().toString()).sorted().toList());
        }
        assertFalse(Files.exists(dir.resolve("garage-client.json.bak")));
    }
}
