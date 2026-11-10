package io.github.alexf6.garage.physics;

/**
 * Modelo de manejo tipo "bicicleta" (Ackermann simplificado): funciones puras, O(1) por tick y sin
 * dependencias de Minecraft, para poder probarlas y reutilizarlas en otros vehículos.
 */
public final class CarPhysics {

    private static final double STOP_EPSILON = 0.003;

    /** Nueva velocidad longitudinal (signo = sentido respecto al frente del carro). */
    public static double nextSpeed(double speed, CarInput input, boolean fuelled, boolean onGround,
                                   boolean inWater, CarTuning t) {
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
     * Ángulo de dirección: se acerca al objetivo a {@code steerRate} por tick (volante suave) y el máximo
     * baja con la velocidad, para maniobrar cerrado al parquear sin volverse inestable en recta.
     */
    public static double nextSteer(double steer, CarInput input, double speed, CarTuning t) {
        double ratio = Math.min(Math.abs(speed) / t.maxSpeed(), 1.0);
        double maxSteer = t.maxSteerLow() + (t.maxSteerHigh() - t.maxSteerLow()) * ratio;
        double target = input.steering() * maxSteer;
        return steer + clamp(target - steer, -t.steerRate(), t.steerRate());
    }

    /**
     * Velocidad lateral (derrape) que queda tras un tick. Las llantas la absorben según el agarre; con el
     * freno de mano casi no la absorben y el carro sigue deslizándose hacia donde iba.
     */
    public static double nextLateral(double lateral, CarInput input, boolean onGround, boolean inWater, CarTuning t) {
        double v;
        if (inWater) v = lateral * t.waterDrag();
        else if (!onGround) v = lateral * t.airDrag();
        else v = lateral * (input.brake() ? t.driftGrip() : t.grip());
        return Math.abs(v) < STOP_EPSILON ? 0.0 : v;
    }

    /**
     * Grados que gira el carro en un tick: ω = v · tan(δ) / L. En reversa el giro se invierte solo.
     * Derrapando, la cola se suelta y gira más ({@code driftYawBoost}).
     */
    public static float yawDelta(double speed, double steer, boolean drifting, CarTuning t) {
        double omega = speed * Math.tan(steer) / t.wheelBase();
        return (float) Math.toDegrees(drifting ? omega * t.driftYawBoost() : omega);
    }

    /** Ángulo de dirección que explica un giro observado (inversa de {@link #yawDelta}); para animar ruedas. */
    public static double steerFromMotion(double yawDeltaDeg, double forward, CarTuning t) {
        if (Math.abs(forward) < 1.0e-3) return Double.NaN;
        return Math.atan(Math.toRadians(yawDeltaDeg) * t.wheelBase() / forward);
    }

    static double towardZero(double value, double amount) {
        return value > 0 ? Math.max(0.0, value - amount) : Math.min(0.0, value + amount);
    }

    static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    private CarPhysics() {}
}
