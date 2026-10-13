# Plan de Desarrollo: LogTech v0.2

## Fase 1 — Base

### T22. Preparación
- **Objetivo:** Partir de una base limpia y poder comprobar la migración.
- **Pasos:**
  1. Revisa los problemas conocidos de la v0.1. Corrige los que pierdan items o causen crashes, y pasa los demás a issues.
  2. Crea la rama `v0.2` y pon la versión `0.2.0-dev` en `gradle.properties`.
  3. Guarda una copia de un mundo de la v0.1 con una red grande (Núcleo, racks llenos, máquinas) en una carpeta fuera del proyecto. Es tu mundo de pruebas de migración.
  4. Actualiza `AGENTS.md`: estado actual (v0.1 completa) y las reglas nuevas: recetas de máquinas en JSON con tipos propios, pureza como data component, JEI, EMI y Jade opcionales, valores nuevos en Config, y que los mundos de la v0.1 deben seguir cargando.
  5. Copia este plan a `docs/plan-v0.2.md`.
- **Criterio:** `gradlew build` pasa desde una rama limpia, el mundo de pruebas está respaldado y `AGENTS.md` está actualizado.

---

### T23. Pureza
- **Objetivo:** Que el mineral lleve una pureza de 1 a 5 y que la Perforadora la herede de su veta.
- **Implementación:**
  1. Data component `logtech:purity` registrado con `DeferredRegister.DataComponents`, con codec persistente `Codec.intRange(1, 5)` y codec de red `ByteBufCodecs.VAR_INT`.
  2. Componente añadido por defecto (pureza 1) a `raw_neoferrite`, `raw_crystalite`, `neoferrite_dust` y `crystalite_dust`. Los lingotes y cristales de datos no tienen pureza.
  3. Tooltip cliente «Pureza: N/5» añadido en `ClientTooltipHandler` y traducido en `es_es`, `es_mx` y `en_us`.
  4. La Perforadora genera mineral crudo con la pureza determinista de la veta sobre la que trabaja y comprueba compatibilidad de búfer con `ItemStack.isSameItemSameComponents`.
  5. El Triturador conserva la pureza de entrada y la propaga a los polvos producidos, deteniéndose si el búfer de salida contiene polvo de otra pureza.
  6. Comprobada la compatibilidad con el Núcleo y Terminal: `ItemKey` distingue componentes, mostrando pilas de distinta pureza por separado. Ítems de mundos v0.1 sin componente se resuelven de forma retrocompatible como pureza 1.
- **Criterio:** Perforadora sobre veta pureza 4 produce mineral con tooltip «Pureza: 4/5», Triturador produce polvo de pureza 4, el Terminal muestra pilas separadas, y mundos v0.1 leen ítems existentes como pureza 1.

---

### T24. Recetas en JSON
- **Objetivo:** Pasar las recetas de las máquinas del código a recetas de datapack con tipos propios (compatibles con JEI, EMI y datapacks personalizados).
- **Implementación:**
  1. Modelo de datos común para máquinas de procesamiento: `ProcessingRecipe` implementando `Recipe<SingleRecipeInput>`, con enums Java puros `PurityMode` (`preserve`, `increase`) y `OutputScaling` (`none`, `inverse_purity`).
  2. Salida de receta estructurada en record `ProcessingOutput` (item, count, chance, scaling) con `Codec` y `StreamCodec`.
  3. Lógica matemática desacoplada en `ProcessingLogic` (clase Java pura) probada exhaustivamente con JUnit 5: cálculo de pureza de salida y escalado inverso de probabilidades para impurezas/subproductos.
  4. Tipos y serializadores registrados con `DeferredRegister`: `logtech:crushing` registrado en `ModRecipeTypes` y `ModRecipeSerializers` con `ProcessingRecipeSerializer` genérico extensible para futuras máquinas (`logtech:washing` y `logtech:centrifuging`).
  5. Datagen builder `ProcessingRecipeBuilder` implementando `RecipeBuilder` y recetas por defecto generadas en `data/logtech/recipe/crushing/` (`raw_neoferrite` y `raw_crystalite` a 2 polvos, 4000 FE total, 100 ticks = 40 FE/t, pureza `preserve`).
  6. Adaptado el `CrusherBlockEntity` y `CrusherMenu` para resolver recetas dinámicamente con `RecipeManager` manteniendo caché por tipo de ítem de entrada, propagando pureza mediante `assemble` y eliminando la clase estática hardcodeada `CrusherRecipes`.
- **Criterio:** El Triturador procesa minerales mediante el `RecipeManager`, admite sobrescritura o nuevas recetas vía datapack JSON sin tocar código, y las pruebas JUnit pasan en limpio.

---

