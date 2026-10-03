package com.rosymaple.hitindication.networking;

import com.rosymaple.hitindication.HitIndication;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: a hit indicator pointing at (x, y, z). */
public record AddHitIndicatorS2CPacket(double x, double y, double z, int indicatorType, int damagePercent, boolean negativeEffectPotion)
        implements CustomPacketPayload {
    public static final Type<AddHitIndicatorS2CPacket> TYPE = new Type<>(HitIndication.id("add_hit_indicator"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddHitIndicatorS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, AddHitIndicatorS2CPacket::x,
            ByteBufCodecs.DOUBLE, AddHitIndicatorS2CPacket::y,
            ByteBufCodecs.DOUBLE, AddHitIndicatorS2CPacket::z,
            ByteBufCodecs.VAR_INT, AddHitIndicatorS2CPacket::indicatorType,
            ByteBufCodecs.VAR_INT, AddHitIndicatorS2CPacket::damagePercent,
            ByteBufCodecs.BOOL, AddHitIndicatorS2CPacket::negativeEffectPotion,
            AddHitIndicatorS2CPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
