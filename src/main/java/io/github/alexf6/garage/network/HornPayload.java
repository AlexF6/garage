package io.github.alexf6.garage.network;

import io.github.alexf6.garage.GarageMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente → servidor: el conductor presionó la bocina. No lleva datos. */
public record HornPayload() implements CustomPacketPayload {

    public static final Type<HornPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, "horn"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HornPayload> CODEC =
            StreamCodec.unit(new HornPayload());

    @Override
    public Type<HornPayload> type() {
        return TYPE;
    }
}
