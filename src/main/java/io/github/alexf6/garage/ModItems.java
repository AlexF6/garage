package io.github.alexf6.garage;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public final class ModItems {

    public static final Item CAR = register("car");

    private static Item register(String name) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, name)
        );
        Item.Properties props = new Item.Properties().stacksTo(1);
        return Registry.register(BuiltInRegistries.ITEM, key, new CarItem(props.setId(key)));
    }

    /** Item del carro que conserva el color de la carrocería (componente vanilla dyed_color). */
    public static ItemStack carStack(int rgb) {
        ItemStack stack = new ItemStack(CAR);
        if (rgb != CarEntity.DEFAULT_COLOR) {
            stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb));
        }
        return stack;
    }

    public static void init() {
        // Mostrar el carro en la pestaña "Herramientas y utilidades" (donde están los botes)
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(tab -> tab.accept(CAR));
    }

    private ModItems() {}
}
