package io.github.alexf6.garage;

import io.github.alexf6.garage.physics.VehicleTuning;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Servidor → cliente, al conectarse: el manejo del Jeep que dicta el servidor. La física la calcula el cliente
 * del conductor, así que sin esto un {@code tuning} distinto en su config le daría ventaja o descuadraría los
 * atropellos. El cliente debe validar el valor recibido con {@link VehicleTuning#firstInvalid()}.
 */
public record TuningSyncPayload(VehicleTuning tuning) implements CustomPacketPayload {

    public static final Type<TuningSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, "tuning_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TuningSyncPayload> CODEC = new StreamCodec<>() {
        @Override
        public TuningSyncPayload decode(RegistryFriendlyByteBuf buf) {
            double[] values = new double[VehicleTuning.FIELD_COUNT];
            for (int i = 0; i < values.length; i++) values[i] = buf.readDouble();
            return new TuningSyncPayload(VehicleTuning.fromArray(values));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, TuningSyncPayload payload) {
            for (double value : payload.tuning().toArray()) buf.writeDouble(value);
        }
    };

    @Override
    public Type<TuningSyncPayload> type() {
        return TYPE;
    }
}
