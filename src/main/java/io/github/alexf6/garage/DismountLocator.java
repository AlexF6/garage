package io.github.alexf6.garage;

import io.github.alexf6.garage.physics.VehicleMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.phys.Vec3;

/**
 * Baja al pasajero por la puerta de su lado, a la altura de su asiento; si ese lado está tapado prueba
 * el otro. Se usa cuando el pasajero ya salió de la lista pero aún no se movió, así que su posición todavía
 * dice en qué asiento iba. Coste: O(1) (2 lados x 3 alturas x poses).
 */
final class DismountLocator {

    /** Por dónde baja el pasajero y con qué postura; quien lo llama es quien se la aplica. */
    record Exit(Vec3 spot, Pose pose) {}

    /** Punto libre por el que bajar, o {@code null} si ningún lado sirve (se usa entonces el comportamiento vanilla). */
    static Exit find(MotorVehicleEntity vehicle, LivingEntity passenger) {
        Vec3 center = vehicle.position();
        Vec3 forward = Vec3.directionFromRotation(0.0F, vehicle.getYRot());
        Vec3 left = new Vec3(forward.z, 0.0, -forward.x); // el +X del modelo (lado del conductor)
        Vec3 rel = passenger.position().subtract(center);
        Vec3 seat = center.add(forward.scale(rel.dot(forward)));
        double side = rel.dot(left) < 0 ? -1.0 : 1.0;
        // La caja de colisión no gira con el vehículo: hay que salir de ella entera, más medio jugador
        double reach = vehicle.getBbWidth() / 2.0 + passenger.getBbWidth() / 2.0 + 0.05;

        for (double s : new double[] {side, -side}) {
            Vec3 from = seat.subtract(center);
            Vec3 dir = left.scale(s);
            VehicleMath.Planar exit = VehicleMath.exitPoint(from.x, from.z, dir.x, dir.z, reach);
            Exit found = findSpot(vehicle, passenger, new Vec3(center.x + exit.x(), vehicle.getY(), center.z + exit.z()));
            if (found != null) return found;
        }
        return null;
    }

    /** Suelo válido y espacio libre en (x, z): al nivel del vehículo, un bloque arriba o uno abajo. */
    private static Exit findSpot(MotorVehicleEntity vehicle, LivingEntity passenger, Vec3 at) {
        BlockPos base = BlockPos.containing(at.x, vehicle.getY(), at.z);
        for (BlockPos pos : new BlockPos[] {base, base.above(), base.below()}) {
            double floor = vehicle.level().getBlockFloorHeight(pos);
            if (!DismountHelper.isBlockFloorValid(floor)) continue;
            Vec3 spot = new Vec3(at.x, pos.getY() + floor, at.z);
            for (Pose pose : passenger.getDismountPoses()) {
                if (DismountHelper.canDismountTo(vehicle.level(), spot, passenger, pose)) {
                    return new Exit(spot, pose);
                }
            }
        }
        return null;
    }

    private DismountLocator() {}
}
