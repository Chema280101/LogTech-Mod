# Plan de Desarrollo: LogTech v0.3

## Fase 1 — Preparación

### T43. Preparación
- **Objetivo:** Partir de una base limpia y tener mundos para probar la migración.
- **Pasos:**
  1. Revisa los problemas conocidos de la v0.2. Corrige los que pierdan items o causen crashes, y pasa los demás a issues.
  2. Crea la rama `v0.3` y pon la versión `0.3.0-dev` en `gradle.properties`.
  3. Guarda una copia de un mundo de la v0.2 con una red grande, máquinas, reglas, pantallas y energía por red. Conserva también el de la v0.1: probarás las dos cadenas de migración (v0.1 → v0.3 y v0.2 → v0.3).
  4. Actualiza `AGENTS.md`: estado actual (v0.2 completa) y las reglas nuevas: el calor y los efectos del firmware viven en clases Java puras, toda acción de jugador pasa por `canPlayerDo`, los valores nuevos van en Config, y los mundos de la v0.1 y la v0.2 deben seguir cargando.
  5. Crea `canPlayerDo(player, action)` con un enum de acciones y una respuesta fija de «sí». Aún no se conecta a nada.
  6. Copia este plan a `docs/plan-v0.3.md`.
- **Criterio:** `gradlew build` pasa desde una rama limpia, los dos mundos de prueba están respaldados y `AGENTS.md` está actualizado.

## Fase 2 — Almacenamiento seguro

### T44. Papelera
- **Objetivo:** Una red de seguridad para los ítems destruidos o descartados.
- **Implementación:**
  1. Pestaña «Papelera» en el Terminal y botón «Destruir» que manda a la papelera el ítem del cursor.
  2. La papelera del Núcleo guarda lo destruido con su hora de juego (`gameTime`), agrupado por tipo de ítem. Aguanta hasta 4 096 ítems durante 36 000 ticks (`trashCapacity` y `trashLifetimeTicks` en configuración). Al llenarse o caducar, se elimina lo más antiguo en orden FIFO.
  3. Restaurar: un clic en una entrada devuelve el ítem a la red si hay espacio; si no, se queda en la papelera.
  4. Acción nueva de reglas «Descartar excedente» (`DISCARD_EXCESS`): manda a la papelera lo que pase de $N$ una vez por ciclo sin parpadeo.
  5. Se guarda en el Núcleo con `data_version: 4`. Los mundos antiguos empiezan con la papelera vacía.
  6. Toda interacción validada en el servidor y sujeta a `canPlayerDo(player, action)`.
- **Criterio:** Destruyes 64 ítems y aparecen en la papelera; restaurarlos los devuelve; pasado el tiempo caducan. La regla de descarte deja el ítem en $N$ y lo sobrante aparece en la papelera. Todo persiste al reiniciar.

### T45. Cuarentena
- **Objetivo:** Que nada se pierda ni se atasque cuando la red no puede aceptar items.
- **Implementación:**
  1. Cuando la red está llena, sobrecargada o apagada, los ítems que llegan por la capability de inserción del Backbone (tuberías, tolvas y Perforadoras) entran en un búfer de cuarentena de 1 024 ítems (`quarantineCapacity` en Config) en lugar de ser rechazados.
  2. Cuando vuelve a haber espacio y la red está encendida, la cuarentena se vacía sola hacia la red cada 20 ticks con un límite por ciclo (`quarantineDrainPerCycle = 64` en Config). Solo se descuentan de cuarentena los ítems que la red realmente acepta, evitando duplicaciones y pérdidas.
  3. Si el búfer de cuarentena también se llena, la capability rechaza como antes.
  4. `insertItem` con `simulate=true` respeta el espacio de la cuarentena sin mutar nada.
  5. La pestaña «Estadísticas» en el Terminal muestra «Cuarentena: N items» en ámbar (`0xFFFFA726`).
  6. Nueva condición en `RuleEngine`: `QUARANTINE_GREATER` (ítems en cuarentena mayor que $N$) con histéresis anti-parpadeo del 5%.
  7. Se guarda en el Núcleo con `data_version: 5`. Desactivable en Config (`quarantineEnabled`), volviendo al comportamiento previo.
  8. Lógica del búfer desacoplada en clase Java pura (`QuarantineBuffer`) con pruebas unitarias en JUnit 5.