### T25. Racks caliente y frío
- **Objetivo:** Introducir almacenamiento jerárquico caliente y frío sin duplicar código, preservando compatibilidad retroactiva.
- **Implementación:**
  1. Enum `StorageTier` (`HOT`, `COLD`) y clase Java pura `SimpleTier` y `TierStorage` que gestiona inserción, extracción, sobrecarga y persistencia NBT por nivel.
  2. Rack original preservado como nivel caliente (`rack`), y añadido `cold_rack` (`ModBlocks.COLD_RACK`, `ColdRackBlockEntity`) con 4x capacidad por módulo (16.384 items por módulo configurable con `coldDataModuleCapacity`).
  3. Inserción jerárquica en el Núcleo: caliente primero y, si se satura, desborde a frío. Extracción unificada y sobrecarga calculada por nivel.
  4. Migración de mundos v0.1 mediante `data_version`: mundos antiguos sin versión cargan el 100% de ítems en caliente.
  5. Terminal adaptado mostrando tooltip desglosado con cantidades en nivel caliente y frío.
- **Criterio:** Racks calientes preservan comportamiento de v0.1, racks fríos expanden la capacidad global recibiendo desbordes y mundos v0.1 cargan intactos.

---

### T26. Política automática caliente y frío
- **Objetivo:** Que el Núcleo rebalancee ítems entre niveles caliente y frío según su uso y políticas de fijación fijadas por el usuario.
- **Implementación:**
  1. Enum `ItemPinMode` (`AUTO`, `FIJAR_CALIENTE`, `SOLO_FRIO`).
  2. Lógica pura desacoplada en `TierPolicyLogic` y `SimpleTier` con tests JUnit 5 (`TierPolicyTest`):
     - Puntuación en memoria (+1 por extracción exitosa) sujeta a decaimiento por ciclo (* 0.9).
     - Ascenso de frío a caliente de ítems de alta puntuación o `FIJAR_CALIENTE` mientras haya espacio en caliente.
     - Descenso de caliente a frío de ítems `SOLO_FRIO` y de ítems de menor puntuación cuando el caliente supera el 90% hasta quedar <= 80%.
     - Límite de procesamiento acotado a $N$ ítems distintos por ciclo (configurable en `tierPolicyMaxItemsPerCycle`).
     - Transferencia atómica de ítems con conservación matemática estricta (suma total invariante antes y después del ciclo).
  3. Núcleo (`CoreBlockEntity`): ciclo periódico cada 200 ticks (`tierPolicyCycleInterval`), persistencia de modos fijados en NBT (`item_policies`) con `data_version: 2`, puntuaciones volátiles en memoria y log de depuración configurable (`tierPolicyDebugLog`).
  4. Terminal: conmutación de políticas en el inventario con `Ctrl + Clic derecho` enviando `TerminalActionPayload.ActionType.CYCLE_PIN_POLICY` validado en el servidor, sincronización mediante `TerminalSyncPayload` y tooltip con modo y pista de atajo de teclado traducidos en español e inglés.
- **Criterio:** Ítems extraídos frecuentemente se mantienen en caliente, ítems en desuso bajan al frío al llenarse el caliente, ítems fijados respetan su nivel sin moverse, sumas invariantes y sin lag con miles de tipos de ítems.

---

### T27. Recuperación desde el frío
- **Objetivo:** Que extraer del frío tarde un poco y se visualice en el Terminal con barra de progreso, cuenta atrás y prevención estricta de duplicación.
- **Implementación:**
  1. Opciones en `LogTechConfig`: `coldRetrievalDelayTicks` (40 ticks por defecto = 2s) y `coldRetrievalMaxItems` (64 ítems por defecto).
  2. Lógica pura desacoplada: `ColdOrder` y `ColdRetrievalLogic` evaluando entregas atómicas ("entrega lo que quede", cancelación si inventario lleno sin restar del frío, y anti-duplicación). Pruebas exhaustivas en `ColdRetrievalTest` (JUnit 5).
  3. Extracción jerárquica en Terminal: el Núcleo (`extractHot`) toma inmediatamente lo disponible en caliente. Si falta, crea un pedido por el resto (hasta 64) en cola en memoria en `TerminalMenu`.
  4. Red y Sincronización: `ColdRetrievalPayload` sincroniza ítem, cantidad, cuenta atrás y tamaño de cola en el cliente.
  5. Interfaz de usuario: barra de progreso fluida adherida al Terminal con icono del ítem, cantidad, seguimiento de progreso en degradado cyan/hielo, cuenta atrás en segundos y tooltip descriptivo.
  6. Seguridad y concurrencia: no se resta nada al crear pedidos; la extracción se realiza en la entrega atómica, garantizando que jugadores concurrentes o cierres de ventana nunca dupliquen ni pierdan ítems.
- **Criterio:** Ítems en caliente llegan al instante, pedidos de frío muestran barra con cuenta atrás, llegan al inventario al finalizar si cabe, cerrar la interfaz cancela pedidos sin perder ítems y funciona en servidor dedicado.

---

