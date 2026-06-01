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

    private static Set<ChunkPos> chunksFor(BlockPos ownerPos, AnkerstoneMode mode) {
        ChunkPos center = new ChunkPos(
                Math.floorDiv(ownerPos.getX(), 16),
                Math.floorDiv(ownerPos.getZ(), 16));

        int radius = mode == AnkerstoneMode.WIDE ? 1 : 0;
        Set<ChunkPos> chunks = new HashSet<>();

        for (int chunkX = center.x() - radius; chunkX <= center.x() + radius; chunkX++) {
            for (int chunkZ = center.z() - radius; chunkZ <= center.z() + radius; chunkZ++) {
                chunks.add(new ChunkPos(chunkX, chunkZ));
            }
        }

        return chunks;
    }

    public static void add(ServerLevel level, BlockPos ownerPos, AnkerstoneMode mode) {
        addToMemory(level, ownerPos, mode);
        AnkerstoneSavedData.get(level).addOwner(ownerPos, mode);
    }

    // Rebuilds runtime ownership and chunk tickets without changing saved data.
    private static void addToMemory(ServerLevel level, BlockPos ownerPos, AnkerstoneMode mode) {
        Map<ChunkPos, Set<BlockPos>> dimensionOwners = ownersFor(level);

        for (ChunkPos chunkPos : chunksFor(ownerPos, mode)) {
            Set<BlockPos> owners = dimensionOwners.computeIfAbsent(chunkPos, ignored -> new HashSet<>());
            boolean wasEmpty = owners.isEmpty();

            owners.add(ownerPos);

            if (wasEmpty) {
                level.setChunkForced(chunkPos.x(), chunkPos.z(), true);
            }
        }
    }

    public static void remove(ServerLevel level, BlockPos ownerPos, AnkerstoneMode mode) {
        Map<ChunkPos, Set<BlockPos>> dimensionOwners = ownersFor(level);

        for (ChunkPos chunkPos : chunksFor(ownerPos, mode)) {
            Set<BlockPos> owners = dimensionOwners.get(chunkPos);

            if (owners == null) {
                continue;
            }

            owners.remove(ownerPos);

            if (owners.isEmpty()) {
                dimensionOwners.remove(chunkPos);
                level.setChunkForced(chunkPos.x(), chunkPos.z(), false);
            }
        }

        AnkerstoneSavedData.get(level).removeOwner(ownerPos);
    }

    public static void restore(ServerLevel level) {
        AnkerstoneSavedData savedData = AnkerstoneSavedData.get(level);

        for (Map.Entry<BlockPos, AnkerstoneMode> entry : savedData.getOwnerModes().entrySet()) {
            addToMemory(level, entry.getKey(), entry.getValue());
        }
    }

    public static void clear(ServerLevel level) {
        OWNERS.remove(level.dimension());
    }
}
