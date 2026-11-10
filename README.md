# Garage

Fabric, Minecraft 26.3, Java 25. Vehículos para Minecraft. Incluye un Jeep Willys "Yipao" cafetero de 4
asientos, una moto Enduro (Yamaha XTZ 125) de 2 asientos y un Motocarro urbano (Bajaj RE / "Torito") de 3 asientos, con combustible, baúl, colores, bocina y física reactiva;
el mod está pensado para sumar más vehículos. Hay que tener el mod en cliente y servidor.

## Uso

- **Poner el vehículo:** clic derecho con el ítem sobre un bloque. Queda mirando hacia donde miras.
- **Subir:** clic derecho sobre el vehículo. El primer jugador en subir conduce (4 asientos en el carro, 2 en tándem en la moto y 3 en el motocarro).
- **Bajar:** shift. Sales por la puerta o costado de tu asiento; si ese lado está tapado, por el otro.
- **Combustible:** clic derecho con carbón o carbón vegetal. Cada unidad da 80 s de acelerador y el tanque lleno 10 min.
  Solo se gasta mientras aceleras o retrocedes. El nivel aparece en la barra de acción mientras conduces.
- **Color:** clic derecho con cualquier tinte.
- **Baúl / alforja:** shift + clic derecho sobre el vehículo (27 espacios en el carro, 18 en el motocarro y 9 en la moto).
- **Romperlo:** golpéalo como a un bote. Suelta el ítem con su color y su nombre, y lo que haya en el baúl.

### Manejo

| Tecla | Acción |
|---|---|
| W / S | Acelerar / frenar y retroceder |
| A / D | Girar |
| Espacio | Freno de mano: frena y, en curva, derrapa |
| H | Bocina (se puede cambiar en Controles → Garage) |

- Velocidad máxima hacia adelante de unos 43 km/h en el carro (14 km/h en reversa), 68 km/h en la moto Enduro y 52 km/h en el motocarro. Todos suben bloques completos sin saltar.
- La dirección depende de la velocidad: a baja velocidad gira cerrado (radio de unos 2.5 bloques) y a alta se endurece.
  En reversa el giro se invierte, como en un carro real.
- En el agua se hunde despacio y no tiene tracción. El carro amortigua las caídas: los pasajeros no reciben daño.
- Desde unos 7 km/h aparta a las criaturas que tiene delante y desde 18 km/h además las lastima. A otros jugadores
  solo los empuja.

### Receta

| Ítem | Receta |
|---|---|
| Carro | `IGI` / `IFI` / `KKK` con lingotes de hierro (`I`), panel de vidrio (`G`), horno (`F`) y lana negra (`K`) |
| Moto | ` H ` / `IFI` / `KWK` con barrotes de hierro (`H`), lingotes (`I`), horno (`F`), lana negra (`K`) y cuero (`W`) |
| Motocarro | `LGL` / `IFI` / `KCK` con cuero (`L`), vidrio (`G`), lingotes (`I`), horno (`F`), lana negra (`K`) y cofre (`C`) |

## Configuración

`config/garage.json` se crea al primer arranque. Las claves que falten se completan con su valor por defecto y el
archivo se reescribe. Si un valor de `tuning` es inválido se usa el manejo por defecto, y si el archivo no se puede
leer se usan los valores por defecto sin modificarlo. Hay que reiniciar el juego para aplicar cambios.

Las velocidades van en bloques/tick (1 b/t = 72 km/h) y los factores son lo que se conserva por tick
(1 = no frena, 0 = frena en seco).