### T28. Reglas
- **Objetivo:** Automatizaciones «condición → acción» configurables desde el Núcleo.
- **Implementación:**
  1. Menú propio del Núcleo (`CoreMenu` y `CoreScreen`) con pestañas «Estado» (telemetría de red, energía, almacenamiento y máquinas) y «Reglas» (hasta 8 filas configurables).
  2. Cada fila dispone de:
     - Ranura fantasma para ítem (asignable arrastrando o haciendo clic con ítem en el cursor, clic sin ítem limpia la ranura).
     - Selector cíclico de condición (`ITEM_COUNT_GREATER`, `ITEM_COUNT_LESS`, `TOTAL_SPACE_GREATER`, `TOTAL_SPACE_LESS`).
     - Campo numérico para umbral con validación y filtrado.
     - Selector cíclico de acción (`MOVE_TO_COLD`, `NOTIFY`, `REDSTONE_SIGNAL`).
     - Botón de habilitación (ON/OFF) e indicador LED de estado de ejecución (Verde activo, Gris en espera, Rojo ignorada).
  3. Motor de reglas `RuleEngine` en Java puro desacoplado de Minecraft:
     - Evaluación de condiciones con histéresis anti-parpadeo (5% para conteo de ítems, 2% para espacio total).
     - Rearme de avisos: los avisos (`NOTIFY`) disparan exactamente una sola vez al cumplirse la condición y se rearman automáticamente cuando deja de cumplirse.
     - Acciones de «mover al frío» (`MOVE_TO_COLD`) se aplican marcando el ítem en `SOLO_FRIO` mientras la regla está activa sin repetir movimientos redundantes.
     - Pruebas exhaustivas en JUnit 5 (`RuleEngineTest`) validando límites, anti-parpadeo y rearme.
  4. Núcleo (`CoreBlockEntity`): evaluación periódica cada 20 ticks, transferencia inmediata de existencias en caliente a frío si la regla se activa, inserción dirigida al frío para ítems con `SOLO_FRIO` y persistencia NBT con `data_version: 3`.
  5. Seguridad y sincronización de red: validación y acotamiento de parámetros en servidor mediante `CoreRuleUpdatePayload` y sincronización bidireccional continua con `CoreSyncPayload`.
- **Criterio:** «Hierro mayor que 5 000 → mover al frío» manda el hierro al frío y lo mantiene allí. «Espacio usado mayor que 90 % → avisar» dispara una vez y se rearma al bajar. Reglas persisten tras reiniciar y reglas sin ítem se ignoran correctamente.

---

### T29. Alarma y avisos
- **Objetivo:** Completar las acciones «avisar» y «señal de redstone» de la T28.
- **Implementación:**
  1. Bloque `alarm` (`ModBlocks.ALARM`, `AlarmBlockEntity`), miembro de red (`NetworkMember`, hoja no conductora) con un canal del 1 al 4 que cicla al hacer clic derecho con sonido y mensaje en barra de acción.
  2. Salida de redstone: implementados `isSignalSource`, `getSignal` y `getDirectSignal` (emite señal 15 cuando está activa, 0 apagada). Avisa a los bloques vecinos (`updateNeighborsAt`) tanto al activarse como al desactivarse o ser destruido.
  3. Cambio de modelo: el bloque pasa de modelo `alarm` (apagado) a `alarm_powered` (encendido con lente brillante) cuando su canal está activo.
  4. Selector de canales en Núcleo: las acciones de redstone permiten seleccionar independientemente los canales 1 al 4 (`REDSTONE_1`, `REDSTONE_2`, `REDSTONE_3`, `REDSTONE_4`), evaluadas por el motor de reglas `RuleEngine`.
  5. Acción «avisar»: los avisos (`NOTIFY`) envían un mensaje descriptivo en el chat con sonido de campana exclusivamente a los jugadores a menos de 64 bloques del Núcleo, una sola vez por activación (rearmándose al normalizarse las condiciones).
  6. Receta y traducciones por datagen: crafteo con antorcha de redstone, lingote de neoferrita, hierro y redstone. Texturas pixel-art 16x16 y traducciones completas en `en_us`, `es_es` y `es_mx`.
- **Criterio:** Una regla con señal enciende una lámpara de redstone junto a la alarma del mismo canal y la apaga al dejar de cumplirse. Una regla con aviso manda un solo mensaje por activación.

---

