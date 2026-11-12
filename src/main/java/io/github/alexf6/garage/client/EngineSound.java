package io.github.alexf6.garage.client;

import io.github.alexf6.garage.GarageConfig;
import io.github.alexf6.garage.MotorVehicleEntity;
import io.github.alexf6.garage.VehicleSpec;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * Motor de un vehículo del mod, solo en el cliente y sin paquetes: un sonido en bucle que sigue al vehículo
 * y cuyo tono sube con la velocidad entre {@link VehicleSpec#idlePitch()} y {@link VehicleSpec#topPitch()}.
 * Arranca en silencio al cargar la entidad y se oye mientras haya conductor y combustible (ambos sincronizados).
 */
public class EngineSound extends AbstractTickableSoundInstance {

    private static final float IDLE_VOLUME = 0.35F; // fracción del volumen configurado en ralentí

    private final MotorVehicleEntity car;

    public EngineSound(MotorVehicleEntity car) {
        super(SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.car = car;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = car.spec().idlePitch();
        follow();
    }

    @Override
    public boolean canPlaySound() {
        return !this.car.isSilent();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (this.car.isRemoved()) {
            stop();
            return;
        }
        follow();

        float ratio = (float) this.car.speedRatio();
        float targetVolume = this.car.isEngineRunning()
                ? GarageConfig.get().engineVolume() * Mth.lerp(ratio, IDLE_VOLUME, 1.0F)
                : 0.0F;
        // Suavizado: el motor no se prende ni cambia de tono de golpe
        this.volume = Mth.lerp(0.2F, this.volume, targetVolume);
        this.pitch = Mth.lerp(0.2F, this.pitch, Mth.lerp(ratio, this.car.spec().idlePitch(), this.car.spec().topPitch()));
    }

    private void follow() {
        this.x = this.car.getX();
        this.y = this.car.getY();
        this.z = this.car.getZ();
    }
}
