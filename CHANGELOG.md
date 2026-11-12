# Changelog

Todos los cambios notables de este mod se documentan aquí. El formato sigue
[Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el versionado, [SemVer](https://semver.org/lang/es/).

## [Unreleased]

### Added
- Motocarguero (trimoto de carga 200cc): 1 asiento con respaldo, platón de carga de 54 casillas (cofre de 6 filas), carenado y tanque teñibles (blanco glaciar por defecto), mampara con rejilla, barandas y compuerta, manejo `VehicleTuning.MOTOCARGUERO` (~47 km/h), receta y traducciones.
- Los baúles admiten de 1 a 6 filas (antes, hasta 3).
- `VehicleResourcesTest` y `ClientVehiclesTest` comprueban que cada vehículo de `VehicleSpecs.ALL` tenga traducciones, ítem, modelo, textura, receta y modelo de cliente: lo que falte ya no se descubre jugando.
- Motocarro urbano (Bajaj RE / "Torito") de 3 asientos (1 conductor + 2 pasajeros): configuración Delta de 3 ruedas, baúl de 18 casillas, carrocería teñible con tintes vanilla (blanco glaciar por defecto), lona de techo negra y receta de crafteo.
- Física y cinemática del motocarro: balanceo centrífugo (*body roll*) hacia el exterior de las curvas, radio de giro cerrado, suspensión reactiva, vibración en ralentí con el motor encendido y bocina aguda.
- Modelo nativo del motocarro en dos capas (`BODY` tintable y `TRIM` fija) con lienzo UV 256x256 (`MotocarroModels`).
- Moto Enduro (Yamaha XTZ 125) de 2 asientos en tándem: tuning ágil `VehicleTuning.ENDURO`, inclinación lateral dinámica (*lean angle*) al doblar en curvas, alforja de 9 casillas, color teñible con tintes (azul Yamaha Racing Blue por defecto) y receta de crafteo.
- Modelo 3D y renderizador nativo (`MotorcycleModels`) en dos capas con horquilla inclinada (*rake* -24°), ruedas de radios (rin 21" / 18") y vibración en ralentí.
- Base común (`MotorVehicleEntity`, `VehicleItem`, `EngineSound`) para compartir colisiones, inventario, combustible, atropellos (`runOver`) y sonido de motor parametrizado entre todos los vehículos.

### Changed
- La animación visual sale de `MotorVehicleEntity` a `physics/VehicleAnimation`, con pruebas, y la entidad queda más corta.
- `engineVolume` pasa de `garage.json` a un `config/garage-client.json` propio, que solo lee el cliente: es un ajuste de cliente y no debía mezclarse con las reglas del servidor. El valor que hubiera en `garage.json` se migra la primera vez.
- `GarageConfig` pasa al paquete `config` y los paquetes de red (`HornPayload`, `TuningSyncPayload`, `SyncedTunings`) a `network`, como pide la guía de estructura.
- La descripción del mod en `fabric.mod.json` pasa a inglés y su traducción (`modmenu.descriptionTranslation.garage`) vive en los tres archivos de idioma.
- `RunOverRules` (en `physics/`) agrupa la sección `runOver` de la config; `ModelKit.IdleVibration` comparte el ralentí de los modelos; `DismountLocator` ya no cambia la postura del pasajero por su cuenta.
- Un solo `MotorVehicleEntity` para todos los vehículos, descrito por un `VehicleSpec` (`VehicleSpecs`): añadir uno ya no obliga a tocar el registro, el ítem ni el arranque del cliente. `MotorcycleEntity` y `MotocarroEntity` desaparecen; los identificadores de entidad, ítem y config no cambian, así que las partidas guardadas siguen funcionando.
- Un solo `VehicleRenderer` (con `ClientVehicles`) en lugar de tres renderers casi idénticos; los modelos comparten atajos en `ModelKit`.
- Los vehículos se declaran con `VehicleSpec.builder`, que nombra cada dato en lugar de un constructor de 14 números seguidos.
- Los registros pasan a `ModVehicles.register()`, llamado desde `onInitialize`.
- `CarEntity`, `CarItem`, `CarConfig` y `CarEngineSound` pasan a llamarse `MotorVehicleEntity`, `VehicleItem`, `GarageConfig` y `EngineSound` porque sirven a todos los vehículos.
- `VehiclePhysics`, `VehicleTuning` y `VehicleInput` (antes `Car*`) y el baúl (`ChestVehicleEntity`) salen de la entidad; el registro de renderers usa `EntityRenderers`, sin la API obsoleta de Fabric.
- El atropello y la salida de pasajeros salen de la entidad a `RunOverPusher` y `DismountLocator`; la matemática sin Minecraft, a `VehicleMath` (con pruebas).
- `installMod` y `exportMod` ya no se enganchan a `assemble`: se ejecutan a mano. La versión de JUnit se mueve a `gradle.properties` y se añade `.editorconfig`.
- El README documenta el rango de cada opción.
- `VehicleSpec` valida sus datos al crearse y `ModVehicles` rechaza ids repetidos: un dato imposible detiene el arranque en lugar de dar un vehículo roto.
- El tope de velocidad del atropello vive en `RunOverMath`, probado, y `VehicleTuning` se serializa con `toArray`/`fromArray`.

### Fixed
- Un valor `null` en una clave simple de `garage.json` (`"maxFuel": null`) conserva el valor por defecto en vez de leerse como 0.
- `VehicleSpec.Horn` rechaza al crearse una bocina sin sonido o con volumen no positivo, en vez de fallar en el primer bocinazo.
- Los jugadores de la moto se sientan sobre el sillín: el anclaje estaba 0.4 bloques por debajo y quedaban hundidos en el motor.
- Un `garage.json` con bytes que no son UTF-8 se trata como corrupto (`.bak` y regeneración), y la config se escribe de forma atómica.
- El servidor decide si el tanque está lleno al repostar; el cliente ya no lo adivina con su config local.
- Un `garage.json` corrupto ahora se guarda como `garage.json.bak` y se regenera con los valores por defecto, en lugar de dejarlo intacto y jugar sin avisar.
- Los valores fuera de rango (`maxFuel`, `fuelPerItem`, `engineVolume` y `runOver`) se corrigen **y se avisan en el log**.
- `maxFuel` y `fuelPerItem` tienen tope (1 000 000): antes un valor enorme desbordaba el cálculo del porcentaje de combustible.
- Un número fuera del rango de `int` en la config se satura en lugar de dar la vuelta.
- `VehicleTuning` rechaza valores por encima de límites razonables (velocidad, aceleración, distancia entre ejes, etc.) y NaN.
- El servidor acota la velocidad medida en los atropellos a la `maxSpeed` del vehículo, y su zona de búsqueda con ella.
- Un atropello con `pushSpeed` en 0 y el vehículo parado ya no genera una dirección NaN.
- El manejo de cada vehículo lo manda el servidor al conectar (`TuningSyncPayload`) y el cliente deja de usar el suyo. Lo recibido vive en `SyncedTunings`, que solo usa el cliente, así que en un mundo integrado el servidor nunca lee lo que recibió su cliente.
- Cada vehículo suelta su propio ítem (`getDropItem` ya no cae por defecto en el Jeep).
- Un vehículo aparcado (sin conductor, en el suelo y quieto) solo recalcula su física cada 10 ticks, que basta para detectar que le quitaron el suelo.
- El servidor ya no calcula animaciones (ruedas, dirección, inclinación) que solo se ven en el cliente.

## [1.0.0] - 2026-11-10

### Added
- Jeep Willys "Yipao" de 4 asientos: combustible, baúl de 27 espacios, colores con tintes, bocina, atropello y empuje
  de criaturas, y sin daño de caída para los pasajeros.
- Física de manejo (tracción, dirección según la velocidad, derrape con freno de mano) y suspensión visual.
- Configuración en `config/garage.json` (combustible, volumen del motor, manejo y atropello).
- Traducciones en inglés, español de España y español de Argentina.
