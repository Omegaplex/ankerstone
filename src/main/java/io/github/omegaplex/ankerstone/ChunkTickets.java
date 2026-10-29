package io.github.omegaplex.ankerstone;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class ChunkTickets {
    // Runtime owners grouped by dimension and center chunk.
    // Owners with the same mode in a chunk share a ticket.
    // Rebuilt from saved owners when the dimension loads.
    private static final Map<ResourceKey<Level>, Map<ChunkPos, Map<BlockPos, AnkerstoneMode>>> OWNERS = new HashMap<>();

    private record PendingOwner(
            ResourceKey<Level> dimension,
            BlockPos pos,
            AnkerstoneMode savedMode) {
    }

    private static final java.util.ArrayDeque<PendingOwner> PENDING = new java.util.ArrayDeque<>();

    public static void validatePending(MinecraftServer server) {
        int attempts = Math.min(16, PENDING.size());

        for (int i = 0; i < attempts; i++) {
            PendingOwner pending = PENDING.removeFirst();
            ServerLevel level = server.getLevel(pending.dimension());
            if (level == null)
                continue;

            OwnerData savedData = OwnerData.get(level);
            if (savedData.getOwnerMode(pending.pos()) != pending.savedMode()) {
                continue;
            }

            ChunkPos chunkPos = chunkFor(pending.pos());
            var chunk = level.getChunkSource()
                    .getChunkNow(chunkPos.x(), chunkPos.z());

            if (chunk == null) {
                PENDING.addLast(pending);
                continue;
            }

            var state = chunk.getBlockState(pending.pos());

            if (!state.is(ModBlocks.ANKERSTONE)) {
                remove(level, pending.pos(), pending.savedMode());
                continue;
            }
            if (!powerChunksReady(level, pending.pos())) {
                PENDING.addLast(pending);
                continue;
            }
            AnkerstoneBlock.reconcileRestoredOwner(
                    level, pending.pos(), state, pending.savedMode());
        }
    }

    private static boolean powerChunksReady(ServerLevel level, BlockPos pos) {
        int minX = Math.floorDiv(pos.getX() - 2, 16);
        int maxX = Math.floorDiv(pos.getX() + 2, 16);
        int minZ = Math.floorDiv(pos.getZ() - 2, 16);
        int maxZ = Math.floorDiv(pos.getZ() + 2, 16);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) {
                    return false;
                }
            }
        }

        return true;
    }

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

        PENDING.removeIf(owner -> owner.dimension().equals(level.dimension()));

        for (Map.Entry<BlockPos, AnkerstoneMode> entry : savedData.getOwnerModes().entrySet()) {
            addToMemory(level, entry.getKey(), entry.getValue());
            PENDING.addLast(new PendingOwner(
                    level.dimension(),
                    entry.getKey().immutable(),
                    entry.getValue()));
        }
    }

    public static void clear(ServerLevel level) {
        OWNERS.remove(level.dimension());
        PENDING.removeIf(owner -> owner.dimension().equals(level.dimension()));
    }
}
