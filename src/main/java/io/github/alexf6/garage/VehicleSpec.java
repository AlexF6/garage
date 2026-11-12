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
 * @param containerRows  filas (de 9) del baúl: 1 a 3
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

    /**
     * @param cooldownTicks ticks mínimos entre dos bocinazos
     * @param pitches       un sonido por cada tono, todos a la vez
     */
    public record Horn(int cooldownTicks, Holder<SoundEvent> sound, float volume, List<Float> pitches) {}
}
