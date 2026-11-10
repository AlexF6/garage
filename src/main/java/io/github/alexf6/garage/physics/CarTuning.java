package io.github.alexf6.garage.physics;

/**
 * Parámetros de manejo de un vehículo. Velocidades en bloques/tick (1 b/t = 72 km/h), ángulos en radianes,
 * fricciones y agarres como factor que se conserva por tick (1 = no frena, 0 = frena en seco).
 * Un vehículo nuevo solo necesita otra instancia, sin tocar {@link CarPhysics}.
 *
 * @param grip          velocidad lateral que se conserva por tick con agarre normal
 * @param driftGrip     velocidad lateral que se conserva con el freno de mano (derrape)
 * @param driftYawBoost multiplica el giro mientras se derrapa: la cola se suelta
 */
public record CarTuning(
        double maxSpeed,
        double maxReverse,
        double accel,
        double reverseAccel,
        double brake,
        double handbrake,
        double rollingFriction,
        double airDrag,
        double waterDrag,
        double wheelBase,
        double maxSteerLow,
        double maxSteerHigh,
        double steerRate,
        double grip,
        double driftGrip,
        double driftYawBoost
) {
    /** Jeep Willys: ~43 km/h, buena maniobra a baja velocidad, dirección más firme a alta. */
    public static final CarTuning WILLYS = new CarTuning(
            0.6, 0.2,
            0.03, 0.02,
            0.04, 0.035,
            0.98, 0.995, 0.85,
            1.75,
            0.6, 0.2,
            0.1,
            0.3, 0.92, 1.6
    );

    /** Primer parámetro fuera de rango, o {@code null} si todos son válidos. */
    public String firstInvalid() {
        if (!(maxSpeed > 0) || !(maxReverse > 0)) return "maxSpeed/maxReverse";
        if (!(accel > 0) || !(reverseAccel > 0) || brake < 0 || handbrake < 0) return "accel/brake";
        if (!isFactor(rollingFriction) || !isFactor(airDrag) || !isFactor(waterDrag)) return "friction";
        if (!(wheelBase > 0)) return "wheelBase";
        if (maxSteerLow < 0 || maxSteerHigh < 0 || maxSteerLow >= Math.PI / 2 || maxSteerHigh >= Math.PI / 2) {
            return "maxSteer";
        }
        if (!(steerRate > 0)) return "steerRate";
        if (!isFactor(grip) || !isFactor(driftGrip) || !(driftYawBoost > 0)) return "grip/drift";
        return null;
    }

    private static boolean isFactor(double value) {
        return value >= 0 && value <= 1;
    }
}
