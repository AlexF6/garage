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
 * Se construye con {@link #builder(String)}, que nombra cada dato en vez de depender de su posición.
 *
 * @param id             nombre del registro de la entidad y del ítem; es estable: renombrarlo rompe los mundos
 * @param width          ancho de la caja de colisión (bloques)
 * @param height         alto de la caja de colisión (bloques)
 * @param seats          asientos (x, y, z) en bloques, con el frente en +Z; el primero es el conductor. Vanilla pone
 *                       los pies del jugador 0.6 por debajo y la cadera 0.15 por encima, así que {@code y} debe
 *                       ser la altura de la parte de arriba del cojín: más bajo, el jugador se hunde en el vehículo
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
 * @param mobsCanBoard   si las criaturas pequeñas que chocan con él a poca velocidad se suben de pasajeras
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
        Supplier<VehicleTuning> tuning,
        boolean mobsCanBoard) {

    public static final int MAX_CONTAINER_ROWS = 6;

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
            if (sound == null) throw new IllegalArgumentException("la bocina necesita un sonido");
            if (!(volume > 0)) throw new IllegalArgumentException("el volumen de la bocina debe ser positivo");
            if (pitches == null || pitches.isEmpty()) throw new IllegalArgumentException("la bocina necesita un tono");
            pitches = List.copyOf(pitches);
        }
    }

    /** Empieza la especificación del vehículo {@code id}; todos los datos son obligatorios salvo la inclinación. */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Especificación por nombre: cada dato va en su método, así no se pueden trasponer dos números. Lo que falte
     * queda como valor imposible y {@link #build()} falla con el motivo.
     */
    public static final class Builder {
        private final String id;
        private float width = Float.NaN;
        private float height = Float.NaN;
        private List<Vec3> seats = List.of();
        private int defaultColor = -1;
        private int containerRows;
        private float wheelRadius = Float.NaN;
        private float headTurnLimit = Float.NaN;
        private float directTurnRate = Float.NaN;
        private float idlePitch = Float.NaN;
        private float topPitch = Float.NaN;
        private float leanPerSteer;
        private Horn horn;
        private Supplier<VehicleTuning> tuning;
        private boolean mobsCanBoard = true;

        private Builder(String id) {
            this.id = id;
        }

        /** Caja de colisión en bloques. */
        public Builder size(float width, float height) {
            this.width = width;
            this.height = height;
            return this;
        }

        /** Asientos en bloques (frente en +Z, {@code y} = parte de arriba del cojín); el primero conduce. */
        public Builder seats(Vec3... seats) {
            this.seats = List.of(seats);
            return this;
        }

        /** Color RGB de la carrocería sin teñir. */
        public Builder color(int rgb) {
            this.defaultColor = rgb;
            return this;
        }

        /** Filas de 9 casillas del baúl. */
        public Builder chestRows(int rows) {
            this.containerRows = rows;
            return this;
        }

        /** Radio de las ruedas del modelo, en bloques. */
        public Builder wheelRadius(float radius) {
            this.wheelRadius = radius;
            return this;
        }

        /** Grados que puede girar la cabeza un pasajero respecto al vehículo. */
        public Builder headTurnLimit(float degrees) {
            this.headTurnLimit = degrees;
            return this;
        }

        /** Grados por tick que gira con A/D en el aire o casi detenido. */
        public Builder directTurnRate(float degreesPerTick) {
            this.directTurnRate = degreesPerTick;
            return this;
        }

        /** Tono del motor en ralentí y a velocidad máxima. */
        public Builder enginePitch(float idle, float top) {
            this.idlePitch = idle;
            this.topPitch = top;
            return this;
        }

        /** Inclinación lateral (radianes por radián de dirección a velocidad máxima); por defecto no se inclina. */
        public Builder leanPerSteer(float lean) {
            this.leanPerSteer = lean;
            return this;
        }

        /** Bocina: un sonido por cada tono, todos a la vez, y ticks mínimos entre bocinazos. */
        public Builder horn(int cooldownTicks, Holder<SoundEvent> sound, float volume, Float... pitches) {
            this.horn = new Horn(cooldownTicks, sound, volume, List.of(pitches));
            return this;
        }

        /** Si las criaturas pequeñas se suben solas de pasajeras al chocar con él; por defecto sí. */
        public Builder mobsCanBoard(boolean mobsCanBoard) {
            this.mobsCanBoard = mobsCanBoard;
            return this;
        }

        public Builder tuning(Supplier<VehicleTuning> tuning) {
            this.tuning = tuning;
            return this;
        }

        /** @throws IllegalArgumentException si falta un dato o alguno es imposible */
        public VehicleSpec build() {
            return new VehicleSpec(id, width, height, seats, defaultColor, containerRows, wheelRadius, headTurnLimit,
                    directTurnRate, idlePitch, topPitch, leanPerSteer, horn, tuning, mobsCanBoard);
        }
    }
}
