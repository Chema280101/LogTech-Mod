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
- **Estado:** Abierto
- **Descripción:** Los sprites e iconos actuales son funcionales y de resolución 16x16 base.
- **Plan:** Mejorar los detalles visuales de los modelos, animaciones y texturas a lo largo de las versiones v0.2 y v0.3.

---

## Revisión de estabilidad v0.2
- **Pérdida de ítems:** 0 incidencias reportadas. La cola de entrega atómica de frío, la retención de subproductos de la centrífuga y el desborde jerárquico garantizan conservación al 100%.
- **Crashes:** 0 crashes reportados. Las integraciones de Jade, JEI y EMI operan con aislamiento modular comprobado.
- **Consumo y balances:** Verificado balance térmico y de energía sin fugas ni sobrecargas infinitas.
