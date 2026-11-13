package io.github.alexf6.garage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.Vec3;

/**
 * Registra la entidad y el ítem de cada {@link VehicleSpec}. Se llama una sola vez desde
 * {@code GarageMod.onInitialize}; antes de eso no hay nada registrado.
 */
public final class ModVehicles {

    private record Registered(EntityType<MotorVehicleEntity> entityType, Item item) {}

    // Se asigna una sola vez en register(); después solo se lee
    private static volatile Map<String, Registered> registered = Map.of();

    public static void register() {
        Map<String, Registered> result = new LinkedHashMap<>();
        for (VehicleSpec spec : VehicleSpecs.ALL) {
            if (result.containsKey(spec.id())) {
                throw new IllegalStateException("VehicleSpecs.ALL repite el id " + spec.id());
            }
            EntityType<MotorVehicleEntity> entityType = registerEntity(spec);
            result.put(spec.id(), new Registered(entityType, registerItem(spec, entityType)));
        }
        registered = Collections.unmodifiableMap(result);

        // Mostrar vehículos en la pestaña "Herramientas y utilidades" (donde están los botes)
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(tab -> VehicleSpecs.ALL.forEach(spec -> tab.accept(item(spec))));
    }

    /** Si {@link #register()} ya se ejecutó en esta JVM. */
    static boolean isRegistered() {
        return !registered.isEmpty();
    }

    public static EntityType<MotorVehicleEntity> entityType(VehicleSpec spec) {
        return lookup(spec).entityType();
    }

    public static Item item(VehicleSpec spec) {
        return lookup(spec).item();
    }

    /** Ítem de un vehículo que conserva el color de la carrocería (componente vanilla dyed_color). */
    public static ItemStack stack(VehicleSpec spec, int rgb) {
        ItemStack stack = new ItemStack(item(spec));
        if (rgb != spec.defaultColor()) {
            stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb));
        }
        return stack;
    }

    private static Registered lookup(VehicleSpec spec) {
        Registered found = registered.get(spec.id());
        if (found == null) {
            throw new IllegalStateException("El vehículo " + spec.id()
                    + " no está registrado: ModVehicles.register() no se ha ejecutado o no lo incluye VehicleSpecs.ALL");
        }
        return found;
    }

    private static EntityType<MotorVehicleEntity> registerEntity(VehicleSpec spec) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
                Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, spec.id()));
        EntityType<MotorVehicleEntity> type = EntityType.Builder
                .<MotorVehicleEntity>of((t, level) -> new MotorVehicleEntity(t, level, spec), MobCategory.MISC)
                .sized(spec.width(), spec.height())
                .passengerAttachments(spec.seats().toArray(new Vec3[0]))
                .clientTrackingRange(10)
                .build(key);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
    }

    private static Item registerItem(VehicleSpec spec, EntityType<MotorVehicleEntity> entityType) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM, Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, spec.id()));
        Item.Properties props = new Item.Properties().stacksTo(1);
        return Registry.register(BuiltInRegistries.ITEM, key, new VehicleItem(entityType, props.setId(key)));
    }

    private ModVehicles() {}
}
