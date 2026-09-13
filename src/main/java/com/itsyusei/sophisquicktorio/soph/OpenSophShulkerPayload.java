package com.itsyusei.sophisquicktorio.soph;

import com.itsyusei.sophisquicktorio.SophisQuickTorio;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -&gt; server request to quick-open a Soph Storage shulker from inventory. */
public record OpenSophShulkerPayload(byte kind, int slot, int data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenSophShulkerPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(SophisQuickTorio.MOD_ID, "open_soph_shulker"));

    public static final StreamCodec<FriendlyByteBuf, OpenSophShulkerPayload> CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.BYTE, OpenSophShulkerPayload::kind,
                    net.minecraft.network.codec.ByteBufCodecs.INT, OpenSophShulkerPayload::slot,
                    net.minecraft.network.codec.ByteBufCodecs.INT, OpenSophShulkerPayload::data,
                    OpenSophShulkerPayload::new);

    public OpenSophShulkerPayload(SophShulker.Host host) {
        this(host.kind, host.slot, host.data);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerPlayer player, OpenSophShulkerPayload payload) {
        try {
            SophShulker.Host host = new SophShulker.Host(payload.kind(), payload.slot(), payload.data());
            SophMenus.open(player, host);
        } catch (Throwable t) {
            SophisQuickTorio.LOGGER.warn("[sqt] soph open request failed: {}", t.toString());
        }
    }

    public static void handleOnNet(OpenSophShulkerPayload payload, IPayloadContext context) {
        try {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                context.enqueueWork(() -> handle(serverPlayer, payload));
            }
        } catch (Throwable t) {
            SophisQuickTorio.LOGGER.warn("[sqt] soph payload dispatch failed: {}", t.toString());
        }
    }
}
