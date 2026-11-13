package io.github.alexf6.garage.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VehicleAnimationTest {

    private static final VehicleTuning T = VehicleTuning.WILLYS;

    @Test
    void lasRuedasRuedanDistanciaEntreRadio() {
        VehicleAnimation a = new VehicleAnimation(0.3, 0.0F);
        a.tick(0.3, 0.0, 0.0F, 0.0, T);
        assertEquals(1.0F, a.wheelRoll(1.0F), 1e-6F);
        assertEquals(0.0F, a.wheelRoll(0.0F), 1e-6F, "el tick anterior estaba en 0");
        assertEquals(0.5F, a.wheelRoll(0.5F), 1e-6F);
    }

    @Test
    void enReversaLasRuedasGiranAlReves() {
        VehicleAnimation a = new VehicleAnimation(0.3, 0.0F);
        a.tick(-0.3, 0.0, 0.0F, 0.0, T);
        assertEquals(-1.0F, a.wheelRoll(1.0F), 1e-6F);
    }

    @Test
    void elRodadoNoCreceSinLimiteYLaInterpolacionSigueContinua() {
        VehicleAnimation a = new VehicleAnimation(0.3, 0.0F);
        for (int i = 0; i < 2000; i++) a.tick(0.6, 0.0, 0.0F, 0.0, T);
        assertTrue(Math.abs(a.wheelRoll(1.0F)) <= VehicleMath.TWO_PI + 1e-3F, "" + a.wheelRoll(1.0F));
        float step = a.wheelRoll(1.0F) - a.wheelRoll(0.0F);
        assertEquals(2.0F, step, 1e-3F, "avanza 0.6 / 0.3 = 2 rad por tick sin saltos al dar la vuelta");
    }

    @Test
    void unRadioONegativoOLaInclinacionNegativaSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> new VehicleAnimation(0.0, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> new VehicleAnimation(-0.3, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> new VehicleAnimation(Double.NaN, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> new VehicleAnimation(0.3, -0.1F));
    }

    @Test
    void sinInclinacionConfiguradaNuncaSeInclina() {
        VehicleAnimation a = new VehicleAnimation(0.3, 0.0F);
        for (int i = 0; i < 30; i++) a.tick(0.5, 0.0, 5.0F, 0.0, T);
        assertEquals(0.0F, a.lean(1.0F));
    }

    @Test
    void conInclinacionSeInclinaHaciaLaDireccionAlGirar() {
        VehicleAnimation a = new VehicleAnimation(0.3, 0.48F);
        for (int i = 0; i < 40; i++) a.tick(0.5, 0.0, 6.0F, 0.0, T);
        assertTrue(Math.abs(a.steer(1.0F)) > 0.0F);
        assertEquals(Math.signum(a.steer(1.0F)), Math.signum(a.lean(1.0F)), "misma dirección que el volante");
        assertTrue(Math.abs(a.lean(1.0F)) > 0.0F);
    }

    @Test
    void detenidoLaDireccionVuelveAlCentro() {
        VehicleAnimation a = new VehicleAnimation(0.3, 0.0F);
        for (int i = 0; i < 20; i++) a.tick(0.4, 0.0, 6.0F, 0.0, T);
        assertTrue(Math.abs(a.steer(1.0F)) > 0.05F);
        for (int i = 0; i < 60; i++) a.tick(0.0, 0.0, 0.0F, 0.0, T);
        assertEquals(0.0F, a.steer(1.0F), 0.01F);
    }

    @Test
    void laSuspensionExisteYReaccionaAlFrenado() {
        VehicleAnimation a = new VehicleAnimation(0.3, 0.0F);
        for (int i = 0; i < 5; i++) a.tick(0.4, -0.1, 0.0F, 0.0, T);
        assertTrue(a.suspension().pitch(1.0F) != 0.0F);
    }
}