- **Criterio:** Apagas la red (sin energía) con una Perforadora trabajando: el mineral entra en cuarentena y no se pierde nada. Al volver la energía, la cuarentena se vacía hacia la red. Con el búfer lleno, la capability rechaza. No hay duplicación al vaciarla.

### T46. Compactar
- **Objetivo:** La acción de reglas «compactar», que convierte excedentes de ítems a su forma compacta de forma atómica y simétrica.
- **Implementación:**
  1. Detección automática de pares reversibles (`CompactionDetector`) a partir de recetas de crafteo (`3x3` o `2x2` con 1 salida y receta inversa de 1 ítem devolviendo exactamente 9 o 4 ítems).
  2. Filtro estricto: solo ítems sin componentes de datos especiales (sin pureza) y con lista negra en Config (`compactBlacklist`).
  3. Recálculo al arrancar el servidor (`ServerStartedEvent`) y al recargar datapacks (`OnDatapackSyncEvent` y `AddReloadListenerEvent`).
  4. Acción `COMPACT` en `RuleAction` y `AutomationRule`: solo válida con condición `ITEM_COUNT_GREATER`.
  5. Conversión atómica desacoplada en Java puro (`CompactionLogic`): planifica extracciones e inserciones respetando el límite por ciclo de 20 ticks (`compactMaxConversionsPerCycle = 64`). Si alguna inserción no cupiera, restituye de forma atómica los ítems base correspondientes.
  6. Suite completa de pruebas unitarias en JUnit 5 (`CompactionLogicTest`, `CompactionDetectorTest`, `RuleEngineTest`).
- **Criterio:** Con 5 000 lingotes de hierro y regla «mayor que 1 000 → compactar», el excedente pasa a bloques de hierro y quedan 1 000 lingotes. Los ítems con pureza no se tocan. Con recetas no reversibles o dudosas, no se compacta.

## Fase 3 — Experiencia de usuario y telemetría

### T47. Vistas guardadas y filtros
- **Objetivo:** Filtros por nivel (todos, caliente, frío) y pureza mínima (0-5) con hasta 8 vistas guardadas por red.
- **Implementación:**
  1. Filtros cliente por nivel y pureza sobre la lista recibida sin mutar la red ni la fuente del servidor.
  2. Hasta 8 vistas por red persistidas en el Núcleo con `data_version: 6`.
  3. Parámetros puros (nombre máx. 20 chars, búsqueda máx. 50 chars, orden por cantidad o nombre, nivel y pureza mínima).
  4. Doble sanitización estricta (cliente y servidor) contra caracteres de control y desbordamientos.
  5. Modificaciones validadas en servidor con `canPlayerDo(player, PlayerAction.MANAGE_VIEWS)`.
  6. Botones [1..8] en interfaz sobre las pestañas con carga directa al hacer clic y guardado mediante Shift+clic, sincronizados bidireccionalmente hacia todos los jugadores conectados.
  7. Clases puras desacopladas de Minecraft y testeables al 100% en JUnit 5 (`ViewManagerTest`).
- **Criterio:** Guardas una vista con Shift+clic, filtras por frío o pureza, y al recargar o pulsar el botón se restaura exactamente. Los cambios se sincronizan a todos los jugadores que tengan el Terminal abierto.

### T48. Estimaciones y tendencias
- **Objetivo:** Que el Terminal indique cuánto falta para llenarse o agotarse la red y los ítems individuales.
- **Implementación:**
  1. Clase Java pura `RateTracker` desacoplada de Minecraft con 5 cubos de un minuto (`MinuteBucket`).
  2. Se actualiza en eventos de inserción y extracción del Núcleo (`insert`, `extract`, `extractHot`, `extractCold`, descarte de excedente a papelera) y agrupa al final de cada minuto (1 200 ticks) sin trabajo por ítem durante ticks normales.
  3. Poda automática de ítems sin cambios recientes tras salir de la ventana de 5 minutos, eliminación inmediata de cancelaciones netas cero en el minuto y límite de seguridad configurable en `LogTechConfig` (`rateTrackerMaxItems` = 10 000).
  4. Totalmente efímero en memoria (RAM), no se guarda en disco ni en NBT.
  5. Pestaña «Estadísticas» muestra estimaciones de llenado («se llena en X» o «estable» si ritmo $\le 0$) para total y por niveles con tooltip detallado.
  6. Tooltip de cada ítem en el Terminal muestra su tendencia («+120/min», «−45/min» o «0/min») y aviso de agotamiento («se agota en X»).
  7. Cobertura completa de pruebas unitarias en JUnit 5 (`RateTrackerTest`).
