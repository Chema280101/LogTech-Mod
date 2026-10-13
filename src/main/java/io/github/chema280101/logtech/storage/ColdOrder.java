package io.github.chema280101.logtech.storage;

import java.util.Objects;

/**
 * Representa un pedido pendiente de extracción de ítems desde el nivel frío de almacenamiento.
 * Un pedido NO descuenta ítems al crearse; espera un número determinado de ticks antes de
 * evaluarse y entregarse atómicamente.
 *
 * @param <K> tipo de clave de ítem (típicamente {@link ItemKey} o String en tests)
 */
public class ColdOrder<K> {
    private final K key;
    private final int requestedAmount;
    private final int totalTicks;
    private int remainingTicks;

    public ColdOrder(K key, int requestedAmount, int totalTicks) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.requestedAmount = Math.max(1, requestedAmount);
        this.totalTicks = Math.max(0, totalTicks);
        this.remainingTicks = this.totalTicks;
    }

    public K getKey() {
        return key;
    }

    public int getRequestedAmount() {
        return requestedAmount;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    /**
     * Hace avanzar un tick la cuenta atrás del pedido.
     */
    public void tick() {
        if (this.remainingTicks > 0) {
            this.remainingTicks--;
        }
    }

    /**
     * @return true si la cuenta atrás ha finalizado y el pedido está listo para ser entregado.
     */
    public boolean isComplete() {
        return this.remainingTicks <= 0;
    }

    /**
     * Progreso normalizado de 0.0 (recién creado) a 1.0 (completo).
     */
    public float getProgress() {
        if (totalTicks <= 0) {
            return 1.0f;
        }
        return Math.min(1.0f, Math.max(0.0f, (float) (totalTicks - remainingTicks) / totalTicks));
    }
}
