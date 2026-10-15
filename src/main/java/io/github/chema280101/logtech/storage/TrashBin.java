package io.github.chema280101.logtech.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestor de la papelera de reciclaje del Núcleo.
 * Almacena los ítems destruidos agrupados por tipo con su hora de juego (gameTime).
 * Soporta capacidad máxima en ítems y expiración tras un número configurable de ticks.
 * Al llenarse o caducar se eliminan los ítems más antiguos (FIFO por gameTime).
 *
 * Clase Java pura desacoplada de Minecraft.
 *
 * @param <K> tipo de clave del ítem
 */
public class TrashBin<K> {

    private final Map<K, TrashEntry<K>> entries = new LinkedHashMap<>();

    public TrashBin() {
    }

    /**
     * Añade una cantidad de un ítem a la papelera registrando la hora de juego (gameTime).
     * Si la cantidad total de la papelera excede maxCapacity, se descartan los ítems más antiguos.
     *
     * @param item clave del ítem
     * @param amount cantidad a añadir
     * @param gameTime hora de juego (gameTime) actual
     * @param maxCapacity capacidad máxima en ítems que puede albergar la papelera
     * @return cantidad realmente añadida y retenida en la papelera
     */
    public long add(K item, long amount, long gameTime, long maxCapacity) {
        if (item == null || amount <= 0 || maxCapacity <= 0) {
            return 0L;
        }

        TrashEntry<K> existing = this.entries.get(item);
        if (existing != null) {
            this.entries.put(item, existing.withAmountAndGameTime(existing.amount() + amount, gameTime));
        } else {
            this.entries.put(item, new TrashEntry<>(item, amount, gameTime));
        }

        enforceCapacity(maxCapacity);
        return Math.min(amount, getAmount(item));
    }

    /**
     * Fuerza el límite de capacidad de la papelera, eliminando los ítems de las entradas
     * con menor gameTime (más antiguas) primero.
     *
     * @param maxCapacity capacidad máxima en ítems
     */
    public void enforceCapacity(long maxCapacity) {
        if (maxCapacity < 0) {
            maxCapacity = 0;
        }

        long total = getTotalCount();
        if (total <= maxCapacity) {
            return;
        }

        long overflow = total - maxCapacity;

        // Ordenar entradas por gameTime ascendente (más antiguas primero)
        List<TrashEntry<K>> sortedEntries = new ArrayList<>(this.entries.values());
        sortedEntries.sort(Comparator.comparingLong(TrashEntry::gameTime));

        for (TrashEntry<K> entry : sortedEntries) {
            if (overflow <= 0) {
                break;
            }

            long currentAmount = entry.amount();
            if (currentAmount <= overflow) {
                this.entries.remove(entry.item());
                overflow -= currentAmount;
            } else {
                long newAmount = currentAmount - overflow;
                this.entries.put(entry.item(), entry.withAmount(newAmount));
                overflow = 0;
            }
        }
    }

    /**
     * Purga las entradas cuya antigüedad respecto a currentGameTime haya alcanzado o superado lifetimeTicks.
     *
     * @param currentGameTime hora de juego actual
     * @param lifetimeTicks duración máxima de vida de un ítem en la papelera en ticks
     * @return número de entradas eliminadas por expiración
     */
    public int pruneExpired(long currentGameTime, long lifetimeTicks) {
        if (lifetimeTicks <= 0) {
            int count = this.entries.size();
            this.entries.clear();
            return count;
        }

        List<K> expiredKeys = new ArrayList<>();
        for (Map.Entry<K, TrashEntry<K>> e : this.entries.entrySet()) {
            TrashEntry<K> entry = e.getValue();
            if (currentGameTime - entry.gameTime() >= lifetimeTicks) {
                expiredKeys.add(e.getKey());
            }
        }

        for (K key : expiredKeys) {
            this.entries.remove(key);
        }

        return expiredKeys.size();
    }

    /**
     * Restaura hasta maxToRestore ítems de la entrada especificada.
     *
     * @param item clave del ítem
     * @param maxToRestore cantidad máxima solicitada a restaurar
     * @return cantidad realmente retirada de la papelera
     */
    public long restore(K item, long maxToRestore) {
        if (item == null || maxToRestore <= 0) {
            return 0L;
        }

        TrashEntry<K> entry = this.entries.get(item);
        if (entry == null) {
            return 0L;
        }

        long available = entry.amount();
        long toRestore = Math.min(available, maxToRestore);

        if (toRestore >= available) {
            this.entries.remove(item);
        } else {
            this.entries.put(item, entry.withAmount(available - toRestore));
        }

        return toRestore;
    }

    /**
     * Devuelve la cantidad almacenada en la papelera para una clave dada.
     */
    public long getAmount(K item) {
        if (item == null) {
            return 0L;
        }
        TrashEntry<K> entry = this.entries.get(item);
        return entry != null ? entry.amount() : 0L;
    }

    /**
     * Devuelve la cantidad total sumada de todos los ítems en la papelera.
     */
    public long getTotalCount() {
        long total = 0L;
        for (TrashEntry<K> entry : this.entries.values()) {
            total += entry.amount();
        }
        return total;
    }

    /**
     * Devuelve una vista inmutable del mapa de entradas.
     */
    public Map<K, TrashEntry<K>> getEntries() {
        return Collections.unmodifiableMap(this.entries);
    }

    /**
     * Devuelve la entrada para un ítem, o null si no está en la papelera.
     */
    public TrashEntry<K> getEntry(K item) {
        return this.entries.get(item);
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public int size() {
        return this.entries.size();
    }

    public void clear() {
        this.entries.clear();
    }
}
