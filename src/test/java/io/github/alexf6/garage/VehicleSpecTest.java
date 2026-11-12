package io.github.alexf6.garage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.alexf6.garage.physics.VehicleTuning;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class VehicleSpecTest {

    private static final List<Vec3> SEATS = List.of(new Vec3(0, 0.6, 0));
    private static final VehicleSpec.Horn HORN = new VehicleSpec.Horn(10, null, 1.0F, List.of(1.0F));

    private static VehicleSpec spec(String id, float width, List<Vec3> seats, int color, int rows,
                                    float wheel, float head, float idle, float top, float lean) {
        return new VehicleSpec(id, width, 1.0F, seats, color, rows, wheel, head, 1.0F, idle, top, lean, HORN,
                () -> VehicleTuning.WILLYS);
    }

    private static VehicleSpec valid() {
        return spec("bike", 1.0F, SEATS, 0xFF0000, 1, 0.3F, 90.0F, 0.5F, 1.5F, 0.0F);
    }

    @Test
    void unaEspecificacionValidaSeCrea() {
        assertDoesNotThrow(VehicleSpecTest::valid);
        assertEquals(1, valid().seats().size());
    }

    @Test
    void elIdSoloAdmiteMinusculasDigitosYGuionBajo() {
        for (String id : new String[] {null, "", "Car", "mi coche", "a-b", "ñandú"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> spec(id, 1.0F, SEATS, 0, 1, 0.3F, 90.0F, 0.5F, 1.5F, 0.0F), String.valueOf(id));
        }
        assertDoesNotThrow(() -> spec("moto_2", 1.0F, SEATS, 0, 1, 0.3F, 90.0F, 0.5F, 1.5F, 0.0F));
    }

    @Test
    void sinAsientosOTamanoImposibleFalla() {
        assertThrows(IllegalArgumentException.class,
                () -> spec("x", 1.0F, List.of(), 0, 1, 0.3F, 90.0F, 0.5F, 1.5F, 0.0F));
        assertThrows(IllegalArgumentException.class,
                () -> spec("x", 0.0F, SEATS, 0, 1, 0.3F, 90.0F, 0.5F, 1.5F, 0.0F));
        assertThrows(IllegalArgumentException.class,
                () -> spec("x", Float.NaN, SEATS, 0, 1, 0.3F, 90.0F, 0.5F, 1.5F, 0.0F));
    }

    @Test
    void lasFilasDelBaulVanDeUnoAlMaximoDeVanilla() {
        for (int rows : new int[] {0, -1, VehicleSpec.MAX_CONTAINER_ROWS + 1, 100}) {
            assertThrows(IllegalArgumentException.class,
                    () -> spec("x", 1.0F, SEATS, 0, rows, 0.3F, 90.0F, 0.5F, 1.5F, 0.0F), "rows " + rows);
        }
        for (int rows = 1; rows <= VehicleSpec.MAX_CONTAINER_ROWS; rows++) {
            int r = rows;
            assertDoesNotThrow(() -> spec("x", 1.0F, SEATS, 0, r, 0.3F, 90.0F, 0.5F, 1.5F, 0.0F));
        }
    }

    @Test
    void elColorDebeSerRgb() {
        assertThrows(IllegalArgumentException.class,
                () -> spec("x", 1.0F, SEATS, -1, 1, 0.3F, 90.0F, 0.5F, 1.5F, 0.0F));
        assertThrows(IllegalArgumentException.class,
                () -> spec("x", 1.0F, SEATS, 0x1000000, 1, 0.3F, 90.0F, 0.5F, 1.5F, 0.0F));
    }

    @Test
    void ruedasGiroDeCabezaTonosEInclinacionTienenRango() {
        assertThrows(IllegalArgumentException.class,
                () -> spec("x", 1.0F, SEATS, 0, 1, 0.0F, 90.0F, 0.5F, 1.5F, 0.0F));
        assertThrows(IllegalArgumentException.class,
                () -> spec("x", 1.0F, SEATS, 0, 1, 0.3F, 181.0F, 0.5F, 1.5F, 0.0F));
        assertThrows(IllegalArgumentException.class,
                () -> spec("x", 1.0F, SEATS, 0, 1, 0.3F, 90.0F, 1.5F, 0.5F, 0.0F), "top menor que idle");
        assertThrows(IllegalArgumentException.class,
                () -> spec("x", 1.0F, SEATS, 0, 1, 0.3F, 90.0F, 0.5F, 1.5F, -0.1F));
    }

    @Test
    void laBocinaNecesitaAlMenosUnTonoYUnEnfriamientoNoNegativo() {
        assertThrows(IllegalArgumentException.class, () -> new VehicleSpec.Horn(10, null, 1.0F, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new VehicleSpec.Horn(-1, null, 1.0F, List.of(1.0F)));
    }

    @Test
    void laListaDeAsientosEsInmutableAunqueSeModifiqueLaOriginal() {
        List<Vec3> seats = new ArrayList<>(SEATS);
        VehicleSpec spec = spec("x", 1.0F, seats, 0, 1, 0.3F, 90.0F, 0.5F, 1.5F, 0.0F);
        seats.add(new Vec3(1, 1, 1));
        assertEquals(1, spec.seats().size());
        assertThrows(UnsupportedOperationException.class, () -> spec.seats().add(Vec3.ZERO));
    }
}
