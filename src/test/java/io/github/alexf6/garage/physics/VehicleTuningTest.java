package io.github.alexf6.garage.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VehicleTuningTest {

    private static final VehicleTuning BASE = VehicleTuning.WILLYS;

    @Test
    void losPerfilesDelModSonValidos() {
        assertNull(VehicleTuning.WILLYS.firstInvalid());
        assertNull(VehicleTuning.ENDURO.firstInvalid());
        assertNull(VehicleTuning.MOTOCARRO.firstInvalid());
    }

    @Test
    void velocidadesFueraDeRangoSonInvalidas() {
        assertEquals("maxSpeed/maxReverse", with(0, BASE.maxReverse()).firstInvalid());
        assertEquals("maxSpeed/maxReverse", with(-1, BASE.maxReverse()).firstInvalid());
        assertEquals("maxSpeed/maxReverse", with(VehicleTuning.SPEED_LIMIT + 0.01, BASE.maxReverse()).firstInvalid());
        assertEquals("maxSpeed/maxReverse", with(BASE.maxSpeed(), 1.0e9).firstInvalid());
        assertNull(with(VehicleTuning.SPEED_LIMIT, VehicleTuning.SPEED_LIMIT).firstInvalid());
    }

    @Test
    void nanEsInvalido() {
        assertNotNull(with(Double.NaN, BASE.maxReverse()).firstInvalid());
        assertNotNull(with(BASE.maxSpeed(), Double.NaN).firstInvalid());
    }

    @Test
    void distanciaEntreEjesYAceleracionTienenTope() {
        VehicleTuning longWheelBase = new VehicleTuning(BASE.maxSpeed(), BASE.maxReverse(), BASE.accel(), BASE.reverseAccel(),
                BASE.brake(), BASE.handbrake(), BASE.rollingFriction(), BASE.airDrag(), BASE.waterDrag(),
                VehicleTuning.WHEEL_BASE_LIMIT + 1, BASE.maxSteerLow(), BASE.maxSteerHigh(), BASE.steerRate(),
                BASE.grip(), BASE.driftGrip(), BASE.driftYawBoost());
        assertEquals("wheelBase", longWheelBase.firstInvalid());
        VehicleTuning strongEngine = new VehicleTuning(BASE.maxSpeed(), BASE.maxReverse(), 50.0, BASE.reverseAccel(),
                BASE.brake(), BASE.handbrake(), BASE.rollingFriction(), BASE.airDrag(), BASE.waterDrag(),
                BASE.wheelBase(), BASE.maxSteerLow(), BASE.maxSteerHigh(), BASE.steerRate(),
                BASE.grip(), BASE.driftGrip(), BASE.driftYawBoost());
        assertEquals("accel/brake", strongEngine.firstInvalid());
    }

    @Test
    void toArrayYFromArrayDanLaMismaCopia() {
        for (VehicleTuning tuning : new VehicleTuning[] {VehicleTuning.WILLYS, VehicleTuning.ENDURO,
                VehicleTuning.MOTOCARRO}) {
            double[] values = tuning.toArray();
            assertEquals(VehicleTuning.FIELD_COUNT, values.length);
            assertEquals(tuning, VehicleTuning.fromArray(values));
        }
    }

    @Test
    void toArrayRespetaElOrdenDeDeclaracion() {
        double[] v = VehicleTuning.WILLYS.toArray();
        assertEquals(VehicleTuning.WILLYS.maxSpeed(), v[0]);
        assertEquals(VehicleTuning.WILLYS.wheelBase(), v[9]);
        assertEquals(VehicleTuning.WILLYS.driftYawBoost(), v[15]);
    }

    @Test
    void fromArrayConLongitudIncorrectaFalla() {
        assertThrows(IllegalArgumentException.class, () -> VehicleTuning.fromArray(new double[3]));
    }

    private static VehicleTuning with(double maxSpeed, double maxReverse) {
        return new VehicleTuning(maxSpeed, maxReverse, BASE.accel(), BASE.reverseAccel(), BASE.brake(), BASE.handbrake(),
                BASE.rollingFriction(), BASE.airDrag(), BASE.waterDrag(), BASE.wheelBase(), BASE.maxSteerLow(),
                BASE.maxSteerHigh(), BASE.steerRate(), BASE.grip(), BASE.driftGrip(), BASE.driftYawBoost());
    }
}
