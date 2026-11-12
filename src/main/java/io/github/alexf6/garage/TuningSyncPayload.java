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
            return new TuningSyncPayload(new VehicleTuning(
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, TuningSyncPayload payload) {
            VehicleTuning t = payload.tuning();
            buf.writeDouble(t.maxSpeed());
            buf.writeDouble(t.maxReverse());
            buf.writeDouble(t.accel());
            buf.writeDouble(t.reverseAccel());
            buf.writeDouble(t.brake());
            buf.writeDouble(t.handbrake());
            buf.writeDouble(t.rollingFriction());
            buf.writeDouble(t.airDrag());
            buf.writeDouble(t.waterDrag());
            buf.writeDouble(t.wheelBase());
            buf.writeDouble(t.maxSteerLow());
            buf.writeDouble(t.maxSteerHigh());
            buf.writeDouble(t.steerRate());
            buf.writeDouble(t.grip());
            buf.writeDouble(t.driftGrip());
            buf.writeDouble(t.driftYawBoost());
        }
    };

    @Override
    public Type<TuningSyncPayload> type() {
        return TYPE;
    }
}
