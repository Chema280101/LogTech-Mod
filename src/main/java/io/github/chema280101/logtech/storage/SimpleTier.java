package io.github.chema280101.logtech.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contenedor de almacenamiento puro en memoria para un nivel (caliente o frío).
 * Libre de dependencias de Minecraft para permitir pruebas unitarias y modularidad.
 *
 * @param <K> tipo de clave del ítem
 */
public class SimpleTier<K> {
    private final String name;
    private final Map<K, Long> storage = new HashMap<>();
    private long capacity;

    public SimpleTier(String name) {
        this(name, 0L);
    }

    public SimpleTier(String name, long capacity) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.capacity = Math.max(0L, capacity);
    }

    public String getName() {
        return this.name;
    }

    public long getCapacity() {
        return this.capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = Math.max(0L, capacity);
    }

    public long getTotalStored() {
        return this.storage.values().stream().mapToLong(Long::longValue).sum();
    }

    public long getRemainingSpace() {
        return Math.max(0L, this.capacity - getTotalStored());
    }

    public boolean isOverloaded() {
        return getTotalStored() > this.capacity;
    }

    public long insert(K key, long amount, boolean simulate) {
        if (key == null || amount <= 0) {
            return 0L;
        }
        long space = getRemainingSpace();
        if (space <= 0) {
            return 0L;
        }
        long toInsert = Math.min(amount, space);
        if (!simulate) {
            this.storage.merge(key, toInsert, Long::sum);
        }
        return toInsert;
    }

    public long extract(K key, long amount, boolean simulate) {
        if (key == null || amount <= 0) {
            return 0L;
        }
        long current = this.storage.getOrDefault(key, 0L);
        long toExtract = Math.min(current, amount);
        if (toExtract <= 0) {
            return 0L;
        }
        if (!simulate) {
            if (current == toExtract) {
                this.storage.remove(key);
            } else {
                this.storage.put(key, current - toExtract);
            }
        }
        return toExtract;
    }

    /**
     * Mueve atómicamente ítems hacia otro nivel garantizando conservación estricta.
     */
    public long moveTo(SimpleTier<K> target, K key, long maxAmount) {
        if (target == null || key == null || maxAmount <= 0) {
            return 0L;
        }
        long available = getAmount(key);
        if (available <= 0) {
            return 0L;
        }
        long targetSpace = target.getRemainingSpace();
        if (targetSpace <= 0) {
            return 0L;
        }
        long toMove = Math.min(maxAmount, Math.min(available, targetSpace));
        if (toMove <= 0) {
            return 0L;
        }
        long extracted = this.extract(key, toMove, false);
        if (extracted > 0) {
            long inserted = target.insert(key, extracted, false);
            if (inserted < extracted) {
                this.insert(key, extracted - inserted, false);
                return inserted;
            }
            return extracted;
        }
        return 0L;
    }

    public long getAmount(K key) {
        return this.storage.getOrDefault(key, 0L);
    }

    public Map<K, Long> getAll() {
        return Collections.unmodifiableMap(this.storage);
    }

    public boolean isEmpty() {
        return this.storage.isEmpty();
    }

    public void clear() {
        this.storage.clear();
    }

    public void setDirect(K key, long amount) {
        if (amount > 0) {
            this.storage.put(key, amount);
        } else {
            this.storage.remove(key);
        }
    }
}
