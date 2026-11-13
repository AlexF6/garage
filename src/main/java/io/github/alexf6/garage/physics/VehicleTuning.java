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
            0.6, 0.2,  // maxSpeed, maxReverse
            0.03, 0.02,  // accel, reverseAccel
            0.04, 0.035,  // brake, handbrake
            0.98, 0.995, 0.85,  // rollingFriction, airDrag, waterDrag
            1.75,  // wheelBase
            0.6, 0.2,  // maxSteerLow, maxSteerHigh
            0.1,  // steerRate
            0.3, 0.92, 1.6  // grip, driftGrip, driftYawBoost
    );

    /** Moto enduro/dual-sport (Yamaha XTZ 125): ligera (~118 kg), ~68 km/h (0.92 b/t), alto torque y giro cerrado. */
    public static final VehicleTuning ENDURO = new VehicleTuning(
            0.92, 0.18,  // maxSpeed, maxReverse
            0.050, 0.02,  // accel, reverseAccel
            0.055, 0.05,  // brake, handbrake
            0.985, 0.992, 0.70,  // rollingFriction, airDrag, waterDrag
            1.25,  // wheelBase
            0.78, 0.36,  // maxSteerLow, maxSteerHigh
            0.24,  // steerRate
            0.16, 0.88, 1.8  // grip, driftGrip, driftYawBoost
    );

    /** Motocarro urbano (Bajaj RE / "Torito"): ligero-medio, ~52 km/h (0.72 b/t), radio cerrado. */
    public static final VehicleTuning MOTOCARRO = new VehicleTuning(
            0.72, 0.18,  // maxSpeed, maxReverse
            0.038, 0.018,  // accel, reverseAccel
            0.045, 0.040,  // brake, handbrake
            0.982, 0.993, 0.75,  // rollingFriction, airDrag, waterDrag
            1.50,  // wheelBase
            0.72, 0.28,  // maxSteerLow, maxSteerHigh
            0.18,  // steerRate
            0.22, 0.90, 1.7  // grip, driftGrip, driftYawBoost
    );

    /** Trimoto de carga / Motocarguero 200cc: pesado con platón metálico, ~47 km/h (0.65 b/t), alto torque y tracción posterior. */
    public static final VehicleTuning MOTOCARGUERO = new VehicleTuning(
            0.65, 0.18,  // maxSpeed, maxReverse
            0.042, 0.018,  // accel, reverseAccel
            0.045, 0.040,  // brake, handbrake
            0.982, 0.993, 0.75,  // rollingFriction, airDrag, waterDrag
            1.70,  // wheelBase
            0.70, 0.25,  // maxSteerLow, maxSteerHigh
            0.16,  // steerRate
            0.26, 0.88, 1.6  // grip, driftGrip, driftYawBoost
    );

    /** Número de parámetros de {@link #toArray()}. */
    public static final int FIELD_COUNT = 16;

    /** Los parámetros en el orden de declaración, para serializarlos sin repetir cada campo a mano. */
    public double[] toArray() {
        return new double[] {maxSpeed, maxReverse, accel, reverseAccel, brake, handbrake, rollingFriction, airDrag,
                waterDrag, wheelBase, maxSteerLow, maxSteerHigh, steerRate, grip, driftGrip, driftYawBoost};
    }

    /** Inversa de {@link #toArray()}; {@code values} debe tener {@link #FIELD_COUNT} elementos. */
    public static VehicleTuning fromArray(double[] values) {
        if (values.length != FIELD_COUNT) {
            throw new IllegalArgumentException("se esperaban " + FIELD_COUNT + " valores y llegaron " + values.length);
        }
        return new VehicleTuning(values[0], values[1], values[2], values[3], values[4], values[5], values[6],
                values[7], values[8], values[9], values[10], values[11], values[12], values[13], values[14],
                values[15]);
    }

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
