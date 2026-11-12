# Garage

Fabric, Minecraft 26.3, Java 25. Vehículos para Minecraft. Incluye un Jeep Willys "Yipao" cafetero de 4
asientos, una moto Enduro (Yamaha XTZ 125) de 2 asientos, un Motocarro urbano (Bajaj RE / "Torito") de 3 asientos y un
Motocarguero (trimoto de carga 200cc) con platón, con combustible, baúl, colores, bocina y física reactiva;
el mod está pensado para sumar más vehículos. Hay que tener el mod en cliente y servidor.

## Uso

- **Poner el vehículo:** clic derecho con el ítem sobre un bloque. Queda mirando hacia donde miras.
- **Subir:** clic derecho sobre el vehículo. El primer jugador en subir conduce (4 asientos en el carro, 2 en tándem en la moto, 3 en el motocarro y 1 en el motocarguero).
- **Bajar:** shift. Sales por la puerta o costado de tu asiento; si ese lado está tapado, por el otro.
- **Combustible:** clic derecho con carbón o carbón vegetal. Cada unidad da 80 s de acelerador y el tanque lleno 10 min.
  Solo se gasta mientras aceleras o retrocedes. El nivel aparece en la barra de acción mientras conduces.
- **Color:** clic derecho con cualquier tinte.
- **Baúl / alforja:** shift + clic derecho sobre el vehículo (27 espacios en el carro, 54 en el platón del motocarguero, 18 en el motocarro y 9 en la moto).
- **Romperlo:** golpéalo como a un bote. Suelta el ítem con su color y su nombre, y lo que haya en el baúl.

### Manejo

| Tecla | Acción |
|---|---|
| W / S | Acelerar / frenar y retroceder |
| A / D | Girar |
| Espacio | Freno de mano: frena y, en curva, derrapa |
| H | Bocina (se puede cambiar en Controles → Garage) |

- Velocidad máxima hacia adelante de unos 43 km/h en el carro (14 km/h en reversa), 68 km/h en la moto Enduro, 52 km/h en el motocarro y 47 km/h en el motocarguero. Todos suben bloques completos sin saltar.
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
| Motocarguero | `BLC` / `IFI` / `KBK` con barrotes de hierro (`B`), cuero (`L`), cofre (`C`), lingotes (`I`), horno (`F`) y lana negra (`K`) |

## Configuración

`config/garage.json` se crea al primer arranque. Las claves que falten se completan con su valor por defecto y el
archivo se reescribe. Hay que reiniciar el juego (o el servidor) para aplicar cambios.

- Un valor fuera de rango se corrige al límite más cercano, se avisa en el log y el archivo se reescribe ya corregido.
  Un `tuning` inválido se sustituye entero por el manejo por defecto.
- Si el archivo no es JSON válido (o un valor tiene el tipo equivocado) se guarda como `garage.json.bak`, se crea
  uno nuevo con los valores por defecto y se avisa en el log. Si no se puede leer por permisos, se juega con los
  valores por defecto sin tocarlo.
- Es una config **de servidor**: en multijugador manda la del servidor. Al conectarse, el servidor envía a cada cliente
  el manejo de todos los vehículos, que reemplaza al suyo mientras dure la conexión. La moto, el motocarro y el
  motocarguero usan perfiles fijos del mod (`ENDURO`, `MOTOCARRO` y `MOTOCARGUERO`) y no leen `tuning`.

Las velocidades van en bloques/tick (1 b/t = 72 km/h) y los factores son lo que se conserva por tick
(1 = no frena, 0 = frena en seco).

| Clave | Por defecto | Rango | Qué hace |
|---|---|---|---|
| `maxFuel` | 12000 | 1 a 1 000 000 | Ticks de acelerador con el tanque lleno |
| `fuelPerItem` | 1600 | 0 a `maxFuel` | Ticks que suma cada carbón |
| `engineVolume` | 0.6 | 0 a 1 | Volumen del motor (0 lo apaga) |
| `tuning.maxSpeed` / `maxReverse` | 0.6 / 0.2 | mayor que 0, hasta 3 | Velocidad máxima hacia adelante / en reversa (b/t) |
| `tuning.accel` / `reverseAccel` | 0.03 / 0.02 | mayor que 0, hasta 1 | Aceleración al arrancar (se suaviza al acercarse al máximo) |
| `tuning.brake` / `handbrake` | 0.04 / 0.035 | 0 a 1 | Velocidad que quita por tick el freno / el freno de mano |
| `tuning.rollingFriction` | 0.98 | 0 a 1 | Factor sin acelerar, en el suelo |
| `tuning.airDrag` / `waterDrag` | 0.995 / 0.85 | 0 a 1 | Factor en el aire / en el agua |
| `tuning.wheelBase` | 1.75 | mayor que 0, hasta 16 | Distancia entre ejes en bloques: más larga, giro más abierto |
| `tuning.maxSteerLow` / `maxSteerHigh` | 0.6 / 0.2 | 0 a menos de 1.57 | Ángulo máximo del volante (radianes) detenido / a velocidad máxima |
| `tuning.steerRate` | 0.1 | mayor que 0, hasta 1 | Cuánto se mueve el volante por tick (radianes) |
| `tuning.grip` | 0.3 | 0 a 1 | Factor de la velocidad lateral con agarre normal |
| `tuning.driftGrip` | 0.92 | 0 a 1 | Factor de la velocidad lateral con freno de mano (más alto, más derrape) |
| `tuning.driftYawBoost` | 1.6 | mayor que 0, hasta 10 | Cuánto más gira el carro mientras derrapa |
| `runOver.enabled` | true | true / false | Empujar y atropellar criaturas |
| `runOver.pushSpeed` | 0.1 | 0 a 10 | Velocidad desde la que empuja (b/t) |
| `runOver.damageSpeed` | 0.25 | `pushSpeed` a 10 | Velocidad desde la que además lastima (b/t) |
| `runOver.damagePerSpeed` | 25.0 | 0 a 1000 | Daño por cada b/t por encima de `damageSpeed` (a tope: 8.75, unos 4 corazones) |
| `runOver.knockback` | 1.2 | 0 a 10 | Fuerza del empuje a velocidad máxima |
| `runOver.hurtPlayers` | false | true / false | Lastimar también a otros jugadores |

