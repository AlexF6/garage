# Changelog

Todos los cambios notables de este mod se documentan aquí. El formato sigue
[Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el versionado, [SemVer](https://semver.org/lang/es/).

## [Unreleased]

### Added
- Motocarro urbano (Bajaj RE / "Torito") de 3 asientos (1 conductor + 2 pasajeros): configuración Delta de 3 ruedas, baúl de 18 casillas, carrocería teñible con tintes vanilla (blanco glaciar por defecto), lona de techo negra y receta de crafteo.
- Física y cinemática del motocarro: balanceo centrífugo (*body roll*) hacia el exterior de las curvas, radio de giro cerrado, suspensión reactiva, vibración en ralentí con el motor encendido y bocina aguda.
- Modelo nativo del motocarro en dos capas (`BODY` tintable y `TRIM` fija) con lienzo UV 256x256 (`MotocarroModels`, `MotocarroRenderer`).
- Moto Enduro (Yamaha XTZ 125) de 2 asientos en tándem: tuning ágil `CarTuning.ENDURO`, inclinación lateral dinámica (*lean angle*) al doblar en curvas, alforja de 9 casillas, color teñible con tintes (azul Yamaha Racing Blue por defecto) y receta de crafteo.
- Modelo 3D y renderizador nativo (`MotorcycleModels`, `MotorcycleRenderer`) en dos capas con horquilla inclinada (*rake* -24°), ruedas de radios (rin 21" / 18") y vibración en ralentí.
- Herencia común en `CarEntity`, `CarItem` y `CarEngineSound` para compartir colisiones, inventario, combustible, atropellos (`runOver`) y sonido de motor parametrizado entre todos los vehículos.

## [1.0.0]

### Added
- Jeep Willys "Yipao" de 4 asientos: combustible, baúl de 27 espacios, colores con tintes, bocina, atropello y empuje
  de criaturas, y sin daño de caída para los pasajeros.
- Física de manejo (tracción, dirección según la velocidad, derrape con freno de mano) y suspensión visual.
- Configuración en `config/garage.json` (combustible, volumen del motor, manejo y atropello).
- Traducciones en inglés, español de España y español de Argentina.
