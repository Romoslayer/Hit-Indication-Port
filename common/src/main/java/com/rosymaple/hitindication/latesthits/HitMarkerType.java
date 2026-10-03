package com.rosymaple.hitindication.latesthits;

public enum HitMarkerType {
    CRIT(0), KILL(1);

    final int type;

    HitMarkerType(int type) {
        this.type = type;
    }

    public int id() {
        return type;
    }

    public static HitMarkerType fromInt(int integerType) {
        return switch (integerType) {
            case 1 -> HitMarkerType.KILL;
            default -> HitMarkerType.CRIT;
        };
    }
}
