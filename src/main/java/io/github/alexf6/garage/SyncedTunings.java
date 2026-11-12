package io.github.alexf6.garage;

import io.github.alexf6.garage.physics.VehicleTuning;
import java.util.Map;

/**
 * Manejo de cada vehículo tal como lo dicta el servidor al que está conectado este cliente, por id de
 * {@link VehicleSpec}. Solo el cliente lo escribe (al recibir {@link TuningSyncPayload} y al desconectarse) y solo
 * el cliente lo lee ({@link MotorVehicleEntity#tuning()} comprueba que la entidad sea de cliente), así que un
 * mundo integrado no comparte estado entre el cliente y su servidor: el servidor siempre usa su propia config.
 */
public final class SyncedTunings {

    private static volatile Map<String, VehicleTuning> remote = Map.of();

    /** El manejo que mandó el servidor para {@code id}, o {@code null} si no hay conexión o no lo mencionó. */
    public static VehicleTuning get(String id) {
        return remote.get(id);
    }

    public static void set(Map<String, VehicleTuning> tunings) {
        remote = Map.copyOf(tunings);
    }

    public static void clear() {
        remote = Map.of();
    }

    private SyncedTunings() {}
}
