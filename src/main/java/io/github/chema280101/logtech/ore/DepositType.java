package io.github.chema280101.logtech.ore;

public enum DepositType {
    NEOFERRITE("neoferrite", "Neoferrita", "Neoferrite"),
    CRYSTALITE("crystalite", "Cristalita", "Crystalite");

    private final String id;
    private final String spanishName;
    private final String englishName;

    DepositType(String id, String spanishName, String englishName) {
        this.id = id;
        this.spanishName = spanishName;
        this.englishName = englishName;
    }

    public String getId() {
        return id;
    }

    public String getSpanishName() {
        return spanishName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public static DepositType fromId(String id) {
        for (DepositType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return NEOFERRITE;
    }
}
