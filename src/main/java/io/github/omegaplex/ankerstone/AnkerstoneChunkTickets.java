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
    // Map of dimension to chunk positions to owner block positions. Read from inner
    // to outer: for each dimension, which chunks have Ankerstone owners, and for
    // each chunk, which block positions are the owners.
    // Static because we only want one instance of this map for the entire server,
    // and we access it from static methods. We don't want to store this in the
    // saved data because it's only used for quick access while the server is
    // running, and we can reconstruct it from the saved data when a server
    // dimension loads.
    private static final Map<ResourceKey<Level>, Map<ChunkPos, Set<BlockPos>>> OWNERS = new HashMap<>();

    // Get the map of chunk positions to owner block positions for a given level,
    // create it if it doesn't exist.
    private static Map<ChunkPos, Set<BlockPos>> ownersFor(ServerLevel level) {
        return OWNERS.computeIfAbsent(level.dimension(), ignored -> new HashMap<>());
    }

    public static void add(ServerLevel level, BlockPos ownerPos) {
        addToMemory(level, ownerPos);
        AnkerstoneSavedData.get(level).addOwner(ownerPos);
    }

    // Rebuilds runtime ownership and chunk tickets without changing saved data.
    private static void addToMemory(ServerLevel level, BlockPos ownerPos) {
        ChunkPos chunkPos = new ChunkPos(
                Math.floorDiv(ownerPos.getX(), 16),
                Math.floorDiv(ownerPos.getZ(), 16));

        Map<ChunkPos, Set<BlockPos>> dimensionOwners = ownersFor(level);
        // Get the set of owner block positions for this chunk, create it if it doesn't
        // exist. Set prevents duplicates, so if the same block is added multiple times,
        // it won't cause issues.
        Set<BlockPos> owners = dimensionOwners.computeIfAbsent(chunkPos, ignored -> new HashSet<>());
        boolean wasEmpty = owners.isEmpty();
        // Add the owner block position to the set of owners for this chunk.
        owners.add(ownerPos);

        if (wasEmpty) {
            // If the set of owners was empty before adding this block, it means this chunk
            // wasn't forced to stay loaded before, so we need to force it now.
            level.setChunkForced(chunkPos.x(), chunkPos.z(), true);
        }
    }

    public static void remove(ServerLevel level, BlockPos ownerPos) {
        // Use floorDiv so negative block coordinates map to the correct chunk.
        ChunkPos chunkPos = new ChunkPos(
                Math.floorDiv(ownerPos.getX(), 16),
                Math.floorDiv(ownerPos.getZ(), 16));

        // Get this dimension's owner map.
        // If this chunk has no owners recorded, there is nothing to remove.
        Map<ChunkPos, Set<BlockPos>> dimensionOwners = ownersFor(level);
        Set<BlockPos> owners = dimensionOwners.get(chunkPos);

        if (owners == null) {
            return;
        }

        owners.remove(ownerPos);
        AnkerstoneSavedData.get(level).removeOwner(ownerPos);

        // If the set of owners is now empty after removing this block, it means there
        // are no more Ankerstone blocks in this chunk, so we can unforce it and remove
        // the entry from the map to save memory.
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
