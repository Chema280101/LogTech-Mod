package io.github.chema280101.logtech.storage;

import java.util.Objects;

/**
 * Lógica pura desacoplada para la resolución y entrega de pedidos desde el nivel frío.
 * <p>
 * Reglas de diseño:
 * 1. Al crearse el pedido no se descuenta nada del almacenamiento.
 * 2. La comprobación de existencias se hace en el momento de la entrega ("entrega lo que quede").
 * 3. Si no cabe en el inventario del jugador, no se resta nada y se avisa.
 * 4. Evita duplicaciones o pérdidas cuando varios jugadores piden sobre el mismo stock de ítems.
 */
public final class ColdRetrievalLogic {

    public enum DeliveryStatus {
        SUCCESS,
        OUT_OF_STOCK,
        INVENTORY_FULL
    }

    public record DeliveryResult(
            int deliverableAmount,
            DeliveryStatus status
    ) {
        public boolean isSuccess() {
            return status == DeliveryStatus.SUCCESS;
        }
    }

    private ColdRetrievalLogic() {
    }

    /**
     * Evalúa si un pedido completado puede entregarse según las existencias reales en frío
     * y el espacio disponible en el inventario del jugador.
     *
     * @param order pedido completado
     * @param availableInCold cantidad actual existente en el nivel frío para esa clave
     * @param availableInventorySpace espacio libre en el inventario del jugador para ese tipo de ítem
     * @return resultado que indica si se entrega y cuántos ítems deben extraerse, o el motivo de fallo
     */
    public static <K> DeliveryResult evaluateDelivery(
            ColdOrder<K> order,
            long availableInCold,
            int availableInventorySpace
    ) {
        Objects.requireNonNull(order, "order cannot be null");

        // 1. Comprobar existencias disponibles en frío
        if (availableInCold <= 0) {
            return new DeliveryResult(0, DeliveryStatus.OUT_OF_STOCK);
        }

        // 2. Determinar cantidad a entregar ("entrega lo que quede", hasta el máximo pedido)
        int toDeliver = (int) Math.min((long) order.getRequestedAmount(), availableInCold);
        if (toDeliver <= 0) {
            return new DeliveryResult(0, DeliveryStatus.OUT_OF_STOCK);
        }

        // 3. Comprobar espacio en el inventario: "Si no cabe, no resta nada y avisa"
        if (availableInventorySpace < toDeliver) {
            return new DeliveryResult(0, DeliveryStatus.INVENTORY_FULL);
        }

        return new DeliveryResult(toDeliver, DeliveryStatus.SUCCESS);
    }
}