- **Criterio:** Con una Perforadora llenando la red, las estimaciones cuadran con lo que sucede. Un ítem consumido baja y avisa de cuándo se agota. Con miles de tipos de ítems, la memoria no crece sin límite.

## Fase 4 — Calor y firmware

### T49. Modelo de calor
- **Objetivo:** Que cada máquina tenga una temperatura que sube al trabajar, y que el sobrecalentamiento tenga consecuencias.
- **Implementación:**
  1. Clase Java pura `HeatModel`: calor de 0 a 100, actualizado cada 20 ticks (1 s) con multiplicador de generación, enfriamiento/disipación proporcional y si la máquina está trabajando. Con la generación base, ninguna máquina se sobrecalienta trabajando normalmente (~50 °C de equilibrio).
  2. Por encima de 70 °C, la máquina trabaja a media velocidad (0.5x) y consume la mitad de energía por tick.
  3. Al llegar a 100 °C se apaga (`MachineStatus.OVERHEATED`, causa «sobrecalentada»), no consume energía y pide 0 en `requestedEnergy()`. No se reanuda hasta descender a $\le 40$ °C (histéresis anti-parpadeo).
  4. Integrado en el Triturador, la Lavadora, la Centrífuga y la Perforadora. La velocidad se aplica al progreso por tick (alternancia de ticks), no a la receta.
  5. Persistencia en BlockEntities con `data_version: 3` (`Heat` y `Overheated`), retrocompatible con mundos de v0.1 y v0.2.
  6. Visualización con barra vertical medidora de calor y tooltip en las pantallas de máquinas (`CrusherScreen`, `WasherScreen`, `CentrifugeScreen`), columna de calor en la lista de máquinas de la pestaña «Estadísticas» del Terminal y visualización en HUD de Jade.
  7. Constantes de balance centralizadas en `LogTechConfig` (`heatBaseGeneration`, `heatCoolingRate`, `heatSlowdownThreshold`, `heatOverheatThreshold`, `heatResumeThreshold`, `heatSlowdownSpeed`).
  8. Suite de pruebas unitarias en JUnit 5 (`HeatModelTest` y `MigrationTest`).
- **Criterio:** Las pruebas JUnit cubren la subida, la ralentización a 70, el apagado a 100 y la reanudación a 40. En el juego, con la generación de calor subida en la configuración, una máquina se sobrecalienta, se detiene mostrando la causa y vuelve sola al enfriarse.

### T50. Disipación
- **Objetivo:** Que el jugador pueda enfriar las máquinas colocando bloques disipadores o mediante contacto con agua.
- **Implementación:**
  1. Bloque disipador (`heatsink`, `ModBlocks.HEATSINK`, `HeatsinkBlock`), con receta de crafteo usando barras de hierro, lingotes de neoferrita y lingotes de hierro.
  2. Cálculo de enfriamiento extra a partir de los 6 vecinos directos: cada disipador suma un valor (`heatHeatsinkCooling` = 1.0 en Config) y cada cara con agua (fuente o corriente detectada vía `FluidState`) suma un valor menor (`heatWaterCooling` = 0.5 en Config).
  3. Recálculo desacoplado en `DissipationLogic` ejecutado únicamente en `neighborChanged`, `updateShape`, `onPlace` y `onLoad`, guardado en caché en la máquina (`cachedCooling`) y pasado a su `HeatModel`. Nunca se escanean vecinos en cada tick.
  4. Pantalla de máquina (`CrusherScreen`, `WasherScreen`, `CentrifugeScreen`) muestra el enfriamiento actual en el tooltip de la barra de calor y en el HUD de Jade.
  5. Objetivo de balance verificado: con Turbo (multiplicador 2.2) y sin refrigeración se sobrecalienta en aproximadamente un minuto (57 s desde 50 °C), y con dos disipadores (enfriamiento = 4.0) aguanta indefinidamente a 50.6 °C (< 70 °C, sin ralentización).
  6. Suite de pruebas unitarias en JUnit 5 (`DissipationLogicTest`).
- **Criterio:** La máquina enfría más con un disipador pegado, con agua o con ambos. Quitar o añadir un bloque vecino se refleja al momento. Un mundo de la v0.2 abre sin cambios.

