package io.github.alexf6.garage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModVehiclesTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void consultarUnVehiculoAntesDeRegistrarDaUnMensajeClaro() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ModVehicles.item(VehicleSpecs.CAR));
        assertTrue(e.getMessage().contains("car"), e.getMessage());
        assertTrue(e.getMessage().contains("register()"), e.getMessage());
        assertThrows(IllegalStateException.class, () -> ModVehicles.entityType(VehicleSpecs.MOTORCYCLE));
    }
}
