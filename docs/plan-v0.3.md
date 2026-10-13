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
