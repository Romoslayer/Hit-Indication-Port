package com.rosymaple.hitindication.networking;

import com.rosymaple.hitindication.HitIndication;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: show a crit or kill marker at the crosshair. */
public record SetHitMarkerS2CPacket(int markerType) implements CustomPacketPayload {
    public static final Type<SetHitMarkerS2CPacket> TYPE = new Type<>(HitIndication.id("set_hit_marker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetHitMarkerS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetHitMarkerS2CPacket::markerType,
            SetHitMarkerS2CPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
