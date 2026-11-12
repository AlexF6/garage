package io.github.alexf6.garage;

import io.github.alexf6.garage.physics.VehicleMath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Aparta (y desde cierta velocidad lastima) a las criaturas que el vehículo tiene delante. Va en el servidor
 * porque el daño lo decide él; sin esto el vehículo choca contra ellas como contra una pared.
 *
 * <p>La velocidad sale de cuánto se movió el vehículo entre dos ticks, y esa posición la manda el cliente del
 * conductor: se acota a la velocidad máxima del {@code tuning} del servidor para que un cliente alterado no
 * consiga daño, empuje ni zonas de búsqueda mayores de lo que permite el manejo. Coste: O(entidades en la zona).
 */
final class RunOverPusher {

    private Vec3 lastPos;

    void tick(ServerLevel level, MotorVehicleEntity vehicle, LivingEntity driver) {
        Vec3 pos = vehicle.position();
        Vec3 motion = lastPos == null ? Vec3.ZERO : pos.subtract(lastPos);
        lastPos = pos;

        GarageConfig.RunOver config = GarageConfig.get().runOver();
        double maxSpeed = vehicle.tuning().maxSpeed();
        double distance = motion.horizontalDistance();
        double speed = Math.min(distance, maxSpeed);
        if (!config.enabled() || distance <= 0.0 || speed < config.pushSpeed()) return;

        double dirX = motion.x / distance;
        double dirZ = motion.z / distance;
        double strength = config.knockback() * VehicleMath.speedRatio(speed, maxSpeed);
        float damage = (float) ((speed - config.damageSpeed()) * config.damagePerSpeed());
        AABB zone = vehicle.getBoundingBox().expandTowards(dirX * speed, 0.0, dirZ * speed).inflate(0.1, 0.0, 0.1);
        DamageSource source = vehicle.damageSources().source(DamageTypes.MOB_ATTACK, vehicle, driver);

        for (Entity entity : level.getEntities(vehicle, zone, RunOverPusher::canBeRunOver)) {
            LivingEntity victim = (LivingEntity) entity;
            boolean hurts = damage > 0 && (config.hurtPlayers() || !(victim instanceof Player));
            if (hurts) victim.hurtServer(level, source, damage);
            VehicleMath.Planar push = VehicleMath.pushDirection(
                    dirX, dirZ, victim.getX() - pos.x, victim.getZ() - pos.z);
            victim.knockback(strength, -push.x(), -push.z(), source, hurts ? damage : 0.0F);
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
