package io.github.omegaplex.ankerstone;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.saveddata.SavedData;

public class LimitData extends SavedData {
    public static final int UNLIMITED = -1;

    private int activeLimit = UNLIMITED;

    public static final Codec<LimitData> CODEC = Codec.intRange(UNLIMITED, Integer.MAX_VALUE)
            .fieldOf("active_limit")
            .xmap(LimitData::new, LimitData::getActiveLimit)
            .codec();

    public int getActiveLimit() {
        return activeLimit;
    }

    public static final SavedDataType<LimitData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Ankerstone.MOD_ID, "limits"),
            LimitData::new,
            CODEC,
            null);

    public LimitData() {
    }

    private LimitData(int activeLimit) {
        this.activeLimit = activeLimit;
    }

    public boolean isUnlimited() {
        return activeLimit == UNLIMITED;
    }

    public void setActiveLimit(int limit) {
        if (limit < UNLIMITED) {
            throw new IllegalArgumentException(
                    "Active limit must be unlimited (-1), zero, or positive");
        }

        if (activeLimit != limit) {
            activeLimit = limit;
            setDirty();
        }
    }

    public static LimitData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
