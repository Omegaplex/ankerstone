package io.github.omegaplex.ankerstone;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

// Persistent saved data for active Ankerstone owner block positions.
public class AnkerstoneSavedData extends SavedData {
    public AnkerstoneSavedData() {
    }

    // Saves owner positions as a list of block positions. We can reconstruct the
    // map of chunk positions to owner block positions from this list when the
    // server starts. We use a list instead of a set because Codec doesn't have a
    // built-in way to serialize sets, but it can serialize lists, and we can easily
    // convert between them.
    public static final Codec<AnkerstoneSavedData> CODEC = BlockPos.CODEC
            .listOf()
            .xmap(AnkerstoneSavedData::new, data -> List.copyOf(data.ownerPositions));

    // Describes how Minecraft identifies, creates, and serializes this saved data.
    public static final SavedDataType<AnkerstoneSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Ankerstone.MOD_ID, "chunk_tickets"),
            AnkerstoneSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_FORCED_CHUNKS);

    // Declaring the set of active Ankerstone owner block positions. This is used to
    // determine which chunks should be forced to stay loaded. A chunk is forced to
    // stay loaded if it has at least one Ankerstone owner block position in it.
    private final Set<BlockPos> ownerPositions = new HashSet<>();

    private AnkerstoneSavedData(List<BlockPos> ownerPositions) {
        this.ownerPositions.addAll(ownerPositions);
    }

    public void addOwner(BlockPos ownerPos) {
        // We only setDirty if the owner position was actually added, to avoid
        // unnecessary saves.
        if (ownerPositions.add(ownerPos)) {
            setDirty();
        }
    }

    public void removeOwner(BlockPos ownerPos) {
        // We only setDirty if the owner position was actually removed, to avoid
        // unnecessary saves.
        if (ownerPositions.remove(ownerPos)) {
            setDirty();
        }
    }

    public Set<BlockPos> getOwnerPositions() {
        return Set.copyOf(ownerPositions);
    }

    // Gets this dimension's saved Ankerstone ownership data, creating it if needed.
    public static AnkerstoneSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
