package io.github.omegaplex.ankerstone;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class AnkerstoneChunkTickets {
    private static final Map<ResourceKey<Level>, Map<ChunkPos, Set<BlockPos>>> OWNERS = new HashMap<>();

    private static Map<ChunkPos, Set<BlockPos>> ownersFor(ServerLevel level) {
        return OWNERS.computeIfAbsent(level.dimension(), ignored -> new HashMap<>());
    }

    public static void add(ServerLevel level, BlockPos ownerPos) {
        addToMemory(level, ownerPos);
        AnkerstoneSavedData.get(level).addOwner(ownerPos);
    }

    private static void addToMemory(ServerLevel level, BlockPos ownerPos) {
        ChunkPos chunkPos = new ChunkPos(
                Math.floorDiv(ownerPos.getX(), 16),
                Math.floorDiv(ownerPos.getZ(), 16));

        Map<ChunkPos, Set<BlockPos>> dimensionOwners = ownersFor(level);
        Set<BlockPos> owners = dimensionOwners.computeIfAbsent(chunkPos, ignored -> new HashSet<>());
        boolean wasEmpty = owners.isEmpty();

        owners.add(ownerPos);

        if (wasEmpty) {
            level.setChunkForced(chunkPos.x(), chunkPos.z(), true);
        }
    }

    public static void remove(ServerLevel level, BlockPos ownerPos) {
        ChunkPos chunkPos = new ChunkPos(
                Math.floorDiv(ownerPos.getX(), 16),
                Math.floorDiv(ownerPos.getZ(), 16));

        Map<ChunkPos, Set<BlockPos>> dimensionOwners = ownersFor(level);
        Set<BlockPos> owners = dimensionOwners.get(chunkPos);

        if (owners == null) {
            return;
        }

        owners.remove(ownerPos);
        AnkerstoneSavedData.get(level).removeOwner(ownerPos);

        if (owners.isEmpty()) {
            dimensionOwners.remove(chunkPos);
            level.setChunkForced(chunkPos.x(), chunkPos.z(), false);
        }
    }

    public static void restore(ServerLevel level) {
        AnkerstoneSavedData savedData = AnkerstoneSavedData.get(level);

        for (BlockPos ownerPos : savedData.getOwnerPositions()) {
            addToMemory(level, ownerPos);
        }
    }

    public static void clear(ServerLevel level) {
        OWNERS.remove(level.dimension());
    }
}
