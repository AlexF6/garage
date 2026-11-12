package io.github.alexf6.garage.physics;

/**
 * Parámetros de manejo de un vehículo. Velocidades en bloques/tick (1 b/t = 72 km/h), ángulos en radianes,
 * fricciones y agarres como factor que se conserva por tick (1 = no frena, 0 = frena en seco).
 * Un vehículo nuevo solo necesita otra instancia (más su entrada en {@code VehicleSpecs}), sin tocar {@link VehiclePhysics}.
 *
 * @param grip          velocidad lateral que se conserva por tick con agarre normal
 * @param driftGrip     velocidad lateral que se conserva con el freno de mano (derrape)
 * @param driftYawBoost multiplica el giro mientras se derrapa: la cola se suelta
 */
public record VehicleTuning(
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
    public static final VehicleTuning WILLYS = new VehicleTuning(
            0.6, 0.2,
            0.03, 0.02,
            0.04, 0.035,
            0.98, 0.995, 0.85,
            1.75,
            0.6, 0.2,
            0.1,
            0.3, 0.92, 1.6
    );

    /** Moto enduro/dual-sport (Yamaha XTZ 125): ligera (~118 kg), ~68 km/h (0.92 b/t), alto torque y giro cerrado. */
    public static final VehicleTuning ENDURO = new VehicleTuning(
            0.92, 0.18,
            0.050, 0.02,
            0.055, 0.05,
            0.985, 0.992, 0.70,
            1.25,
            0.78, 0.36,
            0.24,
            0.16, 0.88, 1.8
    );

    /** Motocarro urbano (Bajaj RE / "Torito"): ligero-medio, ~52 km/h (0.72 b/t), radio cerrado. */
    public static final VehicleTuning MOTOCARRO = new VehicleTuning(
            0.72, 0.18,
            0.038, 0.018,
            0.045, 0.040,
            0.982, 0.993, 0.75,
            1.50,
            0.72, 0.28,
            0.18,
            0.22, 0.90, 1.7
    );

    /** Tope de velocidad (bloques/tick; 3 = 216 km/h): más rápido el vehículo se sale de los chunks cargados. */
    public static final double SPEED_LIMIT = 3.0;
    /** Tope de las aceleraciones y frenadas (bloques/tick por tick). */
    public static final double ACCEL_LIMIT = 1.0;
    /** Tope de la distancia entre ejes (bloques). */
    public static final double WHEEL_BASE_LIMIT = 16.0;
    /** Tope del ritmo del volante (radianes/tick) y del impulso de giro al derrapar. */
    public static final double STEER_RATE_LIMIT = 1.0;
    public static final double DRIFT_YAW_BOOST_LIMIT = 10.0;

    /** Primer parámetro fuera de rango, o {@code null} si todos son válidos (NaN cuenta como inválido). */
    public String firstInvalid() {
        if (!inRange(maxSpeed, SPEED_LIMIT) || !inRange(maxReverse, SPEED_LIMIT)) return "maxSpeed/maxReverse";
        if (!inRange(accel, ACCEL_LIMIT) || !inRange(reverseAccel, ACCEL_LIMIT)
                || !(brake >= 0 && brake <= ACCEL_LIMIT) || !(handbrake >= 0 && handbrake <= ACCEL_LIMIT)) {
            return "accel/brake";
        }
        if (!isFactor(rollingFriction) || !isFactor(airDrag) || !isFactor(waterDrag)) return "friction";
        if (!inRange(wheelBase, WHEEL_BASE_LIMIT)) return "wheelBase";
        if (!(maxSteerLow >= 0 && maxSteerLow < Math.PI / 2) || !(maxSteerHigh >= 0 && maxSteerHigh < Math.PI / 2)) {
            return "maxSteer";
        }
        if (!inRange(steerRate, STEER_RATE_LIMIT)) return "steerRate";
        if (!isFactor(grip) || !isFactor(driftGrip) || !inRange(driftYawBoost, DRIFT_YAW_BOOST_LIMIT)) {
            return "grip/drift";
        }
        return null;
    }

    /** Positivo y no mayor que {@code limit}; falso con NaN. */
    private static boolean inRange(double value, double limit) {
        return value > 0 && value <= limit;
    }

    private static boolean isFactor(double value) {
        return value >= 0 && value <= 1;
    }
}
