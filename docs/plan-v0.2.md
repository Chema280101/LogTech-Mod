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
*(Próxima tarea a detallar e implementar según la ruta de desarrollo de la Fase 1)*
