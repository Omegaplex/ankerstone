package io.github.omegaplex.ankerstone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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

    private record OwnerEntry(BlockPos pos, AnkerstoneMode mode) {
    }

    private static final Codec<OwnerEntry> OWNER_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(OwnerEntry::pos),
            AnkerstoneMode.CODEC.fieldOf("mode").forGetter(OwnerEntry::mode))
            .apply(instance, OwnerEntry::new));

    // Stores position/mode pairs as a list; decoding rebuilds the owner map.
    public static final Codec<AnkerstoneSavedData> CODEC = OWNER_ENTRY_CODEC
            .listOf()
            .xmap(AnkerstoneSavedData::new, AnkerstoneSavedData::ownerEntries);

    // Describes how Minecraft identifies, creates, and serializes this saved data.
    public static final SavedDataType<AnkerstoneSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Ankerstone.MOD_ID, "chunk_tickets"),
            AnkerstoneSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_FORCED_CHUNKS);

    // Maps each active Ankerstone owner block position to the mode it should
    // restore.
    private final Map<BlockPos, AnkerstoneMode> ownerModes = new HashMap<>();

    private AnkerstoneSavedData(List<OwnerEntry> ownerEntries) {
        for (OwnerEntry entry : ownerEntries) {
            ownerModes.put(entry.pos(), entry.mode());
        }
    }

    private List<OwnerEntry> ownerEntries() {
        return ownerModes.entrySet().stream()
                .map(entry -> new OwnerEntry(entry.getKey(), entry.getValue()))
                .toList();
    }

    public void addOwner(BlockPos ownerPos, AnkerstoneMode mode) {
        AnkerstoneMode oldMode = ownerModes.put(ownerPos, mode);

        if (oldMode != mode) {
            setDirty();
        }
    }

    public void removeOwner(BlockPos ownerPos) {
        AnkerstoneMode removedMode = ownerModes.remove(ownerPos);

        if (removedMode != null) {
            setDirty();
        }
    }

    public Map<BlockPos, AnkerstoneMode> getOwnerModes() {
        return Map.copyOf(ownerModes);
    }

    // Gets this dimension's saved Ankerstone ownership data, creating it if needed.
    public static AnkerstoneSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
