package io.github.alexf6.garage.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CarPhysicsTest {

    private static final CarTuning T = CarTuning.WILLYS;
    private static final CarInput GAS = new CarInput(1, 0, false);
    private static final CarInput BACK = new CarInput(-1, 0, false);
    private static final CarInput HANDBRAKE = new CarInput(0, 0, true);

    private static double run(double speed, CarInput input, boolean fuelled, int ticks) {
        for (int i = 0; i < ticks; i++) speed = CarPhysics.nextSpeed(speed, input, fuelled, true, false, T);
        return speed;
    }

    @Test
    void aceleraHastaCercaDelMaximoSinPasarlo() {
        double speed = 0;
        for (int i = 0; i < 400; i++) {
            speed = CarPhysics.nextSpeed(speed, GAS, true, true, false, T);
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

    private static int ticksToStop(CarInput input) {
        double speed = T.maxSpeed();
        int ticks = 0;
        while (speed > 0 && ticks < 1000) {
            speed = CarPhysics.nextSpeed(speed, input, true, true, false, T);
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
        for (int i = 0; i < 20; i++) steer = CarPhysics.nextSteer(steer, new CarInput(1, 1, false), speed, T);
        double omega = Math.toRadians(CarPhysics.yawDelta(speed, steer, false, T));
        double radius = speed / omega;
        assertTrue(radius > 2.0 && radius < 3.0, "radio " + radius);
    }

    @Test
    void aAltaVelocidadLaDireccionSeEndurece() {
        double steerSlow = 0;
        double steerFast = 0;
        for (int i = 0; i < 20; i++) {
            steerSlow = CarPhysics.nextSteer(steerSlow, new CarInput(1, 1, false), 0.0, T);
            steerFast = CarPhysics.nextSteer(steerFast, new CarInput(1, 1, false), T.maxSpeed(), T);
        }
        assertEquals(T.maxSteerLow(), steerSlow, 1e-9);
        assertEquals(T.maxSteerHigh(), steerFast, 1e-9);
    }

    @Test
    void volanteSuaveYVuelveRapidoAlCentro() {
        double steer = CarPhysics.nextSteer(0, new CarInput(0, 1, false), 0, T);
        assertEquals(T.steerRate(), steer, 1e-9);
        double centered = CarPhysics.nextSteer(T.maxSteerLow(), CarInput.NONE, 0, T);
        assertTrue(T.maxSteerLow() - centered > T.steerRate(), "al soltar A/D el volante vuelve más rápido al centro");
    }

    @Test
    void enElAireAbsorbeVelocidadLateralParaPoderRedirigirRumbo() {
        double airLat = CarPhysics.nextLateral(0.4, CarInput.NONE, false, false, CarTuning.ENDURO);
        assertTrue(airLat < 0.4 * CarTuning.ENDURO.airDrag(), "en el aire absorbe parte del derrape para girar");
        assertTrue(airLat > CarPhysics.nextLateral(0.4, CarInput.NONE, true, false, CarTuning.ENDURO));
    }

    @Test
    void enReversaElGiroSeInvierte() {
        assertTrue(CarPhysics.yawDelta(0.2, 0.3, false, T) > 0);
        assertTrue(CarPhysics.yawDelta(-0.2, 0.3, false, T) < 0);
    }

    @Test
    void derrapeGiraMasYConservaVelocidadLateral() {
        assertTrue(CarPhysics.yawDelta(0.4, 0.2, true, T) > CarPhysics.yawDelta(0.4, 0.2, false, T));
        double grip = CarPhysics.nextLateral(0.3, CarInput.NONE, true, false, T);
        double drift = CarPhysics.nextLateral(0.3, HANDBRAKE, true, false, T);
        assertTrue(drift > grip);
        assertEquals(0.0, CarPhysics.nextLateral(0.002, CarInput.NONE, true, false, T));
    }

    @Test
    void elAnguloDeLasRuedasSeRecuperaDelMovimiento() {
        double speed = 0.3;
        double steer = 0.25;
        float yaw = CarPhysics.yawDelta(speed, steer, false, T);
        assertEquals(steer, CarPhysics.steerFromMotion(yaw, speed, T), 1e-4);
        assertTrue(Double.isNaN(CarPhysics.steerFromMotion(5, 0, T)));
    }

    @Test
    void validacionDeParametros() {
        assertNull(T.firstInvalid());
        assertNull(CarTuning.ENDURO.firstInvalid());
        assertNull(CarTuning.MOTOCARRO.firstInvalid());
        assertTrue(CarTuning.ENDURO.maxSpeed() > T.maxSpeed());
        assertTrue(CarTuning.ENDURO.accel() > T.accel());
        assertTrue(CarTuning.MOTOCARRO.maxSpeed() > T.maxSpeed());
        assertTrue(CarTuning.MOTOCARRO.maxSpeed() < CarTuning.ENDURO.maxSpeed());
        CarTuning broken = new CarTuning(0, 0.2, 0.03, 0.02, 0.04, 0.035, 0.98, 0.995, 0.85, 1.75, 0.6, 0.2, 0.1,
                0.3, 0.92, 1.6);
        assertNotNull(broken.firstInvalid());
        CarTuning badSteer = new CarTuning(0.6, 0.2, 0.03, 0.02, 0.04, 0.035, 0.98, 0.995, 0.85, 1.75, 2.0, 0.2, 0.1,
                0.3, 0.92, 1.6);
        assertNotNull(badSteer.firstInvalid());
    }
}
