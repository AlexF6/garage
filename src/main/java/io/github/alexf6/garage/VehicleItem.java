package io.github.alexf6.garage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Ítem que coloca un vehículo del mod en el mundo conservando su color teñido y su nombre personalizado.
 */
public class VehicleItem extends Item {

    private final EntityType<? extends MotorVehicleEntity> entityType;

    public VehicleItem(EntityType<? extends MotorVehicleEntity> entityType, Properties properties) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        MotorVehicleEntity vehicle = this.entityType.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (vehicle == null) return InteractionResult.FAIL;

        // Sobre la cara que se clicó, mirando hacia donde mira el jugador
        Vec3 spot = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
        vehicle.setPos(spot.x, spot.y, spot.z);
        vehicle.setYRot(context.getRotation());
        if (!level.noCollision(vehicle, vehicle.getBoundingBox())) return InteractionResult.FAIL;

        ItemStack stack = context.getItemInHand();
        vehicle.setColor(DyedItemColor.getOrDefault(stack, vehicle.defaultColor()));
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            vehicle.setCustomName(stack.getHoverName());
        }

        serverLevel.addFreshEntity(vehicle);
        level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, spot);
        stack.consume(1, context.getPlayer());
        return InteractionResult.SUCCESS;
    }
}