### T51. Firmware
- **Objetivo:** Chips que modifican el comportamiento, rendimiento y automatización de las máquinas.
- **Implementación:**
  1. Ranura de firmware en el Triturador, la Lavadora, la Centrífuga y la Perforadora (solo acepta chips de firmware, inaccesible por automatización externa de tolvas/tuberías).
  2. Implementada interfaz gráfica para la Perforadora (`DrillMenu`, `DrillScreen`) para acceder a su búfer y ranura de firmware, conservando Shift + clic derecho para cambiar la prioridad de energía.
  3. Efectos desacoplados en clase Java pura `FirmwareEffects` y enum `FirmwareType`:
     - **Eco:** velocidad ×0.7, energía ×0.5, calor ×0.5.
     - **Turbo:** velocidad ×1.5, energía ×2.0, calor ×3.0.
     - **Seguro:** velocidad 1.0x sin ralentización intermedia hasta 85 °C. A 85 °C se apaga por sobrecalentamiento y se reanuda al enfriar a $\le 30$ °C. Nunca trabaja lento: o va a tope o está parada.
     - **Selectivo (Lavadora y Centrífuga):** la extracción por automatización externa (tolvas/tuberías) solo permite extraer el ítem configurado. Se elige con Shift + clic derecho con el chip en la mano principal y el ítem en la mano secundaria, persistido en el data component `logtech:firmware_filter`. La extracción manual en la pantalla permanece irrestricta.
  4. Multiplicadores y umbrales configurables centralizados en `LogTechConfig`.
  5. `requestedEnergy()` refleja fielmente el multiplicador de energía del firmware instalado.
  6. Telemetría sincronizada en `TerminalStatsPayload`: la pestaña «Estadísticas» del Terminal muestra el badge o nombre del firmware junto al calor de cada máquina.
  7. Integración con Jade mostrando el firmware instalado en el tooltip de la máquina.
  8. Recetas de crafteo temáticas usando Cristal de datos (`data_crystal`) y Fragmentos de cristal (`crystal_shard`) con recursos y modelos generados por datagen y texturas pixel-art 16x16.
  9. Persistencia retrocompatible con `data_version: 4` en todas las máquinas.
  10. Suite completa de pruebas unitarias en JUnit 5 (`FirmwareEffectsTest`, `MigrationTest`).
- **Criterio:** Cada chip altera la máquina según su diseño. Con Turbo y refrigeración adecuada, produce un 50% más rápido. Con Selectivo en la Lavadora o Centrífuga, una tolva debajo solo extrae el ítem configurado. Mundos antiguos abren con ranuras vacías sin fallos.

### T52. Llave de configuración
- **Objetivo:** Copiar los ajustes de un bloque y pegarlos en otros del mismo tipo.
- **Implementación:**
  1. Ítem `config_key` (`ModItems.CONFIG_KEY`, `ConfigKeyItem`), apilable a 1, con receta de crafteo temática en `ModRecipeProvider` (lingote de neoferrita, polvo de redstone y lingote de hierro).
  2. Interfaz `Configurable` (`getConfigType`, `exportConfig`, `importConfig`) y especialización `MachineConfigurable` para máquinas con prioridad de energía.
  3. Soporte en bloques:
     - **Máquinas (Triturador, Lavadora, Centrífuga, Perforadora):** tipo `"machine"`, copia y pega `energy_priority` (`HIGH`, `NORMAL`, `LOW`).
     - **Alarma (`AlarmBlockEntity`):** tipo `"alarm"`, copia y pega el canal de redstone (1 a 4).
     - **Núcleo (`CoreBlockEntity`):** tipo `"core"`, copia y pega las reglas de automatización y las políticas de fijado de ítems con validación estricta de versión idéntica (`data_version: 6`) y límites seguros contra cargas manipuladas.
  4. Interacción: Shift + clic derecho copia ajustes a la llave; clic derecho en bloque del mismo tipo los pega.
  5. Componente de datos `ModDataComponents.CONFIG_DATA` almacenando `ConfigPackage` con tipo y `CompoundTag`.
  6. Mensajes en la barra de acción para copiado, pegado, tipo incompatible, llave vacía y versión incompatible.
  7. Tooltip dinámico en la llave detallando el tipo y los valores almacenados o indicando estado vacío.
  8. Validación de seguridad en servidor mediante `canPlayerDo(player, PlayerAction.PASTE_CONFIG)`.
  9. Suite de pruebas unitarias en JUnit 5 (`ConfigKeyTest`).

