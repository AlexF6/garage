package io.github.alexf6.garage.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VehicleMathTest {

    private static final double EPS = 1.0e-9;

    @Test
    void forwardOfProyectaSobreElRumbo() {
        // yaw 0 mira a +Z; yaw 90 mira a -X
        assertEquals(2.0, VehicleMath.forwardOf(0, 2, 0), EPS);
        assertEquals(2.0, VehicleMath.forwardOf(-2, 0, 90), EPS);
        assertEquals(0.0, VehicleMath.forwardOf(2, 0, 0), EPS);
        assertEquals(-2.0, VehicleMath.forwardOf(0, -2, 0), EPS);
        assertEquals(0.0, VehicleMath.forwardOf(0, 0, 123), EPS);
    }

    @Test
    void directYawEnElAireConservaParteDelGiroYSumaElDirecto() {
        assertEquals(10 * VehicleMath.AIR_YAW_RETAIN + 2.8F, VehicleMath.directYaw(10, 0.5, false, 1, 2.8F), 1e-6);
        assertEquals(-2.8F, VehicleMath.directYaw(0, 0.5, false, -1, 2.8F), 1e-6);
        assertEquals(0.0F, VehicleMath.directYaw(0, 0.5, false, 0, 2.8F), 1e-6);
    }

    @Test
    void directYawCasiDetenidoSumaElGiroDirectoSoloSiHayTecla() {
        assertEquals(1.2F, VehicleMath.directYaw(0, 0.0, true, 1, 1.2F), 1e-6);
        assertEquals(-1.2F, VehicleMath.directYaw(0, -0.05, true, -1, 1.2F), 1e-6);
        assertEquals(0.0F, VehicleMath.directYaw(0, 0.0, true, 0, 1.2F), 1e-6);
    }

    @Test
    void directYawEnMovimientoEnElSueloNoCambiaLaFisica() {
        assertEquals(7.0F, VehicleMath.directYaw(7.0F, VehicleMath.SLOW_SPEED, true, 1, 2.8F), 1e-6);
        assertEquals(7.0F, VehicleMath.directYaw(7.0F, -0.3, true, -1, 2.8F), 1e-6);
    }

    @Test
    void exitPointSaleDelCuadradoPorElLadoCorrecto() {
        VehicleMath.Planar right = VehicleMath.exitPoint(0.4, 0.0, 1, 0, 1.2);
        assertEquals(1.2, right.x(), EPS);
        assertEquals(0.0, right.z(), EPS);
        VehicleMath.Planar left = VehicleMath.exitPoint(-0.4, 0.5, -1, 0, 1.2);
        assertEquals(-1.2, left.x(), EPS);
        assertEquals(0.5, left.z(), EPS);
        VehicleMath.Planar diagonal = VehicleMath.exitPoint(0, 0, 1, 1, 2);
        assertEquals(2.0, diagonal.x(), EPS);
        assertEquals(2.0, diagonal.z(), EPS);
    }

    @Test
    void exitPointConDireccionNulaDevuelveElPuntoDePartida() {
        VehicleMath.Planar p = VehicleMath.exitPoint(0.3, -0.2, 0, 0, 1.0);
        assertEquals(0.3, p.x(), EPS);
        assertEquals(-0.2, p.z(), EPS);
    }

    @Test
    void pushDirectionDesviaHaciaElLadoDeLaVictima() {
        // El vehículo va hacia +Z; una víctima a la derecha (+X) se empuja hacia +X además de adelante
        VehicleMath.Planar push = VehicleMath.pushDirection(0, 1, 1, 0);
        assertEquals(VehicleMath.SIDE_PUSH, push.x(), EPS);
        assertEquals(1.0, push.z(), EPS);
        VehicleMath.Planar other = VehicleMath.pushDirection(0, 1, -1, 0);
        assertEquals(-VehicleMath.SIDE_PUSH, other.x(), EPS);
    }

    @Test
    void pushDirectionDeUnaVictimaCentradaVaDerecho() {
        VehicleMath.Planar push = VehicleMath.pushDirection(0, 1, 0, 3);
        assertEquals(0.0, push.x(), EPS);
        assertEquals(1.0, push.z(), EPS);
        VehicleMath.Planar same = VehicleMath.pushDirection(0, 1, 0, 0);
        assertEquals(1.0, same.z(), EPS);
    }

    @Test
    void fullTurnsSoloRestaVueltasCompletas() {
        assertEquals(0.0F, VehicleMath.fullTurns(0.0F));
        assertEquals(0.0F, VehicleMath.fullTurns(VehicleMath.TWO_PI));
        assertEquals(0.0F, VehicleMath.fullTurns(-VehicleMath.TWO_PI));
        float roll = 3.5F * VehicleMath.TWO_PI;
        float rest = roll - VehicleMath.fullTurns(roll);
        assertTrue(Math.abs(rest) <= VehicleMath.TWO_PI);
        assertEquals(0.5F * VehicleMath.TWO_PI, rest, 1e-4F);
        float negative = -3.5F * VehicleMath.TWO_PI;
        assertEquals(-0.5F * VehicleMath.TWO_PI, negative - VehicleMath.fullTurns(negative), 1e-4F);
    }

    @Test
    void nextVisualSteerSigueAlObjetivoSinPasarseDelLimite() {
        assertEquals(0.25F, VehicleMath.nextVisualSteer(0.0F, 0.5, 1.0), 1e-6);
        // el objetivo se recorta al límite antes de suavizar
        assertEquals(0.5F, VehicleMath.nextVisualSteer(0.0F, 5.0, 1.0), 1e-6);
        assertEquals(-0.5F, VehicleMath.nextVisualSteer(0.0F, -5.0, 1.0), 1e-6);
    }

    @Test
    void nextVisualSteerDetenidoVuelveAlCentro() {
        float steer = 0.4F;
        for (int i = 0; i < 40; i++) steer = VehicleMath.nextVisualSteer(steer, Double.NaN, 0.6);
        assertTrue(Math.abs(steer) < 0.01F, "vuelve al centro: " + steer);
    }

    @Test
    void speedRatioSeAcotaEnCeroYUno() {
        assertEquals(0.0, VehicleMath.speedRatio(0.0, 0.6), EPS);
        assertEquals(0.5, VehicleMath.speedRatio(-0.3, 0.6), EPS);
        assertEquals(1.0, VehicleMath.speedRatio(5.0, 0.6), EPS);
    }
}
