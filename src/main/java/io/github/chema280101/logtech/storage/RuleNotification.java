package io.github.chema280101.logtech.storage;

/**
 * Evento de aviso generado cuando una regla con acción NOTIFY se activa por primera vez.
 */
public record RuleNotification<K>(int ruleIndex, AutomationRule<K> rule) {
}
