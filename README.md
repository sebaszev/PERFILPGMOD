# Liberti Profile (Forge 1.20.1)

Perfil de jugador: **clic con la rueda del ratón sobre un jugador** abre su perfil. `/perfil` abre el tuyo.

> Estado: el código está escrito pero **no se ha podido compilar ni probar** (el entorno donde se escribió no
> llega a los servidores de Forge). Es normal que la primera compilación dé algún error pequeño; pásame el
> mensaje y lo corrijo.

## Compilar
1. Instala **Java 17** (JDK).
2. En esta carpeta: `./gradlew build` (Windows: `gradlew.bat build`).
3. El mod queda en `build/libs/libertiprofile-1.0.0.jar`.
4. Ponlo en la carpeta `mods` del **servidor y de todos los jugadores** (hace falta en ambos lados).

## Qué hace
- Marco de color (azul por defecto), jugador 3D que sigue al ratón, fondo gris por defecto.
- Líneas: salud, nivel de experiencia, tiempo de juego, jugadores eliminados (PvP).
- Etiquetas de rol arriba del cuadro (`[ADMIN]`...) y símbolo VIP si el jugador tiene el tag premium.
- Estampilla elegida visible para todos; la colección y el cambio de página solo los ve su dueño.
- Botón de espada: desafío PvP anunciado en el chat. Botón de corazón: 1 al día, se acumulan sin límite.
- Liberticoins bajo el cuadro (solo los ve el dueño; se puede abrir a todos en la config).

## Comandos
| Comando | Quién | Qué hace |
|---|---|---|
| `/perfil` | todos | abre tu perfil |
| `/perfil color <nombre o #RRGGBB>` | todos | color del marco |
| `/perfil marco <id>` / `/perfil marco ninguno` | todos | elegir un marco con diseño que te dieron |
| `/perfil admin fondo dar/quitar <jugadores> <id>` | admin (op 2) | conceder/retirar un fondo |
| `/perfil admin marco dar/quitar <jugadores> <id>` | admin | conceder/retirar un marco con diseño |
| `/perfil admin estampilla dar/quitar <jugadores> <id>` | admin | conceder/retirar una estampilla (sin límite) |
| `/perfil admin fondo/marco/estampilla lista <jugador>` | admin | ver lo que tiene |

Los fondos se cambian con las flechas de abajo a la derecha del cuadro del jugador; las estampillas, pulsando
su recuadro (otra pulsación la quita).

## Tus imágenes PNG: todo se gestiona en el SERVIDOR
Las imágenes **no van dentro del mod ni en los clientes**. Viven en la carpeta del servidor:

`config/libertiprofile/` con cuatro subcarpetas:

| Carpeta | Qué es | Medida recomendada |
|---|---|---|
| `backgrounds/` | fondos detrás del jugador | 176 x 262 px |
| `frames/` | diseños de recuadro (marco entero) | 528 x 369 px, centro transparente |
| `stamps/` | estampillas | 64 x 64 px, fondo transparente |
| `ui/vip.png` | símbolo premium | 48 x 28 px (se muestra a 24x14) |

- El **id** es el nombre del PNG sin `.png`, en minúsculas (letras, números, `_ - .`). Ej.: `stamps/calabaza.png` -> id `calabaza`.
- **Añadir**: sube el PNG a la carpeta y concédelo: `/perfil admin estampilla dar <jugador> <id>`.
- **Quitar**: `/perfil admin estampilla quitar <jugador> <id>` (y borra el PNG si ya no lo quieres).
- **Cambiar** una imagen (p. ej. el logo premium): sustituye el archivo, mismo nombre. Los jugadores la reciben
  la próxima vez que abran un perfil, sin reiniciar nada ni reinstalar el mod.
- `/perfil admin fondo|marco|estampilla archivos` lista los PNG que hay en esa carpeta.
- La primera vez que arranca, el mod copia unos ejemplos: fondo `tormenta`, marco `dorado`, estampillas `calabaza` y `cono`, y `ui/vip.png`.
- Los jugadores descargan las imágenes del servidor automáticamente (solo la mod hace falta en sus carpetas).

## Configuración (`config/libertiprofile-common.toml` en el servidor)
- `premiumTag` (por defecto `sr_vip`, el "VIP habilitado" de tu datapack de roles) y `premiumScoreboard` (opcional, p. ej. `bp_premium`).
- `roleLabels`: líneas `tagDelRol|ETIQUETA|#COLOR|tagDeMostrar`. Ya viene con los 9 roles de tu datapack (`sr_role_*`, colores de sus equipos).
  Arriba del cuadro sale el rol que el jugador eligió mostrar (`sr_show_*`); si lo ocultó (`sr_oculto`), no sale ninguno.
- `coinsSource`: `sdmshop` por defecto (lee la economía de SDMShop Rework). También `scoreboard:`, `nbt:`, `method:`, `none`.
- `showCoinsToOthers`, `pvpCooldownSeconds`, `heartOncePerTargetPerDay`, `defaultFrameColor`, `maxImageKB`.
