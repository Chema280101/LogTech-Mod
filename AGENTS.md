# Contexto del proyecto: LogTech

LogTech es un mod de tecnología para Minecraft Java Edition con NeoForge. Idea central: el jugador ve, mide y optimiza su fábrica (almacenamiento inteligente, minerales con prospección y telemetría por máquina). El diseño completo está en `docs/diseno.md`.

## Entorno fijo (no cambiar sin avisarme)
- Minecraft 1.21.1 con NeoForge para 1.21.1 (plantilla ModDevGradle).
- Java 21 y Gradle (usa `./gradlew`).
- Nombres oficiales de Mojang (los mappings por defecto del MDK).
- Mod id: `logtech`. Paquete base: `io.github.chema280101.logtech`.

## Reglas importantes
- No uses APIs de Forge antiguo ni de otras versiones de Minecraft. Si no estás seguro de que una clase o método existe en NeoForge 1.21.1, búscalo en el código fuente del proyecto o dímelo; no lo inventes.
- Registra bloques, items y demás con `DeferredRegister` (`DeferredRegister.Blocks`, `DeferredRegister.Items`) y regístralos en el constructor del mod con el bus de eventos del mod.
- Nunca crees `new Block(...)` ni `new Item(...)` fuera del registro.
- Para datos en items usa data components, no el NBT antiguo.
- Rutas de datos de 1.21: `loot_table`, `tags/block`, `tags/item` (en singular).
- IDs de registro en inglés, en minúsculas y con guion bajo (`core`, `data_module`). Los nombres en español van en `lang/es_es.json`.
- Paquetes: `block`, `item`, `network`, `machine`, `storage`, `ore`, `client`, `data`, `registry`.
- La lógica va en el servidor; el cliente solo muestra. No hagas tick en cada bloque de la red: usa un gestor de redes por nivel.
- Todo cambio debe compilar con `./gradlew build` y arrancar con `./gradlew runClient`.
- **Recetas de máquinas en JSON con tipos propios:** Define tipos de receta personalizados (`RecipeType` y `RecipeSerializer`) en JSON usando Codecs y StreamCodecs de 1.21.1 en vez de recetas hardcodeadas en Java.
- **Pureza como data component:** La pureza de los minerales e ítems se almacena y propaga mediante `DataComponentType`, garantizando persistencia en stacks.
- **Integraciones modulares (JEI, EMI, Jade):** Deben ser opcionales (soft-dependencies); el mod debe compilar y ejecutarse normalmente sin fallos si ninguno de estos mods auxiliares está instalado.
- **Configuración centralizada:** Cualquier nuevo balance o parámetro configurable debe integrarse limpiamente en `LogTechConfig` (`logtech-server.toml` u otras categorías adecuadas).
- **Compatibilidad con mundos previos:** Los mundos generados en la versión v0.1 deben seguir cargando correctamente en la v0.2 sin corrupción ni pérdidas de datos en bloques, redes o inventarios.

## Forma de trabajar
- Una tarea pequeña a la vez. Explica qué archivos creas o cambias.
- Después de cada tarea, dime cómo probarla en el juego.
- No cambies archivos de Gradle ni refactorices sin que yo lo pida.
- Si hay varias formas de hacerlo, propón la más simple.

## Estado actual
- Versión v0.1 completada (Alpha v0.1.0): Hito 0 hasta T21 finalizados y etiquetados en git `v0.1.0`. Incluye almacenamiento digital, redes backbone y rack, inserción/extracción en terminal con búsqueda y telemetría de 60 min, Triturador (2x polvo @ 40 FE/t), Perforadora (80 FE/t) con vetas deterministas Overworld y escáner con mapa 2D, balance en configuración del servidor y release limpia.
- Rama `v0.2` inicializada con versión `0.2.0-dev`.
- Tarea T22 completada: Preparación v0.2; problemas conocidos revisados y catalogados en `docs/issues.md` (ninguno con pérdida de items ni crashes), rama `v0.2` creada, versión `0.2.0-dev` establecida en `gradle.properties`, mundo de pruebas de la v0.1 respaldado externamente en `d:\logtech_migration_test_world_v0.1`, `AGENTS.md` actualizado con reglas de v0.2 y plan registrado en `docs/plan-v0.2.md`.
- Siguiente tarea: T23. Pureza.

