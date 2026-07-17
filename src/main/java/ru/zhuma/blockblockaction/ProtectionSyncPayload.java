package ru.zhuma.blockblockaction;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Complete server-to-client protection snapshot used by the wand overlays. */
public record ProtectionSyncPayload(List<BlockEntry> blocks, List<EntityEntry> entities) implements CustomPacketPayload {
    public static final Type<ProtectionSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BlockBlockAction.MOD_ID, "protection_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ProtectionSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ProtectionSyncPayload decode(RegistryFriendlyByteBuf buffer) {
            int blockCount = readCount(buffer, "block");
            List<BlockEntry> blocks = new ArrayList<>(blockCount);
            for (int i = 0; i < blockCount; i++) {
                blocks.add(new BlockEntry(buffer.readBlockPos(), buffer.readUnsignedByte()));
            }

            int entityCount = readCount(buffer, "entity");
            List<EntityEntry> entities = new ArrayList<>(entityCount);
            for (int i = 0; i < entityCount; i++) {
                entities.add(new EntityEntry(new UUID(buffer.readLong(), buffer.readLong()), buffer.readUnsignedByte()));
            }
            return new ProtectionSyncPayload(List.copyOf(blocks), List.copyOf(entities));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ProtectionSyncPayload payload) {
            buffer.writeVarInt(payload.blocks.size());
            for (BlockEntry entry : payload.blocks) {
                buffer.writeBlockPos(entry.position);
                buffer.writeByte(entry.stateId);
            }
            buffer.writeVarInt(payload.entities.size());
            for (EntityEntry entry : payload.entities) {
                buffer.writeLong(entry.entityId.getMostSignificantBits());
                buffer.writeLong(entry.entityId.getLeastSignificantBits());
                buffer.writeByte(entry.stateId);
            }
        }

        private static int readCount(RegistryFriendlyByteBuf buffer, String kind) {
            int count = buffer.readVarInt();
            if (count < 0 || count > 100_000) {
                throw new IllegalArgumentException("Invalid protected-" + kind + " sync size: " + count);
            }
            return count;
        }
    };

    @Override
    public Type<ProtectionSyncPayload> type() {
        return TYPE;
    }

    public static void handleOnClient(ProtectionSyncPayload payload, IPayloadContext context) {
        ClientProtectionCache.replace(payload.blocks, payload.entities);
    }

    public record BlockEntry(BlockPos position, int stateId) {
    }

    public record EntityEntry(UUID entityId, int stateId) {
    }
}
