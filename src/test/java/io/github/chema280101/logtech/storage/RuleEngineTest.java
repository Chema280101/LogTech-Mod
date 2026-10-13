package io.github.chema280101.logtech.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private RuleEngine<String> engine;

    @BeforeEach
    void setUp() {
        engine = new RuleEngine<>();
    }

    @Test
    @DisplayName("Criterio: Hierro > 5000 activa MOVE_TO_COLD y lo mantiene allí")
    void testItemCountGreaterActivatesAndMovesToCold() {
        String iron = "minecraft:iron_ingot";
        AutomationRule<String> rule = new AutomationRule<>(
                0,
                iron,
                RuleConditionType.ITEM_COUNT_GREATER,
                5000L,
                RuleAction.MOVE_TO_COLD,
                true
        );
        engine.setRule(0, rule);

        Map<String, Long> items = new HashMap<>();
        items.put(iron, 5000L);
        RuleEngine.NetworkState<String> state = new RuleEngine.NetworkState<>(items, 5000L, 10000L);

        // Con 5000 justo en el umbral, no debe activarse (es mayor estricto)
        RuleEvaluationResult<String> result1 = engine.evaluate(state);
        assertFalse(result1.isColdActive(iron));
        assertFalse(engine.isItemMoveToColdActive(iron));

        // Con 5001, supera el umbral y se activa
        items.put(iron, 5001L);
        state = new RuleEngine.NetworkState<>(items, 5001L, 10000L);
        RuleEvaluationResult<String> result2 = engine.evaluate(state);
        assertTrue(result2.isColdActive(iron));
        assertTrue(engine.isItemMoveToColdActive(iron));
    }

    @Test
    @DisplayName("Histéresis anti-parpadeo: límite oscilante 5000-5001 no conmuta la acción en cada ciclo")
    void testItemCountHysteresisPreventsFlapping() {
        String iron = "minecraft:iron_ingot";
        AutomationRule<String> rule = new AutomationRule<>(
                0,
                iron,
                RuleConditionType.ITEM_COUNT_GREATER,
                5000L,
                RuleAction.MOVE_TO_COLD,
                true
        );
        engine.setRule(0, rule);

        Map<String, Long> items = new HashMap<>();

        // Se activa al superar 5000
        items.put(iron, 5001L);
        engine.evaluate(new RuleEngine.NetworkState<>(items, 5001L, 10000L));
        assertTrue(engine.isItemMoveToColdActive(iron));

        // Oscilación justo en el límite (5000): la histéresis (umbral - 5% = 4750) evita el parpadeo
        items.put(iron, 5000L);
        engine.evaluate(new RuleEngine.NetworkState<>(items, 5000L, 10000L));
        assertTrue(engine.isItemMoveToColdActive(iron), "Debe mantenerse activo en 5000 gracias a la histéresis");

        items.put(iron, 4900L);
        engine.evaluate(new RuleEngine.NetworkState<>(items, 4900L, 10000L));
        assertTrue(engine.isItemMoveToColdActive(iron), "Debe mantenerse activo en 4900 dentro de la banda muerta");

        // Al caer por debajo de 4750 (5000 - 250), se desactiva limpiamente
        items.put(iron, 4749L);
        engine.evaluate(new RuleEngine.NetworkState<>(items, 4749L, 10000L));
        assertFalse(engine.isItemMoveToColdActive(iron), "Debe desactivarse al cruzar el margen inferior");

        // Si sube a 4900 no debe reactivarse hasta que supere nuevamente 5000
        items.put(iron, 4900L);
        engine.evaluate(new RuleEngine.NetworkState<>(items, 4900L, 10000L));
        assertFalse(engine.isItemMoveToColdActive(iron), "No debe reactivarse hasta superar 5000");
    }

    @Test
    @DisplayName("Criterio: Espacio usado > 90 % dispara aviso una vez y se rearma al bajar")
    void testSpaceUsedGreaterNotifyTriggersOnceAndRearms() {
        AutomationRule<String> rule = new AutomationRule<>(
                1,
                null,
                RuleConditionType.TOTAL_SPACE_GREATER,
                90L,
                RuleAction.NOTIFY,
                true
        );
        engine.setRule(1, rule);

        // 89% (8900 / 10000): inactivo, cero avisos
        RuleEngine.NetworkState<String> state1 = new RuleEngine.NetworkState<>(Map.of(), 8900L, 10000L);
        RuleEvaluationResult<String> res1 = engine.evaluate(state1);
        assertEquals(0, res1.notifications().size());

        // 91% (9100 / 10000): se cumple condición -> dispara aviso exactamente 1 vez
        RuleEngine.NetworkState<String> state2 = new RuleEngine.NetworkState<>(Map.of(), 9100L, 10000L);
        RuleEvaluationResult<String> res2 = engine.evaluate(state2);
        assertEquals(1, res2.notifications().size());
        assertEquals(1, res2.notifications().get(0).ruleIndex());

        // Ciclos siguientes manteniéndose en 92% y 91%: la regla sigue activa pero NO repite el aviso
        RuleEngine.NetworkState<String> state3 = new RuleEngine.NetworkState<>(Map.of(), 9200L, 10000L);
        RuleEvaluationResult<String> res3 = engine.evaluate(state3);
        assertEquals(0, res3.notifications().size(), "No debe disparar avisos repetidos mientras siga activa");

        RuleEngine.NetworkState<String> state4 = new RuleEngine.NetworkState<>(Map.of(), 9050L, 10000L);
        RuleEvaluationResult<String> res4 = engine.evaluate(state4);
        assertEquals(0, res4.notifications().size());
        assertTrue(engine.isRuleActive(1), "Sigue activa por histéresis (90 - 2% = 88%)");

        // Al bajar a 87% (debajo de 88%), se desactiva y se rearma
        RuleEngine.NetworkState<String> state5 = new RuleEngine.NetworkState<>(Map.of(), 8700L, 10000L);
        RuleEvaluationResult<String> res5 = engine.evaluate(state5);
        assertEquals(0, res5.notifications().size());
        assertFalse(engine.isRuleActive(1));

        // Al volver a subir a 93%, se dispara nuevamente una vez
        RuleEngine.NetworkState<String> state6 = new RuleEngine.NetworkState<>(Map.of(), 9300L, 10000L);
        RuleEvaluationResult<String> res6 = engine.evaluate(state6);
        assertEquals(1, res6.notifications().size(), "Debe disparar nuevamente tras haberse rearmado");
    }

    @Test
    @DisplayName("Criterio: Regla que requiere ítem sin ítem asignado se ignora")
    void testRuleWithoutItemIsIgnored() {
        // Regla con condición de conteo pero sin ítem
        AutomationRule<String> ruleNoItem = new AutomationRule<>(
                0,
                null,
                RuleConditionType.ITEM_COUNT_GREATER,
                100L,
                RuleAction.MOVE_TO_COLD,
                true
        );
        engine.setRule(0, ruleNoItem);

        assertFalse(ruleNoItem.isValid());
        RuleEvaluationResult<String> res = engine.evaluate(new RuleEngine.NetworkState<>(Map.of(), 500L, 1000L));
        assertFalse(engine.isRuleActive(0));
        assertEquals(0, res.activeColdItems().size());
    }

    @Test
    @DisplayName("Regla de espacio sin ítem funciona para NOTIFY y REDSTONE_SIGNAL")
    void testSpaceRuleWithoutItemWorks() {
        AutomationRule<String> redstoneRule = new AutomationRule<>(
                2,
                null,
                RuleConditionType.TOTAL_SPACE_GREATER,
                80L,
                RuleAction.REDSTONE_SIGNAL,
                true
        );
        engine.setRule(2, redstoneRule);
        assertTrue(redstoneRule.isValid(), "Regla de espacio no requiere ítem");

        RuleEvaluationResult<String> res = engine.evaluate(new RuleEngine.NetworkState<>(Map.of(), 850L, 1000L));
        assertTrue(res.redstoneSignalActive());
        assertTrue(engine.isRedstoneSignalActive());
    }

    @Test
    @DisplayName("Condición ITEM_COUNT_LESS evalúa correctamente e implementa histéresis superior")
    void testItemCountLessCondition() {
        String gold = "minecraft:gold_ingot";
        AutomationRule<String> rule = new AutomationRule<>(
                3,
                gold,
                RuleConditionType.ITEM_COUNT_LESS,
                100L,
                RuleAction.NOTIFY,
                true
        );
        engine.setRule(3, rule);

        // 120 items: inactivo
        Map<String, Long> items = new HashMap<>();
        items.put(gold, 120L);
        RuleEvaluationResult<String> r1 = engine.evaluate(new RuleEngine.NetworkState<>(items, 120L, 1000L));
        assertFalse(engine.isRuleActive(3));
        assertEquals(0, r1.notifications().size());

        // Cae a 90 items (< 100): se activa y notifica 1 vez
        items.put(gold, 90L);
        RuleEvaluationResult<String> r2 = engine.evaluate(new RuleEngine.NetworkState<>(items, 90L, 1000L));
        assertTrue(engine.isRuleActive(3));
        assertEquals(1, r2.notifications().size());

        // Sube a 100 items: por histéresis (100 + 5% = 105), se mantiene activo
        items.put(gold, 100L);
        RuleEvaluationResult<String> r3 = engine.evaluate(new RuleEngine.NetworkState<>(items, 100L, 1000L));
        assertTrue(engine.isRuleActive(3));
        assertEquals(0, r3.notifications().size());

        // Sube a 106 items: se desactiva y se rearma
        items.put(gold, 106L);
        RuleEvaluationResult<String> r4 = engine.evaluate(new RuleEngine.NetworkState<>(items, 106L, 1000L));
        assertFalse(engine.isRuleActive(3));
    }

    @Test
    @DisplayName("Regla deshabilitada nunca se activa")
    void testDisabledRuleNeverActivates() {
        String iron = "minecraft:iron_ingot";
        AutomationRule<String> rule = new AutomationRule<>(
                4,
                iron,
                RuleConditionType.ITEM_COUNT_GREATER,
                10L,
                RuleAction.MOVE_TO_COLD,
                false // disabled
        );
        engine.setRule(4, rule);

        Map<String, Long> items = Map.of(iron, 1000L);
        RuleEvaluationResult<String> res = engine.evaluate(new RuleEngine.NetworkState<>(items, 1000L, 2000L));
        assertFalse(engine.isRuleActive(4));
        assertFalse(res.isColdActive(iron));
    }

    @Test
    @DisplayName("Criterio T29: Canales de redstone del 1 al 4 se activan de forma independiente")
    void testRedstoneChannelsIndependentActivation() {
        // Regla 0: canal 2 cuando espacio > 50%
        engine.setRule(0, new AutomationRule<>(0, null, RuleConditionType.TOTAL_SPACE_GREATER, 50L, RuleAction.REDSTONE_2, true));
        // Regla 1: canal 4 cuando hierro > 500
        engine.setRule(1, new AutomationRule<>(1, "minecraft:iron_ingot", RuleConditionType.ITEM_COUNT_GREATER, 500L, RuleAction.REDSTONE_4, true));

        // Estado inicial: espacio al 40%, hierro en 200 -> ninguno activo
        Map<String, Long> items = Map.of("minecraft:iron_ingot", 200L);
        RuleEvaluationResult<String> res1 = engine.evaluate(new RuleEngine.NetworkState<>(items, 400L, 1000L));
        assertFalse(res1.isChannelActive(1));
        assertFalse(res1.isChannelActive(2));
        assertFalse(res1.isChannelActive(3));
        assertFalse(res1.isChannelActive(4));
        assertFalse(engine.isRedstoneSignalActive());

        // Espacio sube al 60% pero hierro sigue en 200 -> solo canal 2 activo
        RuleEvaluationResult<String> res2 = engine.evaluate(new RuleEngine.NetworkState<>(items, 600L, 1000L));
        assertFalse(res2.isChannelActive(1));
        assertTrue(res2.isChannelActive(2));
        assertFalse(res2.isChannelActive(3));
        assertFalse(res2.isChannelActive(4));
        assertTrue(engine.isRedstoneSignalActive(2));
        assertFalse(engine.isRedstoneSignalActive(4));

        // Hierro sube a 600 -> canales 2 y 4 activos simultáneamente
        items = Map.of("minecraft:iron_ingot", 600L);
        RuleEvaluationResult<String> res3 = engine.evaluate(new RuleEngine.NetworkState<>(items, 600L, 1000L));
        assertTrue(res3.isChannelActive(2));
        assertTrue(res3.isChannelActive(4));
        assertTrue(engine.getActiveRedstoneChannels().containsAll(java.util.Set.of(2, 4)));

        // Espacio cae al 40% (debajo de 50 - 2% = 48%) -> canal 2 se apaga, canal 4 sigue activo
        RuleEvaluationResult<String> res4 = engine.evaluate(new RuleEngine.NetworkState<>(items, 400L, 1000L));
        assertFalse(res4.isChannelActive(2));
        assertTrue(res4.isChannelActive(4));
    }
}
