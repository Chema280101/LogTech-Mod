# Issues & Backlog — LogTech

Registro formal de issues y limitaciones identificadas en la versión alpha v0.1.0 para seguimiento y resolución en futuras versiones.

---

### Issue #1: [Feature] Generadores de energía FE propios
- **Tipo:** Mejora / Nueva funcionalidad
- **Severidad:** Baja (no causa crash ni pérdida de items)
- **Estado:** Cerrado / Resuelto en v0.2
- **Descripción:** LogTech actualmente consume Forge Energy (FE) en máquinas como el Núcleo, Triturador y Perforadora, pero no proveía generadores nativos.
- **Resolución:** Implementado `coal_generator` (Generador de carbón a 40 FE/t) en la versión v0.2.

---

### Issue #2: [UX / Enhancement] Actualización dinámica en tiempo real de reservas en el Mapa de Vetas
- **Tipo:** Mejora de experiencia de usuario
- **Severidad:** Baja (no causa crash ni pérdida de items)
- **Estado:** Abierto
- **Descripción:** La reserva mostrada en los tooltips del mapa 2D de vetas refleja una instantánea del momento en que se abre la interfaz gráfica. Si una perforadora está extrayendo activamente en paralelo, el valor numérico no se decrementa en pantalla hasta que el jugador cierra y vuelve a abrir el mapa.
- **Plan:** Implementar un paquete opcional de sincronización periódica liviana o actualización al interactuar si la pantalla permanece abierta.

---

### Issue #3: [Design] Comportamiento de máquinas como nodos hoja en la red
- **Tipo:** Arquitectura / Comportamiento de red
- **Severidad:** Informativa (comportamiento intencional documentado)
- **Estado:** Cerrado / Por diseño
- **Descripción:** Los bloques de máquinas (`crusher`, `drill`) actúan como nodos hoja en la red: se conectan a cables Backbone o al Núcleo, pero no conducen la red a otros bloques adyacentes. Esto previene uniones accidentales entre redes distintas a través de maquinaria compartida.

---

### Issue #4: [Art / Assets] Refinamiento de texturas y modelos 3D
- **Tipo:** Arte y recursos visuales
- **Severidad:** Baja (estético)
- **Estado:** Cerrado / Resuelto en v0.3
- **Descripción:** Los sprites de minerales usaban provisionalmente el icono base del módulo de datos, y las máquinas y bloques de almacenamiento compartían una sola textura repetida en todas sus caras (`cubeAll`).
- **Resolución:**
  1. Diseñados sprites 16x16 pixel-art dedicados y fieles a la estética de Minecraft vanilla para todos los minerales brutos (`raw_neoferrite`, `raw_crystalite`), polvos triturados (`neoferrite_dust`, `crystalite_dust`), lingote de neoferrita (`neoferrite_ingot`) y cristal de datos (`data_crystal`).
  2. Implementada orientación horizontal (`FACING`) en `CoreBlock`, `RackBlock`, `TerminalBlock`, `CrusherBlock`, `WasherBlock`, `CentrifugeBlock`, `CoalGeneratorBlock` y `DrillBlock`, haciendo que las máquinas miren automáticamente hacia el jugador al colocarse.
  3. Generados modelos 3D orientables (`orientable_with_bottom`) con caras diferenciadas: chasis lateral reforzado, base pesada, panel superior especializado (tolva, chimenea, turbina, visor) y frontal distintivo con pantallas, mandíbulas, bahías y cámaras de fluidos, incluyendo estados activos con fuego y movimiento.

---

## Revisión de estabilidad v0.2
- **Pérdida de ítems:** 0 incidencias reportadas. La cola de entrega atómica de frío, la retención de subproductos de la centrífuga y el desborde jerárquico garantizan conservación al 100%.
- **Crashes:** 0 crashes reportados. Las integraciones de Jade, JEI y EMI operan con aislamiento modular comprobado.
- **Consumo y balances:** Verificado balance térmico y de energía sin fugas ni sobrecargas infinitas.
