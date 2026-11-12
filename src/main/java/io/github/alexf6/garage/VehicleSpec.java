package io.github.alexf6.garage;

import io.github.alexf6.garage.physics.VehicleTuning;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Todo lo que distingue a un vehículo de otro, como datos. Un vehículo nuevo es una entrada en
 * {@link VehicleSpecs}: el registro de entidad e ítem, el comportamiento y el ítem salen de aquí.
 * (Además necesita su modelo y textura en el cliente, su receta, traducciones y modelo de ítem.)
 *
 * @param id             nombre del registro de la entidad y del ítem; es estable: renombrarlo rompe los mundos
 * @param width          ancho de la caja de colisión (bloques)
 * @param height         alto de la caja de colisión (bloques)
 * @param seats          asientos (x, altura del cojín, z), con el frente en +Z; el primero es el conductor
 * @param defaultColor   color RGB de la carrocería sin teñir
 * @param containerRows  filas (de 9) del baúl: 1 a {@value #MAX_CONTAINER_ROWS}
 * @param wheelRadius    radio de las ruedas (bloques); fija cuánto giran al avanzar
 * @param headTurnLimit  grados que puede girar la cabeza un pasajero respecto al vehículo
 * @param directTurnRate grados por tick que gira con A/D en el aire o casi detenido
 * @param idlePitch      tono del motor en ralentí
 * @param topPitch       tono del motor a velocidad máxima
 * @param leanPerSteer   inclinación lateral (radianes) por radián de dirección a velocidad máxima; 0 = no se inclina
 * @param horn           bocina
 * @param tuning         manejo; es un {@code Supplier} porque el del Jeep depende de la config
 */
public record VehicleSpec(
        String id,
        float width,
        float height,
        List<Vec3> seats,
        int defaultColor,
        int containerRows,
        float wheelRadius,
        float headTurnLimit,
        float directTurnRate,
        float idlePitch,
        float topPitch,
        float leanPerSteer,
        Horn horn,
        Supplier<VehicleTuning> tuning) {

    public static final int MAX_CONTAINER_ROWS = 3;

    /** Falla al crearse (es decir, al arrancar) si algún dato es imposible, en vez de dar un vehículo roto. */
    public VehicleSpec {
        if (id == null || !id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("id inválido (solo a-z, 0-9 y _): " + id);
        }
        if (!(width > 0) || !(height > 0)) throw invalid(id, "el tamaño debe ser positivo");
        if (seats == null || seats.isEmpty()) throw invalid(id, "necesita al menos un asiento (el del conductor)");
        seats = List.copyOf(seats);
        if (defaultColor < 0 || defaultColor > 0xFFFFFF) throw invalid(id, "defaultColor debe ser RGB (0 a 0xFFFFFF)");
        if (containerRows < 1 || containerRows > MAX_CONTAINER_ROWS) {
            throw invalid(id, "containerRows debe estar entre 1 y " + MAX_CONTAINER_ROWS);
        }
        if (!(wheelRadius > 0)) throw invalid(id, "wheelRadius debe ser positivo");
        if (!(headTurnLimit > 0 && headTurnLimit <= 180.0F)) throw invalid(id, "headTurnLimit debe estar en (0, 180]");
        if (!(directTurnRate >= 0)) throw invalid(id, "directTurnRate no puede ser negativo");
        if (!(idlePitch > 0) || !(topPitch >= idlePitch)) throw invalid(id, "los tonos deben cumplir 0 < idle <= top");
        if (!(leanPerSteer >= 0)) throw invalid(id, "leanPerSteer no puede ser negativo");
        if (horn == null || tuning == null) throw invalid(id, "horn y tuning son obligatorios");
    }

    private static IllegalArgumentException invalid(String id, String reason) {
        return new IllegalArgumentException("VehicleSpec " + id + ": " + reason);
    }

    /**
     * @param cooldownTicks ticks mínimos entre dos bocinazos
     * @param pitches       un sonido por cada tono, todos a la vez
     */
    public record Horn(int cooldownTicks, Holder<SoundEvent> sound, float volume, List<Float> pitches) {
        public Horn {
            if (cooldownTicks < 0) throw new IllegalArgumentException("cooldownTicks no puede ser negativo");
            if (pitches == null || pitches.isEmpty()) throw new IllegalArgumentException("la bocina necesita un tono");
            pitches = List.copyOf(pitches);
        }
    }
}
