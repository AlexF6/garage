package io.github.alexf6.garage.physics;

/**
 * Entrada del conductor en un tick, inmutable.
 *
 * @param throttle 1 acelera, -1 frena/retrocede, 0 suelta
 * @param steering 1 derecha, -1 izquierda, 0 recto
 * @param brake    freno de mano
 */
public record VehicleInput(int throttle, int steering, boolean brake) {

    public static final VehicleInput NONE = new VehicleInput(0, 0, false);

    public static VehicleInput of(boolean left, boolean right, boolean forward, boolean backward, boolean brake) {
        return new VehicleInput(axis(forward, backward), axis(right, left), brake);
    }

    private static int axis(boolean positive, boolean negative) {
        return (positive ? 1 : 0) - (negative ? 1 : 0);
    }
}
