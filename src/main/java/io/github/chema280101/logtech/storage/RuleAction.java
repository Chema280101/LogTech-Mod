package io.github.chema280101.logtech.storage;

/**
 * Acciones ejecutables por las reglas de automatización en el Núcleo.
 */
public enum RuleAction {
    MOVE_TO_COLD("move_to_cold", 0),
    NOTIFY("notify", 0),
    REDSTONE_SIGNAL("redstone_signal", 1),
    REDSTONE_2("redstone_2", 2),
    REDSTONE_3("redstone_3", 3),
    REDSTONE_4("redstone_4", 4);

    public static final RuleAction REDSTONE_1 = REDSTONE_SIGNAL;

    private final String serializedName;
    private final int channel;

    RuleAction(String serializedName, int channel) {
        this.serializedName = serializedName;
        this.channel = channel;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public int getRedstoneChannel() {
        return channel;
    }

    public boolean isRedstone() {
        return channel >= 1 && channel <= 4;
    }

    public boolean requiresItem() {
        return this == MOVE_TO_COLD;
    }

    public static RuleAction fromSerialized(String name) {
        if (name == null) {
            return MOVE_TO_COLD;
        }
        if ("redstone_1".equalsIgnoreCase(name) || "redstone".equalsIgnoreCase(name)) {
            return REDSTONE_SIGNAL;
        }
        for (RuleAction action : values()) {
            if (action.serializedName.equalsIgnoreCase(name) || action.name().equalsIgnoreCase(name)) {
                return action;
            }
        }
        return MOVE_TO_COLD;
    }
}