### T53. Permisos de acceso
- **Objetivo:** Que una red pueda ser privada, con permisos por jugador y control estricto de accesos.
- **Implementación:**
  1. Modelo de seguridad desacoplado en clases Java puras: `NetworkSecurityManager`, `PlayerAccessEntry`, enums `SecurityMode` (`OPEN`, `PRIVATE`) y `SecurityPermission` (`VIEW`, `EXTRACT`, `INSERT`, `ADMIN`).
  2. Propietario (`ownerUuid`, `ownerName`): El jugador que coloca el Núcleo (`Block.setPlacedBy`). Los núcleos antiguos de v0.1 y v0.2 no tienen propietario (`ownerUuid == null`) y pueden ser reclamados con un botón «Reclamar» por cualquier jugador.
  3. Modos de red: `OPEN` (abierta por defecto, comportamiento histórico) o `PRIVATE` (restringida a propietario, operadores y lista de permisos).
  4. Permisos por jugador: `VIEW` (ver inventario en Terminal), `EXTRACT` (extraer ítems y pedidos en frío), `INSERT` (insertar ítems manual o tolva/tubería en Core/Backbone), y `ADMIN` (administrar reglas, accesos, vistas y ajustes del Núcleo).
  5. Los operadores del servidor (nivel de permisos 2 o más) y el propietario siempre tienen todos los permisos para evitar redes huérfanas o bloqueos.
  6. Pestaña «Acceso» en el menú del Núcleo (`CoreScreen` y `CoreMenu`), visible únicamente para jugadores con permiso `ADMIN` o nivel OP $\ge 2$, con lista interactiva de jugadores autorizados, conmutadores de permisos `[V]`, `[E]`, `[I]`, `[A]`, botón de eliminación `[×]`, selector de jugadores conectados en el servidor y botón «Añadir».
  7. Conexión estricta de `canPlayerDo(player, action)` y `PlayerAction` en todas las acciones:
     - Apertura de Terminal y Core (`OPEN_TERMINAL`, `OPEN_CORE`).
     - Extracción e inserción de ítems (`EXTRACT_ITEM`, `INSERT_ITEM`).
     - Fijación de nivel y políticas (`PIN_TIER`).
     - Papelera (`TRASH_ITEM`, `RESTORE_ITEM`).
     - Reglas de automatización (`CONFIGURE_RULES`).
     - Gestión de vistas (`MANAGE_VIEWS`).
     - Prioridades de energía (`CHANGE_PRIORITY`).
     - Llave de configuración (`PASTE_CONFIG`).
     - Reclamar núcleo (`CLAIM_CORE`) y gestionar accesos (`MANAGE_ACCESS`).
     - Rotura del Núcleo (`BREAK_CORE`): en redes privadas, cancela el evento `BlockEvent.BreakEvent` para jugadores sin permiso `ADMIN`.
  8. Revocación en tiempo real: Si se retiran permisos a un jugador mientras tiene un contenedor abierto, `CoreBlockEntity.closeRevokedPlayerMenus()` y `stillValid` cierran inmediatamente su interfaz de `TerminalMenu` o `CoreMenu`.
  9. Persistencia retrocompatible en el Núcleo con `data_version: 7` (`SecurityNbtAdapter`). Redes de mundos previos v0.1 y v0.2 cargan en modo `OPEN` sin propietario, preservando compatibilidad absoluta.
  10. Suite de pruebas unitarias en JUnit 5 (`NetworkSecurityManagerTest`, `NetworkSecurityTest`, `MigrationTest`).
- **Criterio:** En red privada, un jugador sin permisos no puede abrir el Terminal, ni extraer, ni insertar, ni romper el Núcleo. Quitar permisos cierra la pantalla en el acto. Un operador puede intervenir siempre.

