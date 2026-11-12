package io.github.alexf6.garage.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RunOverMathTest {

    private static final double MAX = 0.6;
    private static final double PUSH = 0.1;
    private static final double DAMAGE_FROM = 0.25;
    private static final double PER_SPEED = 25.0;
    private static final double KNOCKBACK = 1.2;

    private static final RunOverRules RULES = rules(PUSH, DAMAGE_FROM, PER_SPEED);

    private static RunOverRules rules(double push, double damageFrom, double perSpeed) {
        return new RunOverRules(true, push, damageFrom, perSpeed, KNOCKBACK, false);
    }

    private static RunOverMath.Impact at(double distance) {
        return RunOverMath.impact(distance, MAX, RULES);
    }

    @Test
    void quietoOCasiQuietoNoEmpuja() {
        assertSame(RunOverMath.Impact.NONE, at(0.0));
        assertSame(RunOverMath.Impact.NONE, at(-1.0));
        assertSame(RunOverMath.Impact.NONE, at(PUSH - 0.001));
        assertTrue(at(0.0).isNone());
    }

    @Test
    void entreElUmbralDeEmpujeYElDeDanoEmpujaSinLastimar() {
        RunOverMath.Impact impact = at(0.2);
        assertEquals(0.2, impact.speed(), 1e-9);
        assertEquals(0.0F, impact.damage());
        assertEquals(KNOCKBACK * (0.2 / MAX), impact.strength(), 1e-9);
    }

    @Test
    void porEncimaDelUmbralDeDanoElDanoCreceConLaVelocidad() {
        assertEquals((float) ((0.5 - DAMAGE_FROM) * PER_SPEED), at(0.5).damage(), 1e-6F);
        assertTrue(at(0.5).damage() > at(0.3).damage());
    }

    @Test
    void unDesplazamientoImposibleSeAcotaALaVelocidadMaxima() {
        RunOverMath.Impact capped = at(500.0);
        assertEquals(MAX, capped.speed(), 1e-9);
        assertEquals(KNOCKBACK, capped.strength(), 1e-9);
        assertEquals((float) ((MAX - DAMAGE_FROM) * PER_SPEED), capped.damage(), 1e-6F);
        assertEquals(at(MAX).damage(), capped.damage());
    }

    @Test
    void conUmbralDeEmpujeEnCeroUnVehiculoParadoSigueSinEmpujar() {
        assertSame(RunOverMath.Impact.NONE, RunOverMath.impact(0.0, MAX, rules(0.0, DAMAGE_FROM, PER_SPEED)));
        assertEquals(0.01, RunOverMath.impact(0.01, MAX, rules(0.0, DAMAGE_FROM, PER_SPEED)).speed(), 1e-9);
    }

    @Test
    void valoresNoNumericosNoEmpujan() {
        assertSame(RunOverMath.Impact.NONE, at(Double.NaN));
        assertSame(RunOverMath.Impact.NONE,
                RunOverMath.impact(0.3, Double.NaN, RULES));
        assertSame(RunOverMath.Impact.NONE, RunOverMath.impact(0.3, 0.0, RULES));
    }

    @Test
    void sinDanoConfiguradoNuncaHayDano() {
        assertEquals(0.0F, RunOverMath.impact(0.6, MAX, rules(PUSH, DAMAGE_FROM, 0.0)).damage());
    }
}
