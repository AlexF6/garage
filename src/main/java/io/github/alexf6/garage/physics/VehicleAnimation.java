package io.github.alexf6.garage.physics;

/**
 * Estado de animación visual de un vehículo (solo se avanza en el cliente): rodado de las ruedas, giro de las
 * delanteras, inclinación lateral y suspensión. Todo sale del movimiento observado, así que se ve igual para todos
 * sin paquetes. Cada valor guarda también el del tick anterior para interpolar entre ticks. Ángulos en radianes;
 * O(1) por tick y sin dependencias de Minecraft.
 */
public final class VehicleAnimation {

    /** Fracción del camino a la inclinación objetivo que recorre cada tick. */
    private static final float LEAN_SMOOTHING = 0.3F;

    private float wheelRoll;
    private float wheelRollO;
    private float steer;
    private float steerO;
    private float lean;
    private float leanO;
    private final Suspension suspension = new Suspension();
    private final double wheelRadius;
    private final float leanPerSteer;

    /**
     * @param wheelRadius  radio de las ruedas (bloques, positivo); fija cuánto giran al avanzar
     * @param leanPerSteer inclinación por radián de dirección a velocidad máxima; 0 = no se inclina
     */
    public VehicleAnimation(double wheelRadius, float leanPerSteer) {
        if (!(wheelRadius > 0.0)) {
            throw new IllegalArgumentException("wheelRadius debe ser positivo: " + wheelRadius);
        }
        if (!(leanPerSteer >= 0.0F)) {
            throw new IllegalArgumentException("leanPerSteer no puede ser negativo: " + leanPerSteer);
        }
        this.wheelRadius = wheelRadius;
        this.leanPerSteer = leanPerSteer;
    }

    /**
     * Avanza un tick: rodado = distancia / radio; dirección = inversa del modelo de giro; suspensión según las
     * aceleraciones observadas; inclinación lateral proporcional al giro y a la velocidad.
     *
     * @param forward      velocidad longitudinal observada (bloques/tick)
     * @param longAccel    cambio de {@code forward} respecto al tick anterior
     * @param yawDeltaDeg  giro del tick (grados)
     * @param dy           cambio de altura del tick (bloques)
     * @param tuning       manejo del vehículo: da la velocidad máxima y el giro con que se normalizan la dirección
     *                     y la inclinación
     */
    public void tick(double forward, double longAccel, float yawDeltaDeg, double dy, VehicleTuning tuning) {
        suspension.tick(longAccel, forward * Math.toRadians(yawDeltaDeg), dy);

        wheelRollO = wheelRoll;
        wheelRoll += (float) (forward / wheelRadius);
        // Resta vueltas completas a ambos valores: la interpolación sigue continua y el float no pierde precisión
        float turns = VehicleMath.fullTurns(wheelRoll);
        wheelRoll -= turns;
        wheelRollO -= turns;

        steerO = steer;
        double target = VehiclePhysics.steerFromMotion(yawDeltaDeg, forward, tuning);
        steer = VehicleMath.nextVisualSteer(steer, target, tuning.maxSteerLow());

        if (leanPerSteer > 0.0F) {
            leanO = lean;
            float targetLean = steer * (float) VehicleMath.speedRatio(forward, tuning.maxSpeed()) * leanPerSteer;
            lean += (targetLean - lean) * LEAN_SMOOTHING;
        }
    }

    /** Rodado de las ruedas (radianes), interpolado entre el tick anterior y el actual. */
    public float wheelRoll(float partialTick) {
        return lerp(partialTick, wheelRollO, wheelRoll);
    }

    /** Ángulo visual de la dirección (radianes). */
    public float steer(float partialTick) {
        return lerp(partialTick, steerO, steer);
    }

    /** Inclinación lateral (radianes); 0 en los vehículos que no se inclinan. */
    public float lean(float partialTick) {
        return lerp(partialTick, leanO, lean);
    }

    public Suspension suspension() {
        return suspension;
    }

    private static float lerp(float t, float from, float to) {
        return from + t * (to - from);
    }
}
