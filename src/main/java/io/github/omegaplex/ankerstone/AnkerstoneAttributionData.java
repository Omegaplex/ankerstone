package io.github.omegaplex.ankerstone;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class AnkerstoneAttributionData extends SavedData {
    private record PlacementEntry(GlobalPos pos, UUID placer) {
    }

    private static final Codec<PlacementEntry> PLACEMENT_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    GlobalPos.CODEC.fieldOf("pos").forGetter(PlacementEntry::pos),
                    UUIDUtil.CODEC.fieldOf("placer").forGetter(PlacementEntry::placer))
            .apply(instance, PlacementEntry::new));

    public static final Codec<AnkerstoneAttributionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PLACEMENT_ENTRY_CODEC.listOf()
                    .optionalFieldOf("placements", List.of())
                    .forGetter(AnkerstoneAttributionData::placementEntries),
            UUIDUtil.CODEC_SET.optionalFieldOf("pending_credits", Set.of())
                    .forGetter(data -> data.pendingCredits))
            .apply(instance, AnkerstoneAttributionData::new));

    public static final SavedDataType<AnkerstoneAttributionData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Ankerstone.MOD_ID, "attribution"),
            AnkerstoneAttributionData::new,
            CODEC,
            null);

    private final Set<UUID> pendingCredits = new HashSet<>();

    private final Map<GlobalPos, UUID> placers = new HashMap<>();

    public AnkerstoneAttributionData() {
    }

    private AnkerstoneAttributionData(
            List<PlacementEntry> entries, Set<UUID> pendingCredits) {
        for (PlacementEntry entry : entries) {
            placers.put(entry.pos(), entry.placer());
        }
        this.pendingCredits.addAll(pendingCredits);
    }

    private List<PlacementEntry> placementEntries() {
        return placers.entrySet().stream()
                .map(entry -> new PlacementEntry(entry.getKey(), entry.getValue()))
                .toList();
    }

    public void setPlacer(GlobalPos position, UUID placer) {
        GlobalPos key = GlobalPos.of(
                position.dimension(), position.pos().immutable());
        UUID previous = placers.put(key, placer);

        if (!placer.equals(previous)) {
            setDirty();
        }
    }

    public UUID getPlacer(GlobalPos position) {
        return placers.get(position);
    }

    public void removePlacer(GlobalPos position) {
        if (placers.remove(position) != null) {
            setDirty();
        }
    }

    public void queueCredit(UUID player) {
        if (pendingCredits.add(player)) {
            setDirty();
        }
    }

    public boolean hasPendingCredit(UUID player) {
        return pendingCredits.contains(player);
    }

    public void removePendingCredit(UUID player) {
        if (pendingCredits.remove(player)) {
            setDirty();
        }
    }

    // All dimensions share the Overworld's attribution store.
    public static AnkerstoneAttributionData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
