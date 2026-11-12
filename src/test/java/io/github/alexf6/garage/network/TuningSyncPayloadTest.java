package io.github.alexf6.garage.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexf6.garage.physics.VehicleTuning;
import io.netty.buffer.Unpooled;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TuningSyncPayloadTest {

    @AfterEach
    void reset() {
        SyncedTunings.clear();
    }

    private static TuningSyncPayload roundTrip(TuningSyncPayload payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        TuningSyncPayload.write(buf, payload);
        TuningSyncPayload read = TuningSyncPayload.read(buf);
        assertEquals(0, buf.readableBytes(), "no deben sobrar bytes");
        return read;
    }

    @Test
    void idaYVueltaConservaElManejoDeCadaVehiculo() {
        TuningSyncPayload payload = new TuningSyncPayload(Map.of(
                "car", VehicleTuning.WILLYS, "motorcycle", VehicleTuning.ENDURO));
        assertEquals(payload, roundTrip(payload));
    }

    @Test
    void unPaqueteVacioEsValido() {
        assertTrue(roundTrip(new TuningSyncPayload(Map.of())).tunings().isEmpty());
    }

    @Test
    void unNumeroDeVehiculosAbsurdoSeRechazaAntesDeLeer() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(TuningSyncPayload.MAX_ENTRIES + 1);
        assertThrows(IllegalArgumentException.class, () -> TuningSyncPayload.read(buf));
        FriendlyByteBuf negative = new FriendlyByteBuf(Unpooled.buffer());
        negative.writeVarInt(-1);
        assertThrows(IllegalArgumentException.class, () -> TuningSyncPayload.read(negative));
    }

    @Test
    void elMapaDelPaqueteEsInmutable() {
        java.util.HashMap<String, VehicleTuning> source = new java.util.HashMap<>();
        source.put("car", VehicleTuning.WILLYS);
        TuningSyncPayload payload = new TuningSyncPayload(source);
        source.put("x", VehicleTuning.ENDURO);
        assertEquals(1, payload.tunings().size());
        assertThrows(UnsupportedOperationException.class, () -> payload.tunings().put("y", VehicleTuning.ENDURO));
    }

    @Test
    void elManejoSincronizadoSeConsultaPorIdYSeLimpiaAlDesconectar() {
        assertNull(SyncedTunings.get("car"));
        SyncedTunings.set(Map.of("car", VehicleTuning.ENDURO));
        assertEquals(VehicleTuning.ENDURO, SyncedTunings.get("car"));
        assertNull(SyncedTunings.get("motorcycle"), "un id que el servidor no mencionó no tiene override");
        SyncedTunings.clear();
        assertNull(SyncedTunings.get("car"));
    }
}
