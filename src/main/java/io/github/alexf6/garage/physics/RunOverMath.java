package io.github.alexf6.garage.physics;

/**
 * Cuentas del atropello, sin dependencias de Minecraft. La velocidad sale de lo que el vehículo se movió entre dos
 * ticks, y esa posición la manda el cliente del conductor: se acota a la velocidad máxima del manejo para que un
 * cliente alterado no consiga más daño ni empuje del que permite el vehículo. Coste: O(1).
 */
public final class RunOverMath {

    /**
     * @param speed    velocidad efectiva (bloques/tick), nunca mayor que la máxima del vehículo
     * @param strength fuerza del empuje: {@code knockback} escalado por la fracción de velocidad máxima
     * @param damage   daño por criatura; 0 si no llega al umbral de daño
     */
    public record Impact(double speed, double strength, float damage) {
        /** El vehículo va demasiado despacio (o está quieto): no empuja ni lastima. */
        public static final Impact NONE = new Impact(0.0, 0.0, 0.0F);

        public boolean isNone() {
            return speed <= 0.0;
        }
    }

    /**
     * @param distance      desplazamiento horizontal medido en el tick (bloques)
     * @param maxSpeed      velocidad máxima del manejo del servidor (bloques/tick)
     * @param pushSpeed     velocidad desde la que empuja
     * @param damageSpeed   velocidad desde la que además lastima
     * @param damagePerSpeed daño por cada bloque/tick por encima de {@code damageSpeed}
     * @param knockback     fuerza del empuje a velocidad máxima
     */
    public static Impact impact(double distance, double maxSpeed, double pushSpeed, double damageSpeed,
                                double damagePerSpeed, double knockback) {
        if (!(distance > 0.0) || !(maxSpeed > 0.0)) return Impact.NONE;
        double speed = Math.min(distance, maxSpeed);
        if (speed < pushSpeed) return Impact.NONE;
        double strength = knockback * VehicleMath.speedRatio(speed, maxSpeed);
        float damage = (float) Math.max((speed - damageSpeed) * damagePerSpeed, 0.0);
        return new Impact(speed, strength, damage);
    }

    private RunOverMath() {}
}
