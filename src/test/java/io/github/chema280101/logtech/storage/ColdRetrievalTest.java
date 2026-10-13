package io.github.chema280101.logtech.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColdRetrievalTest {

    private SimpleTier<String> coldTier;

    @BeforeEach
    void setUp() {
        coldTier = new SimpleTier<>("cold", 16384L);
    }

    @Test
    @DisplayName("Cuenta atrás del pedido: decrementa por tick y se completa al llegar a 0")
    void testColdOrderCountdown() {
        ColdOrder<String> order = new ColdOrder<>("minecraft:iron_ingot", 64, 40);
        assertEquals(40, order.getTotalTicks());
        assertEquals(40, order.getRemainingTicks());
        assertEquals(0.0f, order.getProgress(), 0.001f);
        assertFalse(order.isComplete());

        for (int i = 0; i < 20; i++) {
            order.tick();
        }
        assertEquals(20, order.getRemainingTicks());
        assertEquals(0.5f, order.getProgress(), 0.001f);
        assertFalse(order.isComplete());

        for (int i = 0; i < 20; i++) {
            order.tick();
        }
        assertEquals(0, order.getRemainingTicks());
        assertEquals(1.0f, order.getProgress(), 0.001f);
        assertTrue(order.isComplete());

        // Ticks adicionales no bajan de 0
        order.tick();
        assertEquals(0, order.getRemainingTicks());
    }

    @Test
    @DisplayName("Crear un pedido no resta ítems del almacenamiento")
    void testOrderCreationDoesNotDeductItems() {
        String key = "minecraft:copper_ingot";
        coldTier.insert(key, 64L, false);

        // Se crea el pedido por 64 ítems
        ColdOrder<String> order = new ColdOrder<>(key, 64, 40);
        assertNotNull(order);

        // El almacenamiento frío debe seguir teniendo exactamente 64
        assertEquals(64L, coldTier.getAmount(key), "El almacenamiento debe permanecer intacto al crear el pedido");
    }

    @Test
    @DisplayName("Entrega completa con stock suficiente y espacio libre en inventario")
    void testSuccessfulDelivery() {
        String key = "minecraft:gold_ingot";
        coldTier.insert(key, 128L, false);

        ColdOrder<String> order = new ColdOrder<>(key, 64, 40);
        long available = coldTier.getAmount(key);
        int space = 64;

        ColdRetrievalLogic.DeliveryResult result =
                ColdRetrievalLogic.evaluateDelivery(order, available, space);

        assertTrue(result.isSuccess());
        assertEquals(ColdRetrievalLogic.DeliveryStatus.SUCCESS, result.status());
        assertEquals(64, result.deliverableAmount());

        // Al ejecutarse la extracción en entrega
        long extracted = coldTier.extract(key, result.deliverableAmount(), false);
        assertEquals(64L, extracted);
        assertEquals(64L, coldTier.getAmount(key));
    }

    @Test
    @DisplayName("Stock parcial: entrega lo que quede si hay menos de lo pedido")
    void testPartialDeliveryWhatRemains() {
        String key = "minecraft:diamond";
        coldTier.insert(key, 25L, false); // Solo hay 25 en frío

        ColdOrder<String> order = new ColdOrder<>(key, 64, 40); // Se pidieron 64
        long available = coldTier.getAmount(key);
        int space = 64;

        ColdRetrievalLogic.DeliveryResult result =
                ColdRetrievalLogic.evaluateDelivery(order, available, space);

        assertTrue(result.isSuccess());
        assertEquals(25, result.deliverableAmount(), "Debe entregar exactamente lo que quede (25 ítems)");

        long extracted = coldTier.extract(key, result.deliverableAmount(), false);
        assertEquals(25L, extracted);
        assertEquals(0L, coldTier.getAmount(key));
    }

    @Test
    @DisplayName("Inventario lleno: si no cabe, no resta nada del frío y devuelve INVENTORY_FULL")
    void testInventoryFullDoesNotDeductItems() {
        String key = "minecraft:redstone";
        coldTier.insert(key, 64L, false);

        ColdOrder<String> order = new ColdOrder<>(key, 64, 40);
        long available = coldTier.getAmount(key);
        int space = 10; // Solo caben 10, pero se piden 64

        ColdRetrievalLogic.DeliveryResult result =
                ColdRetrievalLogic.evaluateDelivery(order, available, space);

        assertFalse(result.isSuccess());
        assertEquals(ColdRetrievalLogic.DeliveryStatus.INVENTORY_FULL, result.status());
        assertEquals(0, result.deliverableAmount());

        // El almacenamiento frío no debe tocarse
        assertEquals(64L, coldTier.getAmount(key), "No debe restarse nada del frío si el inventario no tiene espacio");
    }

    @Test
    @DisplayName("Sin existencias: si se agotó mientras esperaba, devuelve OUT_OF_STOCK")
    void testOutOfStock() {
        String key = "minecraft:lapis_lazuli";
        coldTier.insert(key, 0L, false);

        ColdOrder<String> order = new ColdOrder<>(key, 64, 40);
        long available = coldTier.getAmount(key);
        int space = 64;

        ColdRetrievalLogic.DeliveryResult result =
                ColdRetrievalLogic.evaluateDelivery(order, available, space);

        assertFalse(result.isSuccess());
        assertEquals(ColdRetrievalLogic.DeliveryStatus.OUT_OF_STOCK, result.status());
        assertEquals(0, result.deliverableAmount());
    }

    @Test
    @DisplayName("Anti-duplicación con dos jugadores pidiendo el mismo stack de 64")
    void testTwoPlayersCompetingForSameStackNoDuplication() {
        String key = "minecraft:netherite_ingot";
        coldTier.insert(key, 64L, false); // Solo hay 64 en total en el mod

        // Jugador A y Jugador B piden 64 cada uno casi al mismo tiempo
        ColdOrder<String> orderPlayerA = new ColdOrder<>(key, 64, 40);
        ColdOrder<String> orderPlayerB = new ColdOrder<>(key, 64, 40);

        // Al crear los pedidos, el stock sigue siendo 64 (ninguno resta nada)
        assertEquals(64L, coldTier.getAmount(key));

        // Tick 40: el pedido de Jugador A termina primero
        long availableAtA = coldTier.getAmount(key);
        ColdRetrievalLogic.DeliveryResult resultA =
                ColdRetrievalLogic.evaluateDelivery(orderPlayerA, availableAtA, 64);

        assertTrue(resultA.isSuccess());
        assertEquals(64, resultA.deliverableAmount());
        long deliveredToA = coldTier.extract(key, resultA.deliverableAmount(), false);
        assertEquals(64L, deliveredToA);
        assertEquals(0L, coldTier.getAmount(key));

        // Tick 41: el pedido de Jugador B termina
        long availableAtB = coldTier.getAmount(key);
        assertEquals(0L, availableAtB, "El stock en frío ahora está en 0");

        ColdRetrievalLogic.DeliveryResult resultB =
                ColdRetrievalLogic.evaluateDelivery(orderPlayerB, availableAtB, 64);

        assertFalse(resultB.isSuccess(), "Jugador B no debe recibir items duplicados");
        assertEquals(ColdRetrievalLogic.DeliveryStatus.OUT_OF_STOCK, resultB.status());
        assertEquals(0, resultB.deliverableAmount());

        long totalDelivered = deliveredToA + resultB.deliverableAmount();
        assertEquals(64L, totalDelivered, "El total entregado entre ambos jugadores debe ser exactamente 64");
    }

    @Test
    @DisplayName("Dos jugadores pidiendo 64 con stock de 100: el segundo recibe los 36 restantes sin pérdida ni duplicación")
    void testTwoPlayersCompetingForPartialStock() {
        String key = "minecraft:emerald";
        coldTier.insert(key, 100L, false);

        ColdOrder<String> orderA = new ColdOrder<>(key, 64, 40);
        ColdOrder<String> orderB = new ColdOrder<>(key, 64, 40);

        // Turno de entrega A
        ColdRetrievalLogic.DeliveryResult resultA =
                ColdRetrievalLogic.evaluateDelivery(orderA, coldTier.getAmount(key), 64);
        long deliveredA = coldTier.extract(key, resultA.deliverableAmount(), false);
        assertEquals(64L, deliveredA);
        assertEquals(36L, coldTier.getAmount(key));

        // Turno de entrega B: quedan 36
        ColdRetrievalLogic.DeliveryResult resultB =
                ColdRetrievalLogic.evaluateDelivery(orderB, coldTier.getAmount(key), 64);
        assertTrue(resultB.isSuccess());
        assertEquals(36, resultB.deliverableAmount(), "Jugador B recibe lo que queda (36)");
        long deliveredB = coldTier.extract(key, resultB.deliverableAmount(), false);
        assertEquals(36L, deliveredB);
        assertEquals(0L, coldTier.getAmount(key));

        assertEquals(100L, deliveredA + deliveredB, "Suma entregada debe ser exactamente el stock original de 100");
    }

    @Test
    @DisplayName("Cancelación al cerrar: si el pedido se descarta, no se pierden ítems")
    void testCancellationPreservesItems() {
        String key = "minecraft:amethyst_shard";
        coldTier.insert(key, 50L, false);

        ColdOrder<String> order = new ColdOrder<>(key, 50, 40);
        for (int i = 0; i < 20; i++) {
            order.tick();
        }

        // El jugador cierra el menú -> la cola se descarta
        // Como nunca se restó nada, el almacenamiento sigue intacto al 100%
        assertEquals(50L, coldTier.getAmount(key));
    }
}