| Clave | Por defecto | Qué hace |
|---|---|---|
| `maxFuel` | 12000 | Ticks de acelerador con el tanque lleno |
| `fuelPerItem` | 1600 | Ticks que suma cada carbón |
| `engineVolume` | 0.6 | Volumen del motor, de 0 a 1 (0 lo apaga) |
| `tuning.maxSpeed` / `maxReverse` | 0.6 / 0.2 | Velocidad máxima hacia adelante / en reversa |
| `tuning.accel` / `reverseAccel` | 0.03 / 0.02 | Aceleración al arrancar (se suaviza al acercarse al máximo) |
| `tuning.brake` / `handbrake` | 0.04 / 0.035 | Velocidad que quita por tick el freno / el freno de mano |
| `tuning.rollingFriction` | 0.98 | Factor sin acelerar, en el suelo |
| `tuning.airDrag` / `waterDrag` | 0.995 / 0.85 | Factor en el aire / en el agua |
| `tuning.wheelBase` | 1.75 | Distancia entre ejes en bloques: más larga, giro más abierto |
| `tuning.maxSteerLow` / `maxSteerHigh` | 0.6 / 0.2 | Ángulo máximo del volante (radianes, menor que 1.57) detenido / a velocidad máxima |
| `tuning.steerRate` | 0.1 | Cuánto se mueve el volante por tick |
| `tuning.grip` | 0.3 | Factor de la velocidad lateral con agarre normal |
| `tuning.driftGrip` | 0.92 | Factor de la velocidad lateral con freno de mano (más alto, más derrape) |
| `tuning.driftYawBoost` | 1.6 | Cuánto más gira el carro mientras derrapa |
| `runOver.enabled` | true | Empujar y atropellar criaturas |
| `runOver.pushSpeed` | 0.1 | Velocidad desde la que empuja |
| `runOver.damageSpeed` | 0.25 | Velocidad desde la que además lastima |
| `runOver.damagePerSpeed` | 25.0 | Daño por cada b/t por encima de `damageSpeed` (a tope: 8.75, unos 4 corazones) |
| `runOver.knockback` | 1.2 | Fuerza del empuje a velocidad máxima |
| `runOver.hurtPlayers` | false | Lastimar también a otros jugadores |

En un servidor dedicado, la física la calcula el cliente de cada conductor con su propio `tuning`; el combustible y
los atropellos los decide el servidor con el suyo. Conviene que `tuning` coincida en todos.

## Desarrollo

```
./gradlew test        # pruebas de la física, la suspensión y la configuración
./gradlew build       # compila, prueba y deja el jar en build/libs/
```

El build solo escribe dentro del proyecto. Para que además copie el jar a tu carpeta de mods, define `modsDir` (y,
opcionalmente, `exportDir`) en tu `~/.gradle/gradle.properties` o pásalo con `-PmodsDir=...`. `modsDir` también borra
los jars anteriores de este mod en esa carpeta.

Sigue el patrón del bote vanilla: el cliente del conductor simula el movimiento y el servidor recibe la posición. El
combustible y el color viven en el servidor y se sincronizan; las ruedas, la suspensión y el sonido salen del
movimiento observado, así que se ven igual para todos sin paquetes extra.

| Clase | Responsabilidad |
|---|---|
| `CarEntity`, `MotorcycleEntity`, `MotocarroEntity` | Entidades: manejo, combustible, atropellos, asientos, alforja/baúl e interacciones |
| `physics/CarPhysics` | Funciones puras del manejo: velocidad, volante, giro tipo bicicleta y derrape |
| `physics/CarTuning`, `CarInput` | Parámetros de un vehículo (`WILLYS`, `ENDURO`, `MOTOCARRO`) y entrada inmutable del conductor |
| `physics/Suspension` | Resortes del cabeceo, el balanceo y el rebote (solo visual) |
| `CarConfig` | Lee, completa y valida `config/garage.json` |
| `CarItem`, `ModItems`, `ModEntities` | Ítem parametrizado que pone cualquier vehículo (conserva color y nombre), registros y asientos |
| `HornPayload` | Paquete cliente → servidor de la bocina |
| `client/GarageClient` | Teclas del conductor, bocina y arranque del sonido del motor |
| `client/CarModels`, `CarRenderer` | Modelo del Jeep en dos capas, ruedas, suspensión y bandera |
| `client/MotorcycleModels`, `MotorcycleRenderer` | Modelo de la moto XTZ en dos capas, horquilla inclinada (rake), ruedas y tumbada (lean) |
| `client/MotocarroModels`, `MotocarroRenderer` | Modelo del motocarro en dos capas, tren delantero Delta, suspensión y balanceo centrífugo |
| `client/CarEngineSound` | Motor en bucle cuyo tono sube con la velocidad según el rango de cada vehículo |

Para agregar otro vehículo basta otra instancia de `CarTuning`, sin tocar `CarPhysics`.

Reglas del modelo: las piezas solo se tocan, nunca se solapan con caras en el mismo plano (si no, parpadean). Todo lo
que no son ruedas cuelga de `chassis`, para que la suspensión mueva la carrocería sin mover las ruedas.

### Pendiente

- El motor usa el sonido del minecart a falta de un sonido propio.
