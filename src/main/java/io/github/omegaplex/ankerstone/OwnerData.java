package io.github.omegaplex.ankerstone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class OwnerData extends SavedData {
    public OwnerData() {
    }

    private record OwnerEntry(BlockPos pos, AnkerstoneMode mode) {
    }

    private static final Codec<OwnerEntry> OWNER_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(OwnerEntry::pos),
            AnkerstoneMode.CODEC.fieldOf("mode").forGetter(OwnerEntry::mode))
            .apply(instance, OwnerEntry::new));

    // Stores position/mode pairs as a list; decoding rebuilds the owner map.
    public static final Codec<OwnerData> CODEC = OWNER_ENTRY_CODEC
            .listOf()
            .xmap(OwnerData::new, OwnerData::ownerEntries);

    // Vanilla's forced-chunk data fixes don't apply to our owner format.
    public static final SavedDataType<OwnerData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Ankerstone.MOD_ID, "chunk_tickets"),
            OwnerData::new,
            CODEC,
            null);

    private final Map<BlockPos, AnkerstoneMode> ownerModes = new HashMap<>();

    private OwnerData(List<OwnerEntry> ownerEntries) {
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
        AnkerstoneMode oldMode = ownerModes.put(ownerPos.immutable(), mode);

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

    public AnkerstoneMode getOwnerMode(BlockPos pos) {
        return ownerModes.get(pos);
    }

    public static OwnerData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
