package io.github.alexf6.garage.network;

import io.github.alexf6.garage.GarageMod;
import io.github.alexf6.garage.VehicleSpec;
import io.github.alexf6.garage.physics.VehicleTuning;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Servidor → cliente, al conectarse: el manejo de cada vehículo según el servidor, por id de {@link VehicleSpec}.
 * La física la calcula el cliente del conductor, así que sin esto un {@code tuning} distinto en su config le daría
 * ventaja o descuadraría los atropellos. El cliente debe validar cada valor con {@link VehicleTuning#firstInvalid()}.
 *
 * @param tunings manejo por id de vehículo
 */
public record TuningSyncPayload(Map<String, VehicleTuning> tunings) implements CustomPacketPayload {

    public static final Type<TuningSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(GarageMod.MOD_ID, "tuning_sync"));

    /** Tope de vehículos por paquete y de longitud de un id: un servidor hostil no puede inflar la lectura. */
    static final int MAX_ENTRIES = 64;
    static final int MAX_ID_LENGTH = 64;

    public static final StreamCodec<RegistryFriendlyByteBuf, TuningSyncPayload> CODEC = new StreamCodec<>() {
        @Override
        public TuningSyncPayload decode(RegistryFriendlyByteBuf buf) {
            return read(buf);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, TuningSyncPayload payload) {
            write(buf, payload);
        }
    };

    public TuningSyncPayload {
        tunings = Map.copyOf(tunings);
    }

    /** El manejo que este servidor usa para cada uno de {@code specs}. */
    public static TuningSyncPayload of(List<VehicleSpec> specs) {
        Map<String, VehicleTuning> tunings = new HashMap<>();
        for (VehicleSpec spec : specs) tunings.put(spec.id(), spec.tuning().get());
        return new TuningSyncPayload(tunings);
    }

    static void write(FriendlyByteBuf buf, TuningSyncPayload payload) {
        buf.writeVarInt(payload.tunings().size());
        payload.tunings().forEach((id, tuning) -> {
            buf.writeUtf(id, MAX_ID_LENGTH);
            for (double value : tuning.toArray()) buf.writeDouble(value);
        });
    }

    static TuningSyncPayload read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("demasiados vehículos en el paquete: " + count);
        }
        Map<String, VehicleTuning> tunings = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String id = buf.readUtf(MAX_ID_LENGTH);
            double[] values = new double[VehicleTuning.FIELD_COUNT];
            for (int v = 0; v < values.length; v++) values[v] = buf.readDouble();
            tunings.put(id, VehicleTuning.fromArray(values));
        }
        return new TuningSyncPayload(tunings);
    }

    @Override
    public Type<TuningSyncPayload> type() {
        return TYPE;
    }
}
