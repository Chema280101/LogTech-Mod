package io.github.chema280101.logtech.machine.recipe;

public enum OutputScaling {
    NONE("none"),
    INVERSE_PURITY("inverse_purity");

    private final String serializedName;

    OutputScaling(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public static OutputScaling fromName(String name) {
        if (name != null) {
            for (OutputScaling scaling : values()) {
                if (scaling.serializedName.equalsIgnoreCase(name)) {
                    return scaling;
                }
            }
        }
        return NONE;
    }
}
