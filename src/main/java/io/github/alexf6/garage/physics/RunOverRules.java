package io.github.alexf6.garage.physics;

/**
 * Reglas del atropello y el empuje de criaturas, en bloques/tick (0.1 b/t = 7.2 km/h). Es la sección
 * {@code runOver} de la config; vive aquí para que {@link RunOverMath} las reciba juntas.
 *
 * @param enabled        si el vehículo empuja y atropella
 * @param pushSpeed      desde esta velocidad el vehículo aparta a las criaturas que tiene delante
 * @param damageSpeed    desde esta velocidad además las lastima
 * @param damagePerSpeed daño por cada bloque/tick por encima de {@code damageSpeed}
 * @param knockback      fuerza del empuje a velocidad máxima
 * @param hurtPlayers    si también lastima a otros jugadores (siempre los empuja)
 */
public record RunOverRules(boolean enabled, double pushSpeed, double damageSpeed, double damagePerSpeed,
                           double knockback, boolean hurtPlayers) {}
