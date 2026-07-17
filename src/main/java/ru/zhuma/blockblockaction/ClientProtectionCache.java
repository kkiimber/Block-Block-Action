package ru.zhuma.blockblockaction;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Client-only copy of the Overworld protection state. */
public final class ClientProtectionCache {
    private static Map<BlockPos, ProtectionState> protections = Map.of();
    private static Map<UUID, ProtectionState> entityProtections = Map.of();

    public static void replace(List<ProtectionSyncPayload.BlockEntry> entries, List<ProtectionSyncPayload.EntityEntry> entityEntries) {
        Map<BlockPos, ProtectionState> updatedProtections = new HashMap<>();
        for (ProtectionSyncPayload.BlockEntry entry : entries) {
            updatedProtections.put(
                    entry.position().immutable(),
                    ProtectionState.fromId(entry.stateId())
            );
        }
        protections = Map.copyOf(updatedProtections);

        Map<UUID, ProtectionState> updatedEntityProtections = new HashMap<>();
        for (ProtectionSyncPayload.EntityEntry entry : entityEntries) {
            ProtectionState state = ProtectionState.fromId(entry.stateId());
            if (state.isProtected()) {
                updatedEntityProtections.put(entry.entityId(), state);
            }
        }
        entityProtections = Map.copyOf(updatedEntityProtections);
    }

    public static Map<BlockPos, ProtectionState> protections() {
        return protections;
    }

    public static Map<UUID, ProtectionState> entityProtections() {
        return entityProtections;
    }

    private ClientProtectionCache() {
    }
}
