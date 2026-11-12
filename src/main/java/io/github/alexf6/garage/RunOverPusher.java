package io.github.alexf6.garage;

import io.github.alexf6.garage.config.GarageConfig;

import io.github.alexf6.garage.physics.RunOverMath;
import io.github.alexf6.garage.physics.RunOverRules;
import io.github.alexf6.garage.physics.VehicleMath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Aparta (y desde cierta velocidad lastima) a las criaturas que el vehículo tiene delante. Va en el servidor
 * porque el daño lo decide él; sin esto el vehículo choca contra ellas como contra una pared.
 *
 * <p>La velocidad sale de cuánto se movió el vehículo entre dos ticks, y esa posición la manda el cliente del
 * conductor: se acota a la velocidad máxima del {@code tuning} del servidor para que un cliente alterado no
 * consiga daño, empuje ni zonas de búsqueda mayores de lo que permite el manejo. Coste: O(entidades en la zona).
 */
final class RunOverPusher {

    private double lastX;
    private double lastZ;
    private boolean hasLast;

    void tick(ServerLevel level, MotorVehicleEntity vehicle, LivingEntity driver) {
        double x = vehicle.getX();
        double z = vehicle.getZ();
        double dx = hasLast ? x - lastX : 0.0;
        double dz = hasLast ? z - lastZ : 0.0;
        lastX = x;
        lastZ = z;
        hasLast = true;

        // Sin objetos nuevos mientras está quieto: solo se crean cuando hay a quién apartar
        RunOverRules rules = GarageConfig.get().runOver();
        double distance = Math.hypot(dx, dz);
        if (!rules.enabled() || distance <= 0.0) return;
        RunOverMath.Impact impact = RunOverMath.impact(distance, vehicle.tuning().maxSpeed(), rules);
        if (impact.isNone()) return;

        double dirX = dx / distance;
        double dirZ = dz / distance;
        float damage = impact.damage();
        AABB zone = vehicle.getBoundingBox()
                .expandTowards(dirX * impact.speed(), 0.0, dirZ * impact.speed()).inflate(0.1, 0.0, 0.1);
        DamageSource source = vehicle.damageSources().source(DamageTypes.MOB_ATTACK, vehicle, driver);

        for (Entity entity : level.getEntities(vehicle, zone, RunOverPusher::canBeRunOver)) {
            LivingEntity victim = (LivingEntity) entity;
            boolean hurts = damage > 0 && (rules.hurtPlayers() || !(victim instanceof Player));
            if (hurts) victim.hurtServer(level, source, damage);
            VehicleMath.Planar push = VehicleMath.pushDirection(
                    dirX, dirZ, victim.getX() - x, victim.getZ() - z);
            victim.knockback(impact.strength(), -push.x(), -push.z(), source, hurts ? damage : 0.0F);
        }
    }

    private static boolean canBeRunOver(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()
                || living.isInvulnerable() || living.getVehicle() != null) {
            return false;
        }
        return !(living instanceof Player player && player.isCreative());
    }
}
