package io.github.alexf6.garage.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.alexf6.garage.VehicleSpec;
import io.github.alexf6.garage.VehicleSpecs;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ClientVehiclesTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void cadaVehiculoTieneSuModeloEnElCliente() {
        for (VehicleSpec spec : VehicleSpecs.ALL) {
            ClientVehicles.Model model = ClientVehicles.lookOf(spec);
            assertNotNull(model.look().texture(), spec.id());
            assertNotNull(model.bodyLayer().get(), spec.id() + " sin capa BODY");
            assertNotNull(model.trimLayer().get(), spec.id() + " sin capa TRIM");
        }
    }
}
