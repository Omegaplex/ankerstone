package io.github.omegaplex.ankerstone;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public class AnkerstoneChunkTickets {
    private static final Map<ChunkPos, Set<BlockPos>> OWNERS = new HashMap<>();

    public static void add(ServerLevel level, BlockPos ownerPos) {
        ChunkPos chunkPos = new ChunkPos(
                Math.floorDiv(ownerPos.getX(), 16),
                Math.floorDiv(ownerPos.getZ(), 16));

        Set<BlockPos> owners = OWNERS.computeIfAbsent(chunkPos, ignored -> new HashSet<>());
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
    
    Set<BlockPos> owners = OWNERS.get(chunkPos);

        if (owners == null) {
            return;
        }

        owners.remove(ownerPos);

        if (owners.isEmpty()) {
            OWNERS.remove(chunkPos);
            level.setChunkForced(chunkPos.x(), chunkPos.z(), false);
        }
    }
}
