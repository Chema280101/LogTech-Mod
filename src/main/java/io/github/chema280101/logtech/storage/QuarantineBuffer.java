package io.github.chema280101.logtech.storage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Búfer de cuarentena para ítems entrantes cuando la red está llena, sobrecargada o apagada.
 * Clase Java pura desacoplada de Minecraft.
 *
 * @param <K> tipo de clave del ítem
 */
public class QuarantineBuffer<K> {

    @FunctionalInterface
    public interface QuarantineSink<K> {
        /**
         * Inserta ítems en el destino (la red).
         * @param item clave del ítem
         * @param amount cantidad a insertar
         * @return cantidad realmente aceptada e insertada por el destino
         */
        long accept(K item, long amount);
    }

    private final Map<K, Long> entries = new LinkedHashMap<>();

    public QuarantineBuffer() {
    }

    /**
     * Devuelve la cantidad total de ítems almacenados en el búfer de cuarentena.
     */
    public long getTotalCount() {
        long total = 0L;
        for (long count : entries.values()) {
            total += count;
        }
        return total;
    }

    /**
     * Devuelve la cantidad almacenada de un ítem específico en cuarentena.
     */
    public long getAmount(K item) {
        if (item == null) {
            return 0L;
        }
        return entries.getOrDefault(item, 0L);
    }

    /**
     * Devuelve una vista inmutable de las entradas de cuarentena.
     */
    public Map<K, Long> getEntries() {
        return Collections.unmodifiableMap(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    /**
     * Inserta una cantidad de un ítem en el búfer de cuarentena respetando la capacidad máxima.
     *
     * @param item clave del ítem
     * @param amount cantidad solicitada
     * @param simulate si es true, no modifica el búfer
     * @param maxCapacity capacidad máxima en ítems del búfer
     * @return cantidad realmente aceptada
     */
    public long insert(K item, long amount, boolean simulate, long maxCapacity) {
        if (item == null || amount <= 0 || maxCapacity <= 0) {
            return 0L;
        }

        long currentTotal = getTotalCount();
        long available = Math.max(0L, maxCapacity - currentTotal);
        long toInsert = Math.min(amount, available);

        if (toInsert <= 0) {
            return 0L;
        }

        if (!simulate) {
            entries.merge(item, toInsert, Long::sum);
        }

        return toInsert;
    }

    /**
     * Extrae una cantidad de un ítem del búfer de cuarentena.
     *
     * @param item clave del ítem
     * @param amount cantidad solicitada
     * @param simulate si es true, no modifica el búfer
     * @return cantidad realmente extraída
     */
    public long extract(K item, long amount, boolean simulate) {
        if (item == null || amount <= 0) {
            return 0L;
        }

        long current = getAmount(item);
        long toExtract = Math.min(amount, current);

        if (toExtract <= 0) {
            return 0L;
        }

        if (!simulate) {
            long remaining = current - toExtract;
            if (remaining <= 0) {
                entries.remove(item);
            } else {
                entries.put(item, remaining);
            }
        }

        return toExtract;
    }

    /**
     * Vacía ítems desde la cuarentena hacia el destino (la red) con un límite por ciclo.
     * Los ítems solo se descuentan de la cuarentena en la cantidad exacta que el destino acepta,
     * garantizando que no haya duplicaciones ni pérdidas de ítems.
     *
     * @param sink destino donde insertar los ítems
     * @param maxItems límite máximo de ítems a transferir en este ciclo
     * @return cantidad total de ítems transferidos
     */
    public long drainTo(QuarantineSink<K> sink, long maxItems) {
        if (sink == null || maxItems <= 0 || entries.isEmpty()) {
            return 0L;
        }

        long budgetRemaining = maxItems;
        long totalDrained = 0L;

        var iterator = entries.entrySet().iterator();
        while (iterator.hasNext() && budgetRemaining > 0) {
            var entry = iterator.next();
            long availableInEntry = entry.getValue();
            if (availableInEntry <= 0) {
                iterator.remove();
                continue;
            }

            long toOffer = Math.min(availableInEntry, budgetRemaining);
            long accepted = sink.accept(entry.getKey(), toOffer);

            if (accepted > 0) {
                long remaining = availableInEntry - accepted;
                if (remaining <= 0) {
                    iterator.remove();
                } else {
                    entry.setValue(remaining);
                }
                totalDrained += accepted;
                budgetRemaining -= accepted;
            }

            // Si el destino aceptó menos de lo ofrecido, significa que el espacio en destino se agotó
            if (accepted < toOffer) {
                break;
            }
        }

        return totalDrained;
    }
}