### T31. Centrífuga
- **Objetivo:** Separar subproductos de minerales y lodo, produciendo más escoria cuanto menor es la pureza del mineral.
- **Implementación:**
  1. Ítems nuevos: `slag` (escoria), `crystal_shard` (fragmento de cristal), `sludge` (lodo) y bloque decorativo industrial `slag_block` (crafteable 2x2 con 4 escorias y reversible a 4 escorias).
  2. Tipo de receta JSON `logtech:centrifuging` (`CentrifugingRecipe`) extendiendo `ProcessingRecipe`:
     - 1 polvo de pureza $p$ (120 ticks @ 80 FE/t = 9600 FE) produce 1 polvo idéntico (`purity_mode: PRESERVE`) y escoria con probabilidad $80\% \times (6 - p) / 5$ (`inverse_purity`): $p=1 \rightarrow 80\%$, $p=2 \rightarrow 64\%$, $p=3 \rightarrow 48\%$, $p=4 \rightarrow 32\%$, $p=5 \rightarrow 16\%$.
     - 1 lodo (120 ticks @ 80 FE/t) produce 1 fragmento de cristal (`crystal_shard`) con probabilidad 25%.
  3. Bloque `centrifuge` (`ModBlocks.CENTRIFUGE`, `CentrifugeBlockEntity`):
     - 1 ranura de entrada y 3 ranuras de salida para subproductos.
     - Búfer de energía de 20.000 FE a 80 FE/t con capacidad configurable en `LogTechConfig.CENTRIFUGE_ENERGY_CONSUMPTION`.
     - Generador aleatorio del nivel en el servidor (`level.random`, sin `Math.random`).
     - Prevención estricta de pérdida de subproductos: si alguna salida no cabe, pasa a estado `OUTPUT_FULL` y retiene `pendingOutputs` en memoria y persistido en NBT hasta que se libere espacio en las ranuras de salida.
     - Miembro de red (`NetworkMember`) con telemetría de 60 min (`ActivityCounter`).
  4. Interfaz (`CentrifugeMenu` y `CentrifugeScreen`): ranura de entrada a la izquierda, flecha animada con tooltip, tres ranuras de salida a la derecha, barra medidora de energía FE con tooltip y texto de estado traducido.
  5. Datagen y recursos: recetas JSON de centrifugado, crafteo con materiales v0.1 (hierro, neoferrita, cristal de datos, redstone), modelos, tablas de botín, texturas pixel-art 16x16 y traducciones completas en español e inglés.
  6. Pruebas unitarias en JUnit 5: simulación Monte Carlo de 100.000 ciclos en `CentrifugingSimulationTest` verificando convergencia estadística exacta en las 5 purezas y en lodo, además de prueba a escala de 100 ciclos.
- **Criterio:** La prueba JUnit da valores cercanos a los esperados, 100 polvos de pureza 1 producen ~80 escorias, el lodo produce fragmentos de cristal periódicamente y nunca se pierden subproductos si las ranuras de salida están llenas.

---

### T30. Lavadora
- **Objetivo:** Mejorar la pureza del polvo consumiendo agua y produciendo lodo (`sludge`).
- **Implementación:**
  1. Bloque `washer` (`ModBlocks.WASHER`, `WasherBlockEntity`) y `WasherBlock`:
     - 1 ranura de entrada (polvo), 2 ranuras de salida (polvo purificado y lodo).
     - Tanque de agua de 4 000 mB (`FluidTank` con filtro `Fluids.WATER`), expuesto como capability `Capabilities.FluidHandler.BLOCK` y llenable con cubos (`FluidUtil.interactWithFluidHandler`).
     - Búfer de energía de 10 000 FE a 60 FE/t, expuesto como `Capabilities.EnergyStorage.BLOCK`.
     - Sided item handlers: entrada desde lados/arriba a slot 0, extracción desde abajo de slots 1 y 2.
     - Persistencia NBT completa de inventario, tanque de fluidos, energía y progreso.
  2. Estado de máquina (`MachineStatus`):
     - Añadido `NO_FLUID` («Sin agua» / «Inactiva: sin agua») que se activa si el tanque contiene $< 250\text{ mB}$ sin perder agua ni avance.
  3. Lógica de ciclo y conservación:
     - 80 ticks @ 60 FE/t (4800 FE) y 250 mB de agua.
     - El agua y el polvo de entrada se descuentan **únicamente al finalizar el ciclo**. Si el ciclo se interrumpe o el mundo se cierra, no se gasta agua.
     - Rechazo estricto de pureza $\ge 5$: no entra en la ranura ni inicia receta.
     - Telemetría en la red (`NetworkMember` con `ActivityCounter`).
  4. Interfaz gráfica (`WasherMenu` y `WasherScreen`):
     - Sincronización robusta por red vía `ContainerData` de 11 valores (progreso, energía, agua, capacidad, estado). El cliente nunca lee el BlockEntity directamente.
     - Barra medidora de energía FE con tooltip.
     - Medidor vertical de agua con color acuático y tooltip dinámico en mB.
     - Ranuras de entrada y 2 salidas, flecha animada y estado visual coloreado.
  5. Recetas y Datagen:
     - Tipo de receta JSON `logtech:washing` (`WashingRecipe`) con `purity_mode: increase`.
     - Recetas para `neoferrite_dust` y `crystalite_dust` produciendo polvo $p+1$ y 1 `sludge`.
     - Crafteo de la lavadora con lingotes de neoferrita, hierro, balde de agua y cristal de datos.
     - Modelos, estados, loot tables y tags generados automáticamente.
  6. Pruebas unitarias JUnit 5 (`WashingSimulationTest`): validación de incremento de pureza, rechazo de pureza 5 y balance de agua.
