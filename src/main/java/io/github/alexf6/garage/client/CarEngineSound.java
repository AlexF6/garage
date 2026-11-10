package io.github.alexf6.garage.client;

import io.github.alexf6.garage.CarConfig;
import io.github.alexf6.garage.CarEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * Motor del carro, solo en el cliente y sin paquetes: un sonido en bucle que sigue al carro y cuyo tono
 * sube con la velocidad. Arranca en silencio al cargar el carro y se oye mientras haya conductor y
 * combustible (ambos ya están sincronizados). Usa el rodar del minecart a falta de un sonido propio.
 */
public class CarEngineSound extends AbstractTickableSoundInstance {

    private static final float IDLE_PITCH = 0.5F;
    private static final float TOP_PITCH = 1.4F;
    private static final float IDLE_VOLUME = 0.35F; // fracción del volumen configurado en ralentí

    private final CarEntity car;

    public CarEngineSound(CarEntity car) {
        super(SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.car = car;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = IDLE_PITCH;
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

        float ratio = (float) Mth.clamp(Math.abs(this.car.getForwardSpeed()) / CarConfig.get().tuning().maxSpeed(), 0.0, 1.0);
        float targetVolume = this.car.isEngineRunning()
                ? CarConfig.get().engineVolume() * Mth.lerp(ratio, IDLE_VOLUME, 1.0F)
                : 0.0F;
        // Suavizado: el motor no se prende ni cambia de tono de golpe
        this.volume = Mth.lerp(0.2F, this.volume, targetVolume);
        this.pitch = Mth.lerp(0.2F, this.pitch, Mth.lerp(ratio, IDLE_PITCH, TOP_PITCH));
    }

    private void follow() {
        this.x = this.car.getX();
        this.y = this.car.getY();
        this.z = this.car.getZ();
    }
}
