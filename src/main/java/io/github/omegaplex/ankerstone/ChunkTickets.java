package io.github.omegaplex.ankerstone;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class ChunkTickets {
    // Runtime owners grouped by dimension and center chunk.
    // Owners with the same mode in a chunk share a ticket.
    // Rebuilt from saved owners when the dimension loads.
    private static final Map<ResourceKey<Level>, Map<ChunkPos, Map<BlockPos, AnkerstoneMode>>> OWNERS = new HashMap<>();

    private static Map<ChunkPos, Map<BlockPos, AnkerstoneMode>> ownersFor(ServerLevel level) {
        return OWNERS.computeIfAbsent(level.dimension(), ignored -> new HashMap<>());
    }

    static final TicketType ANKERSTONE_TICKET = new TicketType(
            TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING
                    | TicketType.FLAG_SIMULATION
                    | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);

    private static ChunkPos chunkFor(BlockPos ownerPos) {
        return new ChunkPos(
                Math.floorDiv(ownerPos.getX(), 16),
                Math.floorDiv(ownerPos.getZ(), 16));
    }

    private static int radiusFor(AnkerstoneMode mode) {
        return mode == AnkerstoneMode.WIDE ? 3 : 2;
    }

    public static void add(ServerLevel level, BlockPos ownerPos, AnkerstoneMode mode) {
        addToMemory(level, ownerPos, mode);
        OwnerData.get(level).addOwner(ownerPos, mode);
    }

    // Rebuilds runtime ownership and chunk tickets without changing saved data.
    private static void addToMemory(ServerLevel level, BlockPos ownerPos, AnkerstoneMode mode) {
        Map<ChunkPos, Map<BlockPos, AnkerstoneMode>> dimensionOwners = ownersFor(level);
        ChunkPos chunkPos = chunkFor(ownerPos);
        Map<BlockPos, AnkerstoneMode> owners = dimensionOwners.computeIfAbsent(chunkPos, ignored -> new HashMap<>());
        boolean alreadyHasThisMode = owners.containsValue(mode);

        owners.put(ownerPos.immutable(), mode);

        if (!alreadyHasThisMode) {
            level.getChunkSource().addTicketWithRadius(ANKERSTONE_TICKET, chunkPos, radiusFor(mode));
        }
    }

    public static void remove(ServerLevel level, BlockPos ownerPos, AnkerstoneMode mode) {
        Map<ChunkPos, Map<BlockPos, AnkerstoneMode>> dimensionOwners = ownersFor(level);
        ChunkPos chunkPos = chunkFor(ownerPos);
        Map<BlockPos, AnkerstoneMode> owners = dimensionOwners.get(chunkPos);

        if (owners == null) {
            OwnerData.get(level).removeOwner(ownerPos);
            return;
        }

        owners.remove(ownerPos);
        boolean stillHasThisMode = owners.containsValue(mode);

        if (!stillHasThisMode) {
            level.getChunkSource().removeTicketWithRadius(ANKERSTONE_TICKET, chunkPos, radiusFor(mode));
        }

        if (owners.isEmpty()) {
            dimensionOwners.remove(chunkPos);
        }

        OwnerData.get(level).removeOwner(ownerPos);
    }

    public static void restore(ServerLevel level) {
        OwnerData savedData = OwnerData.get(level);

        for (Map.Entry<BlockPos, AnkerstoneMode> entry : savedData.getOwnerModes().entrySet()) {
            addToMemory(level, entry.getKey(), entry.getValue());
        }
    }

    public static void clear(ServerLevel level) {
        OWNERS.remove(level.dimension());
    }
}