- **Criterio:** Polvo con pureza 2 sale con pureza 3 más lodo, pureza 5 es rechazada, sin agua se detiene con causa visible, tuberías y cubos llenan el tanque, y el agua no se gasta si el ciclo no termina.

---

### T32. Módulos de datos de nivel 2 y 3
- **Objetivo:** Dar un uso a los fragmentos de cristal y una progresión al almacenamiento digital.
- **Implementación:**
  1. Nuevos ítems registrados: `data_module_advanced` (nivel 2, x4 capacidad por defecto) y `data_module_master` (nivel 3, x16 capacidad por defecto). Los módulos de la v0.1 (`data_module`, nivel 1) se mantienen sin cambios (x1 capacidad).
  2. Multiplicadores configurables en `LogTechConfig` (`dataModuleAdvancedMultiplier = 4`, `dataModuleMasterMultiplier = 16`).
  3. Lógica matemática desacoplada en `DataModuleLogic` (Java puro) y probada exhaustivamente con JUnit 5 (`DataModuleCapacityTest`), verificando compatibilidad de v0.1, racks con módulos mezclados y redes complejas.
  4. Compatibilidad en Racks (`RackBlockEntity` y `RackBlock`): ranuras aceptan cualquiera de los tres módulos. El cálculo de capacidad suma la capacidad base multiplicada por el multiplicador del módulo instalado en cada ranura.
  5. Tooltip informativo en cliente (`ClientTooltipHandler`): muestra la capacidad aportada en rack caliente y frío para cada módulo según la configuración activa.
  6. Recetas crafteo con progresión:
     - Módulo avanzado: `data_module` en el centro rodeado de `crystal_shard`, `data_crystal` y `neoferrite_ingot`.
     - Módulo maestro: `data_module_advanced` en el centro rodeado de `crystal_shard` y `data_crystal`.
  7. Recursos y Datagen: modelos de ítems, texturas pixel-art personalizadas 16x16, recetas y traducciones completas en `en_us`, `es_es` y `es_mx`.
- **Criterio:** La capacidad total suma bien con módulos mezclados, y un mundo de la v0.1 abre sin cambios ni pérdidas.

---

### T33. Energía por red
- **Objetivo:** Que las máquinas conectadas a la red reciban FE del Núcleo de forma proporcional.
- **Implementación:**
  1. Configuración (`LogTechConfig`): `coreEnergyCapacity` configurable (200 000 FE por defecto).
  2. Entrada de energía por Backbone (`BackboneEnergyStorage` y `ModCapabilities`):
     - `Capabilities.EnergyStorage.BLOCK` registrado en `ModBlocks.BACKBONE`.
     - Solo entrada (`canExtract = false`): cualquier generador pegado a cualquier Backbone alimenta la red y canaliza la energía al Núcleo conectado.
  3. Demanda y recepción en máquinas (`NetworkMember` y `MachineEnergyStorage`):
     - Métodos en `NetworkMember`: `requestedEnergy()` (devuelve hueco en búfer interno) y `receiveNetworkEnergy(amount)`.
     - `MachineEnergyStorage.receiveNetworkEnergy(toReceive)`: acepta hasta la capacidad máxima disponible sin verse limitado por el `maxReceive` de cables externos.
     - Implementado en `CrusherBlockEntity`, `DrillBlockEntity`, `WasherBlockEntity` y `CentrifugeBlockEntity`. Productores (`CoalGeneratorBlockEntity`) y alarmas devuelven 0.
  4. Lógica de reparto proporcional (`EnergyDistributionLogic`):
     - Java puro desacoplado con pruebas unitarias exhaustivas en `EnergyDistributionLogicTest` y `EnergyNetworkSimulationTest`.
     - Si la energía disponible cubre la demanda total, asigna el 100%. Si no, reparte proporcionalmente `floor(available * req / totalRequested)`.
     - Invariante estricta: energía entregada $\le available$, sin creación ni destrucción de energía.
  5. Ciclo en Núcleo (`CoreBlockEntity`):
     - Cada 20 ticks descuenta primero el consumo propio del Núcleo (400 FE @ 20 FE/t).
     - Si el Núcleo cubrió su propio consumo y queda energía, reparte proporcionalmente a los miembros en chunks cargados.
     - Contadores de depuración (`energyReceivedTotal`, `energyDistributedTotal`, `energyConsumedSelfTotal`) persistidos en NBT.
