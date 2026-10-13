package io.github.chema280101.logtech.machine.recipe;

public enum PurityMode {
    PRESERVE("preserve"),
    INCREASE("increase");

    private final String serializedName;

    PurityMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public static PurityMode fromName(String name) {
        if (name != null) {
            for (PurityMode mode : values()) {
                if (mode.serializedName.equalsIgnoreCase(name)) {
                    return mode;
                }
            }
        }
        return PRESERVE;
    }
}
