package io.github.alexf6.garage.physics;

/**
 * Cálculos puros de la entidad del vehículo (sin dependencias de Minecraft, para poder probarlos).
 * Todas las funciones son O(1). Unidades: desplazamientos en bloques, velocidades en bloques/tick,
 * ángulos en grados salvo que se indique radianes.
 */
public final class VehicleMath {

    /** Vuelta completa en radianes. */
    public static final float TWO_PI = (float) (Math.PI * 2.0);
    /** Fracción del giro que se conserva en el aire: sin suelo, las ruedas ya no dirigen. */
    public static final float AIR_YAW_RETAIN = 0.75F;
    /** Por debajo de esta velocidad (bloques/tick) el vehículo gira "en el sitio" con A/D. */
    public static final double SLOW_SPEED = 0.08;
    /** Cuánto se desvía hacia un lado el empuje al atropellar, respecto al rumbo (1 = a 45°). */
    public static final double SIDE_PUSH = 0.6;
    /** Factor por tick con el que la dirección visual vuelve al centro cuando el vehículo está detenido. */
    public static final float STEER_RECENTER = 0.8F;
    /** Fracción del camino al objetivo que recorre la dirección visual en cada tick. */
    public static final float STEER_SMOOTHING = 0.5F;

    /** Punto o vector sobre el plano horizontal (X, Z). */
    public record Planar(double x, double z) {}

    /** Componente de un desplazamiento horizontal en la dirección a la que mira el vehículo. */
    public static double forwardOf(double dx, double dz, float yawDeg) {
        double yawRad = Math.toRadians(yawDeg);
        return -dx * Math.sin(yawRad) + dz * Math.cos(yawRad);
    }

    /**
     * Giro (grados) de un tick con las teclas A/D aplicadas directamente al rumbo: en el aire se conserva
     * {@link #AIR_YAW_RETAIN} del giro de la física y se suma el giro directo; casi detenido y con una tecla
     * pulsada solo se suma el giro directo.
     *
     * @param yawStep  giro que calculó la física
     * @param speed    velocidad longitudinal (bloques/tick)
     * @param steering -1 izquierda, 0 recto, 1 derecha
     * @param turnRate grados por tick del giro directo
     */
    public static float directYaw(float yawStep, double speed, boolean onGround, int steering, float turnRate) {
        if (!onGround) return yawStep * AIR_YAW_RETAIN + steering * turnRate;
        if (Math.abs(speed) < SLOW_SPEED && steering != 0) return yawStep + steering * turnRate;
        return yawStep;
    }

    /**
     * Punto en el que un rayo que parte de {@code (fromX, fromZ)} (relativo al centro) sale de un cuadrado de
     * medio lado {@code reach} centrado en el origen. Con una dirección nula devuelve el punto de partida.
     */
    public static Planar exitPoint(double fromX, double fromZ, double dirX, double dirZ, double reach) {
        double t = Double.MAX_VALUE;
        if (Math.abs(dirX) > 1.0e-6) t = Math.min(t, (Math.signum(dirX) * reach - fromX) / dirX);
        if (Math.abs(dirZ) > 1.0e-6) t = Math.min(t, (Math.signum(dirZ) * reach - fromZ) / dirZ);
        if (t == Double.MAX_VALUE) return new Planar(fromX, fromZ);
        return new Planar(fromX + dirX * t, fromZ + dirZ * t);
    }

    /**
     * Dirección del empuje de un atropello: hacia adelante y hacia el lado en que ya estaba la víctima, para
     * que salga del camino y no quede debajo.
     *
     * @param dirX rumbo del vehículo (vector unitario)
     * @param offX posición de la víctima relativa al vehículo
     */
    public static Planar pushDirection(double dirX, double dirZ, double offX, double offZ) {
        double sideSign = Math.signum(offX * dirZ - offZ * dirX);
        return new Planar(dirX + dirZ * sideSign * SIDE_PUSH, dirZ - dirX * sideSign * SIDE_PUSH);
    }

    /**
     * Radianes (múltiplo de {@link #TWO_PI}) que hay que restar al rodado de las ruedas para que no crezca sin
     * límite y el {@code float} no pierda precisión. Restarlo a ambos valores (actual y previo) mantiene la
     * interpolación continua.
     */
    public static float fullTurns(float wheelRoll) {
        if (Math.abs(wheelRoll) <= TWO_PI) return 0.0F;
        return TWO_PI * (int) (wheelRoll / TWO_PI);
    }

    /**
     * Siguiente ángulo visual de la dirección (radianes).
     *
     * @param target   ángulo que explica el movimiento observado, o NaN si el vehículo está detenido
     * @param maxSteer límite del ángulo (radianes)
     */
    public static float nextVisualSteer(float steer, double target, double maxSteer) {
        double goal = Double.isNaN(target) ? steer * STEER_RECENTER : target;
        double clamped = Math.clamp(goal, -maxSteer, maxSteer);
        return steer + (float) (clamped - steer) * STEER_SMOOTHING;
    }

    /** Velocidad como fracción de la máxima, en [0, 1]. */
    public static double speedRatio(double speed, double maxSpeed) {
        return Math.min(Math.abs(speed) / maxSpeed, 1.0);
    }

    private VehicleMath() {}
}
