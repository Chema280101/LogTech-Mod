package io.github.chema280101.logtech.machine.firmware;

/**
 * Tipos de chips de firmware disponibles para instalar en máquinas de LogTech.
 */
public enum FirmwareType {
    NONE("none"),
    ECO("eco"),
    TURBO("turbo"),
    SAFE("safe"),
    SELECTIVE("selective");

    private final String id;

    FirmwareType(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public static FirmwareType fromId(String id) {
        if (id == null) {
            return NONE;
        }
        for (FirmwareType type : values()) {
            if (type.id.equalsIgnoreCase(id) || type.name().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return NONE;
    }
}
