package ru.zhuma.blockblockaction;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Stores server-authoritative block and entity protection state for the Overworld. */
public final class ProtectionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BLOCK_FILE_NAME = "protected_blocks.dat";
    private static final String ENTITY_FILE_NAME = "protected_entities.dat";

    private final Map<BlockPos, ProtectionState> blockProtections = new HashMap<>();
    private final Map<UUID, ProtectionState> entityProtections = new HashMap<>();
    private final Map<UUID, ProtectionState> selectedModes = new HashMap<>();
    private final Map<UUID, BlockPos> pendingAreaStarts = new HashMap<>();

    public void load(ServerLevel level) {
        blockProtections.clear();
        entityProtections.clear();
        loadBlocks(getFile(level, BLOCK_FILE_NAME));
        loadEntities(getFile(level, ENTITY_FILE_NAME));
    }

    public void save(ServerLevel level) {
        Path dataDirectory = getFile(level, BLOCK_FILE_NAME).getParent();
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException exception) {
            LOGGER.error("Could not create protected-block data directory {}", dataDirectory, exception);
            return;
        }

        saveBlocks(getFile(level, BLOCK_FILE_NAME));
        saveEntities(getFile(level, ENTITY_FILE_NAME));
    }

    public ProtectionState selectedMode(ServerPlayer player) {
        return selectedModes.getOrDefault(player.getUUID(), ProtectionState.BREAK_ONLY);
    }

    public void setSelectedMode(ServerPlayer player, ProtectionState state) {
        selectedModes.put(player.getUUID(), state);
    }

    public boolean beginOrCancelArea(ServerPlayer player, BlockPos position) {
        UUID playerId = player.getUUID();
        if (pendingAreaStarts.remove(playerId) != null) {
            return false;
        }
        pendingAreaStarts.put(playerId, position.immutable());
        return true;
    }

    public BlockPos finishArea(ServerPlayer player) {
        return pendingAreaStarts.remove(player.getUUID());
    }

    public void setBlockProtection(BlockPos position, ProtectionState state) {
        BlockPos immutablePosition = position.immutable();
        if (state.isProtected()) {
            blockProtections.put(immutablePosition, state);
        } else {
            blockProtections.remove(immutablePosition);
        }
    }

    public void setBlockProtectionInArea(BlockPos first, BlockPos second, ProtectionState state) {
        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX());
        int maxY = Math.max(first.getY(), second.getY());
        int maxZ = Math.max(first.getZ(), second.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    setBlockProtection(new BlockPos(x, y, z), state);
                }
            }
        }
    }

    public Optional<ProtectionState> getBlockProtection(BlockPos position) {
        return Optional.ofNullable(blockProtections.get(position));
    }

    public boolean isBlockProtected(BlockPos position) {
        return blockProtections.containsKey(position);
    }

    public void setEntityProtection(Entity entity, ProtectionState state) {
        ProtectionState entityState = state.forEntity();
        if (entityState.isProtected()) {
            entityProtections.put(entity.getUUID(), entityState);
        } else {
            entityProtections.remove(entity.getUUID());
        }
    }

    public Optional<ProtectionState> getEntityProtection(Entity entity) {
        return Optional.ofNullable(entityProtections.get(entity.getUUID()));
    }

    public boolean isEntityProtected(Entity entity) {
        return entityProtections.containsKey(entity.getUUID());
    }

    public void syncToAllPlayers() {
        PacketDistributor.sendToAllPlayers(new ProtectionSyncPayload(createBlockEntries(), createEntityEntries()));
    }

    public void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ProtectionSyncPayload(createBlockEntries(), createEntityEntries()));
    }

    private List<ProtectionSyncPayload.BlockEntry> createBlockEntries() {
        return blockProtections.entrySet().stream()
                .map(entry -> new ProtectionSyncPayload.BlockEntry(entry.getKey(), entry.getValue().ordinal()))
                .toList();
    }

    private List<ProtectionSyncPayload.EntityEntry> createEntityEntries() {
        return entityProtections.entrySet().stream()
                .map(entry -> new ProtectionSyncPayload.EntityEntry(entry.getKey(), entry.getValue().ordinal()))
                .toList();
    }

    private void loadBlocks(Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }

        Map<BlockPos, Integer> occurrences = new HashMap<>();
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            int recordCount = input.readInt();
            if (recordCount < 0) {
                throw new IOException("Negative protected-block record count: " + recordCount);
            }
            for (int i = 0; i < recordCount; i++) {
                BlockPos position = new BlockPos(input.readInt(), input.readInt(), input.readInt());
                occurrences.merge(position, 1, Integer::sum);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not load protected blocks from {}", file, exception);
            return;
        }
        occurrences.forEach((position, copies) -> blockProtections.put(position, ProtectionState.fromStorageCopies(copies)));
    }

    private void saveBlocks(Path file) {
        int recordCount = blockProtections.values().stream().mapToInt(ProtectionState::storageCopies).sum();
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            output.writeInt(recordCount);
            for (Map.Entry<BlockPos, ProtectionState> entry : blockProtections.entrySet()) {
                for (int i = 0; i < entry.getValue().storageCopies(); i++) {
                    writePosition(output, entry.getKey());
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Could not save protected blocks to {}", file, exception);
        }
    }

    private void loadEntities(Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            int count = input.readInt();
            if (count < 0 || count > 1_000_000) {
                throw new IOException("Invalid protected-entity count: " + count);
            }
            for (int i = 0; i < count; i++) {
                UUID entityId = new UUID(input.readLong(), input.readLong());
                ProtectionState state = ProtectionState.fromId(input.readUnsignedByte()).forEntity();
                if (state.isProtected()) {
                    entityProtections.put(entityId, state);
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Could not load protected entities from {}", file, exception);
        }
    }

    private void saveEntities(Path file) {
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            output.writeInt(entityProtections.size());
            for (Map.Entry<UUID, ProtectionState> entry : entityProtections.entrySet()) {
                output.writeLong(entry.getKey().getMostSignificantBits());
                output.writeLong(entry.getKey().getLeastSignificantBits());
                output.writeByte(entry.getValue().ordinal());
            }
        } catch (IOException exception) {
            LOGGER.error("Could not save protected entities to {}", file, exception);
        }
    }

    private static void writePosition(DataOutputStream output, BlockPos position) throws IOException {
        output.writeInt(position.getX());
        output.writeInt(position.getY());
        output.writeInt(position.getZ());
    }

    private static Path getFile(ServerLevel level, String fileName) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve("data").resolve(fileName);
    }
}
