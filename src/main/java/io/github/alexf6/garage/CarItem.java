package io.github.alexf6.garage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class CarItem extends Item {

    public CarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        CarEntity car = ModEntities.CAR.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (car == null) return InteractionResult.FAIL;

        // Sobre la cara que se clicó, mirando hacia donde mira el jugador
        Vec3 spot = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
        car.setPos(spot.x, spot.y, spot.z);
        car.setYRot(context.getRotation());
        if (!level.noCollision(car, car.getBoundingBox())) return InteractionResult.FAIL;

        ItemStack stack = context.getItemInHand();
        car.setColor(DyedItemColor.getOrDefault(stack, CarEntity.DEFAULT_COLOR) & 0xFFFFFF);
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            car.setCustomName(stack.getHoverName());
        }

        serverLevel.addFreshEntity(car);
        level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, spot);
        stack.consume(1, context.getPlayer());
        return InteractionResult.SUCCESS;
    }
}