### T34. Prioridades de energía
- **Objetivo:** Que, cuando no alcanza la energía, se alimenten primero las máquinas más importantes.
- **Implementación:**
  1. Enum `EnergyPriority` (`HIGH`, `NORMAL`, `LOW`) implementado en `NetworkMember`, con métodos `getEnergyPriority()`, `setEnergyPriority()`, `isEnergyShed()` y `setEnergyShed()`.
  2. Botón interactivo en pantallas (`CrusherScreen`, `WasherScreen`, `CentrifugeScreen` y combinación Shift + Clic derecho en `DrillBlock`):
     - Botón `[ P: Alta ]`, `[ P: Normal ]`, `[ P: Baja ]` situado en la esquina superior derecha con tooltip explicativo.
     - Paquete C2S `MachinePriorityPayload` validado en el servidor actualizando la máquina mediante la interfaz modular `PriorityConfigurableMenu`.
     - Sincronización continua de estado al cliente vía `ContainerData`.
  3. Persistencia NBT y migración retrocompatible:
     - Guardado en `CrusherBlockEntity`, `WasherBlockEntity`, `CentrifugeBlockEntity` y `DrillBlockEntity` con `data_version: 2` y clave `energy_priority`.
     - Mundos de v0.1 cargan con prioridad `NORMAL` por defecto sin pérdidas ni errores.
  4. Estado y causa en `MachineStatus`:
     - Añadido estado `SHED` («Prioridad baja») con causa traducida `gui.logtech.machine_status.cause.shed` («apagada por prioridad baja»).
     - Visualizado en la columna de estado de cada máquina y en la pestaña de estadísticas del Terminal.
  5. Motor de reparto `PriorityEnergyDistributor` (clase Java pura desacoplada):
     - Orden de atención: Núcleo y Terminal van siempre primero; luego ALTA, NORMAL y BAJA.
     - Dentro de un mismo nivel, reparto proporcional a lo pedido: $\text{floor}(E \times \text{pedido} / \text{totalNivel})$.
     - Estabilización temporal en límites (`holdCycles = 3`): mantiene el estado o asignación durante varios ciclos para evitar oscilaciones o parpadeos rápidos (20 ticks).
     - El cambio de prioridad por parte del usuario o el ingreso de potencia suficiente reactiva la máquina de forma inmediata.
     - Garantía matemática de conservación: energía total asignada $\le$ disponible.
  6. Pruebas unitarias JUnit 5 (`PriorityEnergyDistributorTest`):
     - Validación del escenario de tres máquinas con prioridades distintas donde ALTA trabaja y BAJA se detiene, y al añadir otro generador BAJA vuelve.
     - Comprobación de reparto proporcional en el mismo nivel, estabilidad en el límite, respuesta instantánea ante cambios de prioridad y conservación de energía.
- **Criterio:** Tres máquinas con prioridades distintas y un generador insuficiente: la ALTA trabaja y la BAJA se detiene. Al añadir otro generador, la BAJA vuelve. El Terminal muestra la causa.

---

### T35. Panel de energía
- **Objetivo:** Ver cuánta energía entra, cuánta se consume y quién la gasta desde el Terminal.
- **Implementación:**
  1. Telemetría de energía en `ActivityCounter`:
     - Añadido registro y acumulación de `energyConsumed` y `energyReceived` en `MinuteBucket`, `ActivitySummary` e historial de 60 minutos de `ActivityCounter`.
     - Métodos `recordEnergyConsumed(fe)`, `recordEnergyReceived(fe)`, `getEnergyConsumed(minuteIndex)` y `getEnergyReceived(minuteIndex)`.
  2. Telemetría en máquinas:
     - `CoreBlockEntity`: acumula entrada en `energyReceivedCounter` cuando recibe FE de generadores externos por el Backbone, registra su consumo propio (400 FE / 20 ticks) y avanza contadores cada minuto.
     - `CrusherBlockEntity`, `WasherBlockEntity`, `CentrifugeBlockEntity` y `DrillBlockEntity`: registran FE consumida en su `activityCounter` en cada tick activo de procesamiento.
  3. Pestaña «Energía» en Terminal (`TerminalMenu` y `TerminalScreen`):
     - Tercera pestaña añadida al Terminal (`TAB_ENERGY = 2`).
     - Barra de progreso visual con FE almacenada y capacidad máxima de la red con formato numérico y porcentaje.
     - Cifras en tiempo real de FE/min recibida y FE/min consumida.
     - Gráfica histórica de barras diferenciadas de los últimos 60 minutos (verde para recibida, rojo/naranja para consumida) con tooltip dinámico al pasar el cursor mostrando minuto, recibida, consumida y balance neto (+ / -).
     - Consumo agrupado por rama: Almacenamiento (Núcleo y Terminal) y Minerales (Triturador, Lavadora, Centrífuga y Perforadora).
     - Lista de máquinas conectadas ordenada de mayor a menor consumo con su prioridad (`Alta`, `Normal`, `Baja`), insignia coloreada y soporte para desplazamiento por rueda de ratón.
     - Botón/insignia interactiva en la lista que permite ciclar la prioridad de la máquina directamente desde el Terminal enviando el paquete C2S `TerminalPriorityPayload`.
  4. Red y Sincronización eficiente:
     - `TerminalTabPayload` sincroniza la pestaña activa (`0` = Items, `1` = Estadísticas, `2` = Energía).
     - `TerminalEnergyPayload` transmite el estado energético y telemetría de 60 minutos.
     - La sincronización ocurre cada 20 ticks únicamente si la pestaña de Energía está abierta (`activeTab == TAB_ENERGY`). Si otra pestaña está abierta, no se envía ningún paquete de energía.
     - Al cerrar el menú, el cliente avisa al servidor restableciendo la pestaña a 0.
     - Validación en servidor para el cambio de prioridades: verifica contenedor abierto, distancia al Terminal, pertenencia a la red y chunk cargado.
  5. Recursos y Datagen:
     - Claves de traducción generadas en `en_us`, `es_es` y `es_mx` para pestañas, etiquetas de energía, ramas y tooltips.
  6. Pruebas automáticas en JUnit 5:
     - `ActivityCounterTest`: validación unitaria de registro y agregación de FE consumida y recibida.
     - `EnergyTelemetryTest`: validación matemática de coherencia (la suma de consumos por máquina coincide exactamente con el total de la red y la suma por ramas), caída de entrada a 0 al desconectar generador, ordenación descendente y ciclado seguro de prioridades.
