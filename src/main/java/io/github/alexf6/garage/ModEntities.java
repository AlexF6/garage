package io.github.alexf6.garage;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;

public final class ModEntities {

    // Asientos (x, altura del cojín, z), con el frente del carro en +Z: conductor, copiloto y dos atrás.
    private static final Vec3[] SEATS = {
            new Vec3(0.44, 0.75, 0.0),
            new Vec3(-0.44, 0.75, 0.0),
            new Vec3(0.44, 0.75, -0.75),
            new Vec3(-0.44, 0.75, -0.75),
    };

    public static final EntityType<CarEntity> CAR = register("car",
            EntityType.Builder.of(CarEntity::new, MobCategory.MISC)
                    .sized(2.4F, 2.2F)
                    .passengerAttachments(SEATS)
                    .clientTrackingRange(10));

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(
            String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
                Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, name)
        );
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void init() {}

    private ModEntities() {}
}
