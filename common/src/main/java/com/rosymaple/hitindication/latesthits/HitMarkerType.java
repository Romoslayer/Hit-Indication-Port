package com.rosymaple.hitindication.latesthits;

import org.jspecify.annotations.Nullable;

public enum HitMarkerType {
    CRIT(0), KILL(1);

    final int type;

    HitMarkerType(int type) {
        this.type = type;
    }

    public int id() {
        return type;
    }

    /** @return null for an id this version does not know */
    public static @Nullable HitMarkerType fromInt(int integerType) {
        return switch (integerType) {
            case 0 -> HitMarkerType.CRIT;
            case 1 -> HitMarkerType.KILL;
            default -> null;
        };
    }
}
