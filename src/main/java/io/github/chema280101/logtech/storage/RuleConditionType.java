package io.github.chema280101.logtech.storage;

/**
 * Tipos de condición para las reglas de automatización en el Núcleo.
 */
public enum RuleConditionType {
    ITEM_COUNT_GREATER("item_greater"),
    ITEM_COUNT_LESS("item_less"),
    TOTAL_SPACE_GREATER("space_greater"),
    TOTAL_SPACE_LESS("space_less");

    private final String serializedName;

    RuleConditionType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public boolean requiresItem() {
        return this == ITEM_COUNT_GREATER || this == ITEM_COUNT_LESS;
    }

    public static RuleConditionType fromSerialized(String name) {
        for (RuleConditionType type : values()) {
            if (type.serializedName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return ITEM_COUNT_GREATER;
    }
}