### T54. Registro de actividad
- **Objetivo:** Saber quién hizo qué en una red.
- **Implementación:**
  1. Modelo de registro desacoplado en clases Java puras: `ActivityAction` (`INSERT`, `EXTRACT`, `DESTROY`, `RESTORE`, `CHANGE_RULES`, `CHANGE_ACCESS`), `ActivityLogEntry` (registro inmutable con hora de juego, UUID y nombre del jugador, acción, item y cantidad) y `ActivityLogBuffer` (búfer circular con agrupación temporal y filtrado paginado).
  2. Agrupación temporal inteligente: Las acciones repetidas del mismo jugador, ítem y tipo en menos de 10 segundos (200 ticks) se agrupan en una sola entrada acumulando la cantidad y refrescando la marca temporal, incluso ante acciones intercaladas de otros jugadores.
  3. Búfer circular de 500 entradas configurables (`LogTechConfig.ACTIVITY_LOG_CAPACITY`), descartando FIFO las más antiguas al llenarse. Desactivable en configuración (`LogTechConfig.ACTIVITY_LOG_ENABLED`).
  4. Persistencia en el Núcleo con `data_version: 8` (`ActivityLogNbtAdapter`). Mundos antiguos inician con registro vacío sin pérdida ni corrupción de datos.
  5. Auditoría de acciones de jugador: Registro exclusivo de acciones directas de jugadores (`CoreBlock` inserción manual, `TerminalMenu` inserción, extracción y papelera, `CoreMenu` reglas, `CoreAccessUpdatePayload` accesos, `CoreClaimPayload` reclamar). La automatización (tolvas, perforadoras, máquinas, reglas, cuarentena, compactación) NO se registra.
  6. Pestaña «Registro» en el Terminal: Visible exclusivamente para el propietario, administradores y operadores (`NetworkSecurity.canPlayerDo(player, PlayerAction.VIEW_LOG)`). Lista cronológica paginada (10 entradas por página) con navegación `<` y `>`, rueda del ratón y filtro dinámico por nombre o UUID del jugador con búsqueda insensible a mayúsculas.
  7. Paquetes de red acotados: Consulta bajo demanda (`TerminalLogRequestPayload`) y respuesta paginada (`TerminalLogPagePayload`) enviando solo los 10 registros de la página actual, manteniendo paquetes de menos de 350 bytes.
  8. Suite completa de pruebas unitarias en JUnit 5 (`ActivityLogBufferTest`, `MigrationTest`).
- **Criterio:** Dos jugadores operan en la red y el registro muestra a cada uno con sus acciones agrupadas. Un jugador que saca 64 ítems diez veces seguidas ocupa pocas entradas. Al llegar a 500, desaparecen las más antiguas. El registro persiste al reiniciar.

## Fase 5 — Cierre

### T55. Configuración y migración
- **Objetivo:** Cerrar la configuración y asegurar que los mundos de la v0.1 y v0.2 abren bien sin errores en el registro ni pérdida de ítems.
- **Implementación:**
  1. Auditoría completa de constantes de balance e integración centralizada en `LogTechConfig`:
     - `MAX_SAVED_VIEWS`: número máximo de vistas guardadas en el Terminal (default 8, rango 1..8).
     - `ACTIVITY_LOG_GROUPING_WINDOW_TICKS`: ventana temporal para agrupar acciones repetidas de jugadores en el registro (default 200 ticks = 10 s).
  2. Inventario exhaustivo de datos guardados y cadena de versiones documentado:
     - Núcleo (`CoreBlockEntity`): `data_version: 8`. Mundos de la v0.1 (`data_version < 2` o sin versión) trasladan el 100% de ítems al nivel caliente, iniciando nivel frío vacío y políticas en `AUTO`. Mundos de la v0.2 preservan caliente, frío y reglas. Papelera, cuarentena, vistas, seguridad (OPEN sin dueño) y registro de actividad inicializan vacíos/seguros.
     - Racks (`RackBlockEntity`, `ColdRackBlockEntity`): serialización `inventory` 100% retrocompatible.
     - Máquinas (`CrusherBlockEntity`, `WasherBlockEntity`, `CentrifugeBlockEntity`, `DrillBlockEntity`): `data_version: 4`. Mundos v0.1 adoptan `EnergyPriority.NORMAL`, calor en 0 y firmware vacío.
     - Vetas (`DepositSavedData`): estructura idéntica `VeinStates` preservando descubrimientos y reservas.
  3. Verificación de mundos de prueba v0.1 y v0.2 respaldados en disco y en `run\saves`.
  4. Suite exhaustiva de pruebas unitarias de ciclo completo en JUnit 5 (`MigrationTest`: `testFullMigrationChainV01ToV03AndReload`, `testFullMigrationChainV02ToV03AndReload` y `testMachineFullMigrationChainAndReload`), validando preservación de ítems, energía, reglas y re-apertura idéntica tras guardar en v0.3.
- **Criterio:** Ambos mundos abren y funcionan sin errores en el registro, y no quedan constantes de balance sueltas.
