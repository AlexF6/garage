package io.github.alexf6.garage.client;

import io.github.alexf6.garage.CarConfig;
import io.github.alexf6.garage.CarEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * Motor de un vehículo del mod, solo en el cliente y sin paquetes: un sonido en bucle que sigue al vehículo
 * y cuyo tono sube con la velocidad entre {@link CarEntity#idleEnginePitch()} y {@link CarEntity#topEnginePitch()}.
 * Arranca en silencio al cargar la entidad y se oye mientras haya conductor y combustible (ambos sincronizados).
 */
public class CarEngineSound extends AbstractTickableSoundInstance {

    private static final float IDLE_VOLUME = 0.35F; // fracción del volumen configurado en ralentí

    private final CarEntity car;

    public CarEngineSound(CarEntity car) {
        super(SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.car = car;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = car.idleEnginePitch();
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

        float ratio = (float) Mth.clamp(Math.abs(this.car.getForwardSpeed()) / this.car.tuning().maxSpeed(), 0.0, 1.0);
        float targetVolume = this.car.isEngineRunning()
                ? CarConfig.get().engineVolume() * Mth.lerp(ratio, IDLE_VOLUME, 1.0F)
                : 0.0F;
        // Suavizado: el motor no se prende ni cambia de tono de golpe
        this.volume = Mth.lerp(0.2F, this.volume, targetVolume);
        this.pitch = Mth.lerp(0.2F, this.pitch, Mth.lerp(ratio, this.car.idleEnginePitch(), this.car.topEnginePitch()));
    }

    private void follow() {
        this.x = this.car.getX();
        this.y = this.car.getY();
        this.z = this.car.getZ();
    }
}
