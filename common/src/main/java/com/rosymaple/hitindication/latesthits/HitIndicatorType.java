package com.rosymaple.hitindication.latesthits;

import org.jspecify.annotations.Nullable;

public enum HitIndicatorType {
    HIT(0), BLOCK(1), ND_HIT(2);

    final int type;

    HitIndicatorType(int type) {
        this.type = type;
    }

    public int id() {
        return type;
    }

    /** @return null for an id this version does not know */
    public static @Nullable HitIndicatorType fromInt(int integerType) {
        return switch (integerType) {
            case 0 -> HitIndicatorType.HIT;
            case 1 -> HitIndicatorType.BLOCK;
            case 2 -> HitIndicatorType.ND_HIT;
            default -> null;
        };
    }
}