- **Criterio:** Las cifras son coherentes con las máquinas trabajando (la suma por máquina coincide con el total). Al desconectar el generador se ve caer la entrada. No se envían paquetes con otra pestaña abierta. La prioridad de las máquinas puede alternarse desde el panel.

---

## Fase 5 — Compatibilidad

### T37. Jade
- **Objetivo:** Que Jade (el indicador HUD al mirar un bloque) muestre la información de LogTech.
- **Implementación:**
  1. Dependencia opcional añadida en `build.gradle` (`compileOnly` y `localRuntime` para entorno de desarrollo): `maven.modrinth:jade:15.10.6+neoforge` con repositorio Modrinth Maven.
  2. Aislamiento total en el paquete `io.github.chema280101.logtech.compat.jade`. Ninguna clase fuera de este paquete menciona clases de Jade. Se añadió prueba unitaria `CompatIsolationTest` en JUnit 5 que audita el código fuente principal para garantizar que no existan imports de visores externos fuera de `compat`.
  3. Proveedores modulares implementando `IBlockComponentProvider` y `IServerDataProvider<BlockAccessor>` con sincronización ligera en `CompoundTag`:
     - `MachineJadeProvider`: Máquinas (Triturador, Lavadora, Centrífuga y Perforadora) reportando estado con su causa traducida, progreso en %, energía almacenada y máxima en FE, prioridad energética (`Alta`, `Normal`, `Baja`); en Lavadora, cantidad y capacidad de agua en mB; en Perforadora, pureza y reserva restante (o «Infinita») de la veta descubierta sobre la que perfora (o aviso si no hay veta).
     - `CoreJadeProvider`: Núcleo reportando capacidad de almacenamiento usada y total, energía almacenada y máxima, y estado de la red («Válida», «Sobrecargada» o «Apagada»).
     - `RackJadeProvider`: Racks reportando nivel (`Caliente` o `Frío`), conteo de módulos de datos instalados (x/4) y capacidad total aportada.
  4. Plugin principal `LogTechJadePlugin` implementando `IWailaPlugin` y anotado con `@WailaPlugin`, registrando proveedores del servidor en `register` y componentes del cliente en `registerClient`.
  5. Recursos y Datagen: Claves de traducción añadidas y generadas en `en_us`, `es_es` y `es_mx`.
  6. Comprobada la independencia de Jade: el mod compila y empaqueta en jar sin Jade presente en runtime.
- **Criterio:** Con Jade, al mirar cada bloque aparece su información estructurada. Sin Jade, el juego arranca y funciona con total normalidad.

---

