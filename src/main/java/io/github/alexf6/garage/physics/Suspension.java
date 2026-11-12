package io.github.alexf6.garage.physics;

/**
 * Suspensión visual: resortes amortiguados para el cabeceo (acelerar/frenar), el balanceo (curvas) y el
 * rebote vertical (escalones, caídas) de la carrocería respecto a las ruedas. Solo afecta al render.
 * O(1) por tick y sin dependencias de Minecraft; guarda el valor anterior para interpolar entre ticks.
 */
public final class Suspension {

    private static final double PITCH_PER_ACCEL = 1.5;  // rad por (bloque/tick²)
    private static final double ROLL_PER_ACCEL = 1.2;
    private static final double MAX_ANGLE = 0.08;       // ~4.5°
    private static final double MAX_HEAVE = 0.12;       // bloques (~2 píxeles)
    private static final double STIFFNESS = 0.3;
    private static final double DAMPING = 0.6;          // velocidad que se conserva por tick

    private double pitch, pitchVel, roll, rollVel, heave, heaveVel;
    private float pitchO, rollO, heaveO;

    /**
     * @param longAccel aceleración hacia el frente (bloques/tick²); positiva al acelerar
     * @param latAccel  aceleración centrípeta (v · ω); positiva en curva a la derecha
     * @param dy        lo que subió el carro este tick (bloques)
     */
    public void tick(double longAccel, double latAccel, double dy) {
        pitchO = (float) pitch;
        rollO = (float) roll;
        heaveO = (float) heave;

        // Al acelerar sube la trompa (cabeceo negativo); en curva la carrocería se inclina hacia afuera
        pitchVel = (pitchVel + (clamp(-longAccel * PITCH_PER_ACCEL, MAX_ANGLE) - pitch) * STIFFNESS) * DAMPING;
        pitch = clamp(pitch + pitchVel, MAX_ANGLE);
        rollVel = (rollVel + (clamp(latAccel * ROLL_PER_ACCEL, MAX_ANGLE) - roll) * STIFFNESS) * DAMPING;
        roll = clamp(roll + rollVel, MAX_ANGLE);

        // La carrocería se queda atrás cuando las ruedas suben o bajan de golpe y luego el resorte la alcanza
        heave = clamp(heave - dy * 0.5, MAX_HEAVE);
        heaveVel = (heaveVel - heave * STIFFNESS) * DAMPING;
        heave = clamp(heave + heaveVel, MAX_HEAVE);
    }

    public float pitch(float partialTick) {
        return lerp(partialTick, pitchO, (float) pitch);
    }

    public float roll(float partialTick) {
        return lerp(partialTick, rollO, (float) roll);
    }

    /** Desplazamiento vertical de la carrocería en bloques (positivo = arriba). */
    public float heave(float partialTick) {
        return lerp(partialTick, heaveO, (float) heave);
    }

    private static double clamp(double value, double limit) {
        return VehiclePhysics.clamp(value, -limit, limit);
    }

    private static float lerp(float delta, float from, float to) {
        return from + delta * (to - from);
    }
}