El servidor calcula los atropellos con la velocidad que mide entre dos ticks, limitada a la `maxSpeed` del vehículo,
así que un cliente alterado no consigue más daño ni empuje del que permite el manejo.

## Desarrollo

```
./gradlew test        # pruebas de la física, la suspensión y la configuración
./gradlew build       # compila, prueba y deja el jar en build/libs/
```

El build solo escribe dentro del proyecto. Para copiar el jar a tu carpeta de mods, define `modsDir` (y,
opcionalmente, `exportDir`) en tu `~/.gradle/gradle.properties` o pásalo con `-PmodsDir=...`, y ejecuta a mano
`./gradlew installMod` (o `exportMod`); no se lanzan solas con `build`. `installMod` también borra los jars anteriores
de este mod en esa carpeta.

Sigue el patrón del bote vanilla: el cliente del conductor simula el movimiento y el servidor recibe la posición. El
combustible y el color viven en el servidor y se sincronizan; las ruedas, la suspensión y el sonido salen del
movimiento observado, así que se ven igual para todos sin paquetes extra.

| Clase | Responsabilidad |
|---|---|
| `MotorVehicleEntity` | Entidad única de todos los vehículos: estado, combustible, asientos e interacciones. Lo que cambia de uno a otro lo dicta su `VehicleSpec` |
| `ChestVehicleEntity` | Base con el baúl de 1 a 3 filas: guardado, menú, botín y vaciado al destruirse |
| `VehicleSpec`, `VehicleSpecs` | Datos de cada vehículo (tamaño, asientos, color, baúl, ruedas, tonos, bocina, inclinación, manejo) y catálogo `ALL` |
| `ModVehicles`, `VehicleItem` | Registro de la entidad y el ítem de cada `VehicleSpec` (desde `onInitialize`) e ítem que lo pone conservando color y nombre |
| `RunOverPusher`, `DismountLocator` | Atropello y empuje de criaturas (servidor, con la velocidad acotada) y punto por donde baja cada pasajero |
| `physics/VehiclePhysics` | Funciones puras del manejo: velocidad, volante, giro tipo bicicleta y derrape |
| `physics/VehicleMath` | Funciones puras de la entidad: rumbo, giro directo, salida, empuje, rodado, dirección visual y combustible |
| `physics/VehicleTuning`, `VehicleInput` | Parámetros de un vehículo (`WILLYS`, `ENDURO`, `MOTOCARRO`) con sus límites, y entrada inmutable del conductor |
| `physics/Suspension` | Resortes del cabeceo, el balanceo y el rebote (solo visual) |
| `GarageConfig` | Lee, completa, valida y respalda `config/garage.json` |
| `HornPayload`, `TuningSyncPayload` | Paquetes: bocina (cliente → servidor) y manejo del Jeep (servidor → cliente, al conectar) |
| `client/GarageClient` | Teclas del conductor, bocina, recepción del manejo y arranque del sonido del motor |
| `client/ClientVehicles`, `VehicleRenderer` | Tabla de modelos por vehículo y renderer común (carrocería teñida + resto sin teñir) |
| `client/CarModels` | Modelo del Jeep en dos capas, ruedas, suspensión y bandera |
| `client/MotorcycleModels` | Modelo de la moto XTZ en dos capas, horquilla inclinada (rake), ruedas y tumbada (lean) |
| `client/MotocarroModels` | Modelo del motocarro en dos capas, tren delantero Delta, suspensión y balanceo centrífugo |
| `client/MotocargueroModels` | Modelo del motocarguero en dos capas: carenado y tanque teñibles, mampara con rejilla, platón con barandas y compuerta, y 3 ruedas |
| `client/EngineSound` | Motor en bucle cuyo tono sube con la velocidad según el rango de cada vehículo |

Para agregar otro vehículo:

1. Un `VehicleTuning` nuevo en `physics/` (o reutilizar uno) y una entrada en `VehicleSpecs` (incluida en `ALL`).
2. Su modelo (`*Models` con las capas `BODY` y `TRIM`) y una entrada en `client/ClientVehicles`.
3. Datos: textura de entidad, textura e `items/*.json` e ítem, receta y claves de `lang/` en los tres idiomas.

No hace falta tocar `VehiclePhysics`, `MotorVehicleEntity`, `ModVehicles` ni `GarageClient`. `ClientVehicles` falla al
arrancar si falta la entrada de algún vehículo.

Reglas del modelo: las piezas solo se tocan, nunca se solapan con caras en el mismo plano (si no, parpadean). Todo lo
que no son ruedas cuelga de `chassis`, para que la suspensión mueva la carrocería sin mover las ruedas.

### Pendiente

- El motor usa el sonido del minecart a falta de un sonido propio.
