package io.github.alexf6.garage;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public final class ModItems {

    public static final Item CAR = register("car", ModEntities.CAR);
    public static final Item MOTORCYCLE = register("motorcycle", ModEntities.MOTORCYCLE);
    public static final Item MOTOCARRO = register("motocarro", ModEntities.MOTOCARRO);

    private static Item register(String name, EntityType<? extends CarEntity> entityType) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, name)
        );
        Item.Properties props = new Item.Properties().stacksTo(1);
        return Registry.register(BuiltInRegistries.ITEM, key, new CarItem(entityType, props.setId(key)));
    }

    /** Ítem de un vehículo que conserva el color de la carrocería (componente vanilla dyed_color). */
    public static ItemStack vehicleStack(Item item, int rgb, int defaultColor) {
        ItemStack stack = new ItemStack(item);
        if (rgb != defaultColor) {
            stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb));
        }
        return stack;
    }

    public static ItemStack carStack(int rgb) {
        return vehicleStack(CAR, rgb, CarEntity.DEFAULT_COLOR);
    }

    public static ItemStack motorcycleStack(int rgb) {
        return vehicleStack(MOTORCYCLE, rgb, MotorcycleEntity.DEFAULT_COLOR);
    }

    public static ItemStack motocarroStack(int rgb) {
        return vehicleStack(MOTOCARRO, rgb, MotocarroEntity.DEFAULT_COLOR);
    }

    public static void init() {
        // Mostrar vehículos en la pestaña "Herramientas y utilidades" (donde están los botes)
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(tab -> {
                    tab.accept(CAR);
                    tab.accept(MOTORCYCLE);
                    tab.accept(MOTOCARRO);
                });
    }

    private ModItems() {}
}
