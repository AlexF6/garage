package io.github.alexf6.garage.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SuspensionTest {

    @Test
    void alAcelerarSubeLaTrompaYAlFrenarBaja() {
        Suspension accel = new Suspension();
        Suspension brake = new Suspension();
        for (int i = 0; i < 5; i++) {
            accel.tick(0.03, 0, 0);
            brake.tick(-0.04, 0, 0);
        }
        assertTrue(accel.pitch(1) < 0);
        assertTrue(brake.pitch(1) > 0);
    }

    @Test
    void enCurvaALaDerechaSeInclinaHaciaAfuera() {
        Suspension s = new Suspension();
        for (int i = 0; i < 5; i++) s.tick(0, 0.04, 0);
        assertTrue(s.roll(1) > 0);
    }

    @Test
    void vuelveAlReposoYNoPasaLosLimites() {
        Suspension s = new Suspension();
        s.tick(5, -5, 1); // golpe exagerado (subir un bloque frenando en curva)
        assertTrue(Math.abs(s.pitch(1)) <= 0.08 + 1e-6);
        assertTrue(Math.abs(s.roll(1)) <= 0.08 + 1e-6);
        assertTrue(s.heave(1) < 0, "la carrocería se queda abajo al subir el escalón");
        assertTrue(s.heave(1) >= -0.12 - 1e-6);
        for (int i = 0; i < 100; i++) s.tick(0, 0, 0);
        assertEquals(0, s.pitch(1), 1e-3);
        assertEquals(0, s.roll(1), 1e-3);
        assertEquals(0, s.heave(1), 1e-3);
    }

    @Test
    void interpolaEntreTicks() {
        Suspension s = new Suspension();
        s.tick(0.03, 0, 0);
        assertEquals(0, s.pitch(0), 1e-9);
        assertEquals(s.pitch(1) / 2, s.pitch(0.5F), 1e-6);
    }
}
