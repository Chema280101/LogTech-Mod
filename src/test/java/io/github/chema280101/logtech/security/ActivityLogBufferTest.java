package io.github.chema280101.logtech.security;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActivityLogBufferTest {

    private ActivityLogBuffer<String> buffer;
    private UUID playerA;
    private UUID playerB;

    @BeforeEach
    void setUp() {
        buffer = new ActivityLogBuffer<>(200L); // 200 ticks = 10 segundos
        playerA = UUID.randomUUID();
        playerB = UUID.randomUUID();
    }

    @Test
    @DisplayName("Registro básico almacena atributos inmutables correctamente")
    void testRecordBasicAttributes() {
        buffer.record(1000L, playerA, "PlayerA", ActivityAction.INSERT, "minecraft:iron_ingot", 64L, 500);

        assertEquals(1, buffer.size());
        ActivityLogEntry<String> entry = buffer.getEntries().getFirst();
        assertEquals(1000L, entry.gameTime());
        assertEquals(playerA, entry.playerUuid());
        assertEquals("PlayerA", entry.playerName());
        assertEquals(ActivityAction.INSERT, entry.action());
        assertEquals("minecraft:iron_ingot", entry.item());
        assertEquals(64L, entry.amount());
    }

    @Test
    @DisplayName("Acciones repetidas del mismo jugador, ítem y tipo en menos de 10 segundos se agrupan en una sola entrada")
    void testGroupingWithinTenSeconds() {
        // Primera extracción a los 1000 ticks
        buffer.record(1000L, playerA, "PlayerA", ActivityAction.EXTRACT, "minecraft:iron_ingot", 64L, 500);
        // Segunda extracción a los 1100 ticks (5 segundos después, dentro de la ventana de 10s)
        buffer.record(1100L, playerA, "PlayerA", ActivityAction.EXTRACT, "minecraft:iron_ingot", 64L, 500);

        assertEquals(1, buffer.size(), "Debe existir exactamente una entrada agrupada");
        ActivityLogEntry<String> entry = buffer.getEntries().getFirst();
        assertEquals(128L, entry.amount(), "La cantidad debe sumar 64 + 64 = 128");
        assertEquals(1100L, entry.gameTime(), "La marca de tiempo debe actualizarse a la más reciente");
    }

    @Test
    @DisplayName("Un jugador que saca 64 ítems diez veces seguidas ocupa una sola entrada acumulando 640")
    void testTenExtractionsInARow() {
        for (int i = 0; i < 10; i++) {
            // Cada extracción ocurre 1 segundo (20 ticks) después de la anterior
            long time = 1000L + (i * 20L);
            buffer.record(time, playerA, "PlayerA", ActivityAction.EXTRACT, "minecraft:diamond", 64L, 500);
        }

        assertEquals(1, buffer.size(), "Diez extracciones seguidas deben ocupar exactamente una entrada");
        ActivityLogEntry<String> entry = buffer.getEntries().getFirst();
        assertEquals(640L, entry.amount(), "La cantidad total debe ser 640");
        assertEquals(1180L, entry.gameTime(), "La marca de tiempo debe coincidir con la última extracción");
    }

    @Test
    @DisplayName("Dos jugadores operan en la red de forma intercalada y ambos tienen sus acciones agrupadas")
    void testTwoPlayersInterleavedActionsGroupProperly() {
        // PlayerA extrae hierro a tick 1000
        buffer.record(1000L, playerA, "PlayerA", ActivityAction.EXTRACT, "minecraft:iron_ingot", 64L, 500);
        // PlayerB extrae cobre a tick 1020
        buffer.record(1020L, playerB, "PlayerB", ActivityAction.EXTRACT, "minecraft:copper_ingot", 32L, 500);
        // PlayerA extrae hierro de nuevo a tick 1040
        buffer.record(1040L, playerA, "PlayerA", ActivityAction.EXTRACT, "minecraft:iron_ingot", 64L, 500);
        // PlayerB extrae cobre de nuevo a tick 1060
        buffer.record(1060L, playerB, "PlayerB", ActivityAction.EXTRACT, "minecraft:copper_ingot", 32L, 500);

        assertEquals(2, buffer.size(), "Debe haber exactamente dos entradas en el registro");

        // Orden cronológico: PlayerA @ 1040, luego PlayerB @ 1060
        ActivityLogEntry<String> entry1 = buffer.getEntries().get(0);
        ActivityLogEntry<String> entry2 = buffer.getEntries().get(1);

        assertEquals(playerA, entry1.playerUuid());
        assertEquals("minecraft:iron_ingot", entry1.item());
        assertEquals(128L, entry1.amount());
        assertEquals(1040L, entry1.gameTime());

        assertEquals(playerB, entry2.playerUuid());
        assertEquals("minecraft:copper_ingot", entry2.item());
        assertEquals(64L, entry2.amount());
        assertEquals(1060L, entry2.gameTime());
    }

    @Test
    @DisplayName("No se agrupan acciones si el jugador es diferente")
    void testNoGroupingDifferentPlayers() {
        buffer.record(1000L, playerA, "PlayerA", ActivityAction.EXTRACT, "minecraft:iron_ingot", 64L, 500);
        buffer.record(1010L, playerB, "PlayerB", ActivityAction.EXTRACT, "minecraft:iron_ingot", 64L, 500);

        assertEquals(2, buffer.size());
    }

    @Test
    @DisplayName("No se agrupan acciones si el ítem es diferente")
    void testNoGroupingDifferentItems() {
        buffer.record(1000L, playerA, "PlayerA", ActivityAction.EXTRACT, "minecraft:iron_ingot", 64L, 500);
        buffer.record(1010L, playerA, "PlayerA", ActivityAction.EXTRACT, "minecraft:gold_ingot", 64L, 500);

        assertEquals(2, buffer.size());
    }

    @Test
    @DisplayName("No se agrupan acciones si el tipo de acción es diferente")
    void testNoGroupingDifferentActions() {
        buffer.record(1000L, playerA, "PlayerA", ActivityAction.INSERT, "minecraft:iron_ingot", 64L, 500);
        buffer.record(1010L, playerA, "PlayerA", ActivityAction.EXTRACT, "minecraft:iron_ingot", 64L, 500);

        assertEquals(2, buffer.size());
    }

    @Test
    @DisplayName("No se agrupan acciones si transcurren más de 10 segundos (200 ticks)")
    void testNoGroupingAfterTenSeconds() {
        buffer.record(1000L, playerA, "PlayerA", ActivityAction.EXTRACT, "minecraft:iron_ingot", 64L, 500);
        buffer.record(1201L, playerA, "PlayerA", ActivityAction.EXTRACT, "minecraft:iron_ingot", 64L, 500);

        assertEquals(2, buffer.size(), "Más de 200 ticks de diferencia debe crear una nueva entrada");
    }

    @Test
    @DisplayName("Al superar la capacidad máxima del búfer circular, se descartan las más antiguas (FIFO)")
    void testCircularBufferCapacityEnforcement() {
        int maxCap = 5;
        for (int i = 0; i < 8; i++) {
            // Acciones a intervalos largos para evitar agrupación
            long time = i * 1000L;
            buffer.record(time, playerA, "PlayerA", ActivityAction.INSERT, "item_" + i, 1L, maxCap);
        }

        assertEquals(5, buffer.size(), "El tamaño no debe exceder maxCapacity");
        // Las entradas 0, 1 y 2 deben haberse descartado. Quedan 3, 4, 5, 6, 7
        assertEquals("item_3", buffer.getEntries().get(0).item());
        assertEquals("item_7", buffer.getEntries().get(4).item());
    }

    @Test
    @DisplayName("Paginación y filtro por jugador funcionan adecuadamente")
    void testPaginationAndPlayerFiltering() {
        for (int i = 0; i < 25; i++) {
            UUID p = (i % 2 == 0) ? playerA : playerB;
            String name = (i % 2 == 0) ? "Alice" : "Bob";
            buffer.record(i * 1000L, p, name, ActivityAction.INSERT, "item_" + i, 1L, 500);
        }

        assertEquals(25, buffer.size());

        // Paginación sin filtro (10 por página, newestFirst = true)
        ActivityLogBuffer.PageResult<String> page0 = buffer.getPage(0, 10, null, true);
        assertEquals(0, page0.pageIndex());
        assertEquals(3, page0.totalPages());
        assertEquals(25, page0.totalMatchingEntries());
        assertEquals(10, page0.entries().size());
        assertEquals("item_24", page0.entries().getFirst().item(), "La más reciente debe ir primera en newestFirst");

        ActivityLogBuffer.PageResult<String> page2 = buffer.getPage(2, 10, null, true);
        assertEquals(2, page2.pageIndex());
        assertEquals(5, page2.entries().size()); // 25 total: 10 + 10 + 5

        // Filtro por jugador "Alice" (índices pares: 0, 2, ..., 24 -> 13 ítems)
        ActivityLogBuffer.PageResult<String> alicePage = buffer.getPage(0, 10, "alice", true);
        assertEquals(13, alicePage.totalMatchingEntries());
        assertEquals(2, alicePage.totalPages());
        assertEquals(10, alicePage.entries().size());
        for (ActivityLogEntry<String> e : alicePage.entries()) {
            assertEquals("Alice", e.playerName());
        }

        // Filtro sin coincidencias
        ActivityLogBuffer.PageResult<String> noMatch = buffer.getPage(0, 10, "NonExistent", true);
        assertEquals(0, noMatch.totalMatchingEntries());
        assertEquals(0, noMatch.entries().size());
        assertEquals(1, noMatch.totalPages());
    }

    @Test
    @DisplayName("Capacidad 0 o negativa no almacena entradas")
    void testZeroOrNegativeCapacity() {
        buffer.record(1000L, playerA, "PlayerA", ActivityAction.INSERT, "item_1", 1L, 0);
        assertTrue(buffer.isEmpty());
    }
}
