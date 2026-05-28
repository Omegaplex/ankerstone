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

public class AnkerstoneSavedData extends SavedData {
    public AnkerstoneSavedData() {
    }

    public static final Codec<AnkerstoneSavedData> CODEC = BlockPos.CODEC
            .listOf()
            .xmap(AnkerstoneSavedData::new, data -> List.copyOf(data.ownerPositions));

    public static final SavedDataType<AnkerstoneSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Ankerstone.MOD_ID, "chunk_tickets"),
            AnkerstoneSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_FORCED_CHUNKS);

    private final Set<BlockPos> ownerPositions = new HashSet<>();

    private AnkerstoneSavedData(List<BlockPos> ownerPositions) {
        this.ownerPositions.addAll(ownerPositions);
    }

    public void addOwner(BlockPos ownerPos) {
        if (ownerPositions.add(ownerPos)) {
            setDirty();
        }
    }

    public void removeOwner(BlockPos ownerPos) {
        if (ownerPositions.remove(ownerPos)) {
            setDirty();
        }
    }

    public Set<BlockPos> getOwnerPositions() {
        return Set.copyOf(ownerPositions);
    }

    public static AnkerstoneSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
