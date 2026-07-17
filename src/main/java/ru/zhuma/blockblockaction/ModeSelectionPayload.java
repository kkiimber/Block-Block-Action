package ru.zhuma.blockblockaction;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-to-server selected wand mode update. */
public record ModeSelectionPayload(int stateId) implements CustomPacketPayload {
    public static final Type<ModeSelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BlockBlockAction.MOD_ID, "select_protection_mode")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ModeSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT,
            ModeSelectionPayload::stateId,
            ModeSelectionPayload::new
    );

    @Override
    public Type<ModeSelectionPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(ModeSelectionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            BlockBlockAction.protectionManager().setSelectedMode(player, ProtectionState.fromId(payload.stateId));
        }
    }
}