### T38. JEI
- **Objetivo:** Que JEI muestre las recetas de las máquinas de LogTech (`logtech:crushing`, `logtech:washing`, `logtech:centrifuging`).
- **Implementación:**
  1. Dependencias opcionales en Gradle:
     - Repositorio BlameJared Maven (`https://maven.blamejared.com/`).
     - Dependencias: `compileOnly "mezz.jei:jei-1.21.1-neoforge-api:19.21.0.247"` y `localRuntime "mezz.jei:jei-1.21.1-neoforge:19.21.0.247"`.
  2. Aislamiento total en el paquete `io.github.chema280101.logtech.compat.jei`:
     - Verificado mediante `CompatIsolationTest` para garantizar que ningún archivo fuera de `compat/` importe `mezz.jei.*`.
  3. Categorías de recetas (`ProcessingRecipeCategory` base extensible):
     - `CrushingCategory` (`logtech:crushing`): muestra ingrediente de entrada con pureza representativa (1), salidas, energía en FE y FE/t, tiempo en segundos y ticks, y efecto de pureza en texto («Pureza: Conservada»).
     - `WashingCategory` (`logtech:washing`): muestra ingrediente con pureza representativa (2), salidas con pureza resultante (3), consumo de agua en mB (250 mB), energía, tiempo, y efecto en texto («Efecto: Pureza +1 (máx. 5)»).
     - `CentrifugingCategory` (`logtech:centrifuging`): muestra ingrediente de entrada, salidas principales y subproductos con probabilidad en tooltip y escala inversa por pureza («Probabilidad: hasta 80% (escala por pureza: (6 - p)/5)», lodo con 25%), energía, tiempo, y efecto de pureza («Pureza: Conservada»).
  4. Catalizadores y áreas clicables:
     - Máquinas asociadas como catalizadores de sus recetas en `registerRecipeCatalysts`: Triturador (`CRUSHER`), Lavadora (`WASHER`) y Centrífuga (`CENTRIFUGE`), permitiendo ver sus recetas con 'U' (usos).
     - Áreas clicables (`registerGuiHandlers`) en la flecha de progreso de `CrusherScreen`, `WasherScreen` y `CentrifugeScreen`.
  5. Carga dinámica de recetas:
     - Lectura directa desde el `RecipeManager` activo (`recipeManager.getAllRecipesFor(...)`), garantizando compatibilidad automática con cualquier datapack.
  6. Manejo de pureza representativa:
     - Evita generar recetas duplicadas por cada nivel de pureza, mostrando un ejemplo claro y representativo acompañado de explicaciones textuales.
  7. Recursos y Datagen:
     - Traducciones completas generadas en `en_us`, `es_es` y `es_mx`.
  8. Pruebas unitarias automatizadas (`JeiCompatTest` y `CompatIsolationTest`):
     - Cobertura de anotaciones `@JeiPlugin`, tipos de receta, interfaz `IModPlugin`, lógica de pureza representativa y escalado.
- **Criterio:** Con JEI, las tres categorías aparecen con sus recetas y las máquinas muestran sus usos. Sin JEI, el juego arranca y compila normalmente.

---

### T39. EMI
- **Objetivo:** Que EMI soporte las categorías y estaciones de trabajo de LogTech.
- **Estado:** Pendiente.

---

## Fase 6 — Cierre

### T40. Configuración y migración
- **Objetivo:** Cerrar la configuración y asegurar que los mundos de la v0.1 abren bien sin errores en el registro ni pérdida de items.
- **Implementación:**
  1. Auditoría completa de constantes de balance en el código e integración centralizada en `LogTechConfig`:
     - `MAX_RULES` (número máximo de reglas en el Núcleo, default 8).
     - `SCREEN_REACH_DISTANCE` (alcance máximo de interacción con pantallas y menús, default 8).
     - `RULE_NOTIFICATION_RANGE` (radio de avisos y notificaciones de reglas por chat/sonido, default 64).
     - `WASHER_ENERGY_CONSUMPTION` (consumo base en FE/t de la lavadora, default 60).
     - `ENERGY_DISTRIBUTION_INTERVAL` (intervalo en ticks entre repartos de energía del Núcleo, default 20).
     - `ENERGY_PRIORITY_HOLD_CYCLES` (ciclos de estabilización temporal para evitar oscilaciones en el reparto, default 3).
  2. Inventario exhaustivo de datos guardados y migración de `data_version`:
     - Núcleo (`CoreBlockEntity`): `data_version: 3`. Mundos de v0.1 (`data_version < 2`) transfieren el 100% de ítems al nivel caliente (`hotTier`), inicializando nivel frío vacío (0 ítems) y políticas en `AUTO`.
     - Racks (`RackBlockEntity` y `ColdRackBlockEntity`): serialización `inventory` 100% retrocompatible, módulos v0.1 cargan intactos.
     - Máquinas (`CrusherBlockEntity`, `DrillBlockEntity`, `CoalGeneratorBlockEntity`): `data_version: 2`. Mundos v0.1 adoptan `EnergyPriority.NORMAL` por defecto sin perder progreso, búfer de ítems ni energía.
     - Vetas (`DepositSavedData`): estructura idéntica `VeinStates` preservando descubrimientos y reservas restantes.
  3. Comprobación y copia del mundo de pruebas de la v0.1 (`d:\logtech_migration_test_world_v0.1`) a `run\saves\v0.1_Migration_Test_World` para pruebas directas en el cliente.
  4. Suite de pruebas automatizadas en `MigrationTest` (JUnit 5) validando integridad de archivos `.dat`, compresión GZIP de vetas, no-descarte de ítems en el Núcleo y ciclo completo de guardado y re-apertura.
- **Criterio:** El mundo de la v0.1 abre y funciona sin errores en el registro, y no quedan constantes de balance sueltas en el código.

