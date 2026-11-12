package io.github.alexf6.garage.physics;

/**
 * Modelo de manejo tipo "bicicleta" (Ackermann simplificado): funciones puras, O(1) por tick y sin
 * dependencias de Minecraft, para poder probarlas y reutilizarlas en otros vehículos.
 */
public final class VehiclePhysics {

    private static final double STOP_EPSILON = 0.003;

    /** Nueva velocidad longitudinal (signo = sentido respecto al frente del carro). */
    public static double nextSpeed(double speed, VehicleInput input, boolean fuelled, boolean onGround,
                                   boolean inWater, VehicleTuning t) {
        double v = speed;
        double friction = onGround ? t.rollingFriction() : t.airDrag();

        if (inWater) {
            v *= t.waterDrag(); // sin tracción en el agua
        } else if (input.brake()) {
            v = towardZero(v, t.handbrake());
        } else if (input.throttle() > 0) {
            if (v < 0) v = towardZero(v, t.brake());
            else v = fuelled ? v + t.accel() * (1.0 - v / t.maxSpeed()) : v * friction;
        } else if (input.throttle() < 0) {
            if (v > 0) v = towardZero(v, t.brake());
            else v = fuelled ? v - t.reverseAccel() * (1.0 + v / t.maxReverse()) : v * friction;
        } else {
            v *= friction;
        }

        v = clamp(v, -t.maxReverse(), t.maxSpeed());
        return Math.abs(v) < STOP_EPSILON ? 0.0 : v;
    }

    /**
     * Ángulo de dirección: se acerca al objetivo a {@code steerRate} por tick (y vuelve al centro más rápido
     * al soltar las teclas) y el máximo baja con la velocidad, para maniobrar cerrado sin perder estabilidad.
     */
    public static double nextSteer(double steer, VehicleInput input, double speed, VehicleTuning t) {
        double ratio = Math.min(Math.abs(speed) / t.maxSpeed(), 1.0);
        double maxSteer = t.maxSteerLow() + (t.maxSteerHigh() - t.maxSteerLow()) * ratio;
        double target = input.steering() * maxSteer;
        double rate = input.steering() == 0 ? t.steerRate() * 1.8 : t.steerRate();
        return steer + clamp(target - steer, -rate, rate);
    }

    /**
     * Velocidad lateral (derrape) que queda tras un tick. Las llantas la absorben según el agarre; en el aire
     * conserva una fracción intermedia entre {@code grip} y {@code airDrag} para poder redirigir el rumbo en saltos
     * y bajadas de bloques.
     */
    public static double nextLateral(double lateral, VehicleInput input, boolean onGround, boolean inWater, VehicleTuning t) {
        double v;
        if (inWater) v = lateral * t.waterDrag();
        else if (!onGround) v = lateral * ((t.grip() + t.airDrag()) * 0.5);
        else v = lateral * (input.brake() ? t.driftGrip() : t.grip());
        return Math.abs(v) < STOP_EPSILON ? 0.0 : v;
    }

    /**
     * Grados que gira el carro en un tick: ω = v · tan(δ) / L. En reversa el giro se invierte solo.
     * Derrapando, la cola se suelta y gira más ({@code driftYawBoost}).
     */
    public static float yawDelta(double speed, double steer, boolean drifting, VehicleTuning t) {
        double omega = speed * Math.tan(steer) / t.wheelBase();
        return (float) Math.toDegrees(drifting ? omega * t.driftYawBoost() : omega);
    }

    /** Ángulo de dirección que explica un giro observado (inversa de {@link #yawDelta}); para animar ruedas. */
    public static double steerFromMotion(double yawDeltaDeg, double forward, VehicleTuning t) {
        if (Math.abs(forward) < 1.0e-3) return Double.NaN;
        return Math.atan(Math.toRadians(yawDeltaDeg) * t.wheelBase() / forward);
    }

    static double towardZero(double value, double amount) {
        return value > 0 ? Math.max(0.0, value - amount) : Math.min(0.0, value + amount);
    }

    static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    private VehiclePhysics() {}
}
