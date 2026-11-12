package io.github.alexf6.garage.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VehiclePhysicsTest {

    private static final VehicleTuning T = VehicleTuning.WILLYS;
    private static final VehicleInput GAS = new VehicleInput(1, 0, false);
    private static final VehicleInput BACK = new VehicleInput(-1, 0, false);
    private static final VehicleInput HANDBRAKE = new VehicleInput(0, 0, true);

    private static double run(double speed, VehicleInput input, boolean fuelled, int ticks) {
        for (int i = 0; i < ticks; i++) speed = VehiclePhysics.nextSpeed(speed, input, fuelled, true, false, T);
        return speed;
    }

    @Test
    void aceleraHastaCercaDelMaximoSinPasarlo() {
        double speed = 0;
        for (int i = 0; i < 400; i++) {
            speed = VehiclePhysics.nextSpeed(speed, GAS, true, true, false, T);
            assertTrue(speed <= T.maxSpeed());
        }
        assertTrue(speed > T.maxSpeed() * 0.95, "velocidad final " + speed);
        // Arranque fuerte: en 2 segundos ya va a más de la mitad
        assertTrue(run(0, GAS, true, 40) > T.maxSpeed() * 0.5);
    }

    @Test
    void sinCombustibleNoAceleraYSeDetiene() {
        assertEquals(0.0, run(0, GAS, false, 20));
        assertEquals(0.0, run(T.maxSpeed(), GAS, false, 1000));
    }

    @Test
    void frenaDesdeVelocidadMaximaEnMenosDeUnSegundo() {
        assertTrue(ticksToStop(BACK) <= 20);
        assertTrue(ticksToStop(HANDBRAKE) <= 20);
        // Con la reversa presionada, una vez detenido empieza a retroceder
        assertTrue(run(T.maxSpeed(), BACK, true, 40) < 0);
    }

    private static int ticksToStop(VehicleInput input) {
        double speed = T.maxSpeed();
        int ticks = 0;
        while (speed > 0 && ticks < 1000) {
            speed = VehiclePhysics.nextSpeed(speed, input, true, true, false, T);
            ticks++;
        }
        return ticks;
    }

    @Test
    void reversaLimitada() {
        double speed = run(0, BACK, true, 400);
        assertTrue(speed < 0);
        assertTrue(speed >= -T.maxReverse());
    }

    @Test
    void radioDeGiroCerradoABajaVelocidad() {
        double speed = 0.05;
        double steer = 0;
        for (int i = 0; i < 20; i++) steer = VehiclePhysics.nextSteer(steer, new VehicleInput(1, 1, false), speed, T);
        double omega = Math.toRadians(VehiclePhysics.yawDelta(speed, steer, false, T));
        double radius = speed / omega;
        assertTrue(radius > 2.0 && radius < 3.0, "radio " + radius);
    }

    @Test
    void aAltaVelocidadLaDireccionSeEndurece() {
        double steerSlow = 0;
        double steerFast = 0;
        for (int i = 0; i < 20; i++) {
            steerSlow = VehiclePhysics.nextSteer(steerSlow, new VehicleInput(1, 1, false), 0.0, T);
            steerFast = VehiclePhysics.nextSteer(steerFast, new VehicleInput(1, 1, false), T.maxSpeed(), T);
        }
        assertEquals(T.maxSteerLow(), steerSlow, 1e-9);
        assertEquals(T.maxSteerHigh(), steerFast, 1e-9);
    }

    @Test
    void volanteSuaveYVuelveRapidoAlCentro() {
        double steer = VehiclePhysics.nextSteer(0, new VehicleInput(0, 1, false), 0, T);
        assertEquals(T.steerRate(), steer, 1e-9);
        double centered = VehiclePhysics.nextSteer(T.maxSteerLow(), VehicleInput.NONE, 0, T);
        assertTrue(T.maxSteerLow() - centered > T.steerRate(), "al soltar A/D el volante vuelve más rápido al centro");
    }

    @Test
    void enElAireAbsorbeVelocidadLateralParaPoderRedirigirRumbo() {
        double airLat = VehiclePhysics.nextLateral(0.4, VehicleInput.NONE, false, false, VehicleTuning.ENDURO);
        assertTrue(airLat < 0.4 * VehicleTuning.ENDURO.airDrag(), "en el aire absorbe parte del derrape para girar");
        assertTrue(airLat > VehiclePhysics.nextLateral(0.4, VehicleInput.NONE, true, false, VehicleTuning.ENDURO));
    }

    @Test
    void enReversaElGiroSeInvierte() {
        assertTrue(VehiclePhysics.yawDelta(0.2, 0.3, false, T) > 0);
        assertTrue(VehiclePhysics.yawDelta(-0.2, 0.3, false, T) < 0);
    }

    @Test
    void derrapeGiraMasYConservaVelocidadLateral() {
        assertTrue(VehiclePhysics.yawDelta(0.4, 0.2, true, T) > VehiclePhysics.yawDelta(0.4, 0.2, false, T));
        double grip = VehiclePhysics.nextLateral(0.3, VehicleInput.NONE, true, false, T);
        double drift = VehiclePhysics.nextLateral(0.3, HANDBRAKE, true, false, T);
        assertTrue(drift > grip);
        assertEquals(0.0, VehiclePhysics.nextLateral(0.002, VehicleInput.NONE, true, false, T));
    }

    @Test
    void elAnguloDeLasRuedasSeRecuperaDelMovimiento() {
        double speed = 0.3;
        double steer = 0.25;
        float yaw = VehiclePhysics.yawDelta(speed, steer, false, T);
        assertEquals(steer, VehiclePhysics.steerFromMotion(yaw, speed, T), 1e-4);
        assertTrue(Double.isNaN(VehiclePhysics.steerFromMotion(5, 0, T)));
    }

    @Test
    void validacionDeParametros() {
        assertNull(T.firstInvalid());
        assertNull(VehicleTuning.ENDURO.firstInvalid());
        assertNull(VehicleTuning.MOTOCARRO.firstInvalid());
        assertNull(VehicleTuning.MOTOCARGUERO.firstInvalid());
        assertTrue(VehicleTuning.ENDURO.maxSpeed() > T.maxSpeed());
        assertTrue(VehicleTuning.ENDURO.accel() > T.accel());
        assertTrue(VehicleTuning.MOTOCARRO.maxSpeed() > T.maxSpeed());
        assertTrue(VehicleTuning.MOTOCARRO.maxSpeed() < VehicleTuning.ENDURO.maxSpeed());
        assertTrue(VehicleTuning.MOTOCARGUERO.maxSpeed() > T.maxSpeed());
        assertTrue(VehicleTuning.MOTOCARGUERO.maxSpeed() < VehicleTuning.MOTOCARRO.maxSpeed());
        assertTrue(VehicleTuning.MOTOCARGUERO.accel() > VehicleTuning.MOTOCARRO.accel());
        VehicleTuning broken = new VehicleTuning(0, 0.2, 0.03, 0.02, 0.04, 0.035, 0.98, 0.995, 0.85, 1.75, 0.6, 0.2, 0.1,
                0.3, 0.92, 1.6);
        assertNotNull(broken.firstInvalid());
        VehicleTuning badSteer = new VehicleTuning(0.6, 0.2, 0.03, 0.02, 0.04, 0.035, 0.98, 0.995, 0.85, 1.75, 2.0, 0.2, 0.1,
                0.3, 0.92, 1.6);
        assertNotNull(badSteer.firstInvalid());
    }
}
