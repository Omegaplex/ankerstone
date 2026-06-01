package io.github.omegaplex.ankerstone;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum AnkerstoneMode implements StringRepresentable {
    OFF("off"),
    SINGLE("single"),
    WIDE("wide");

    public static final Codec<AnkerstoneMode> CODEC = StringRepresentable.fromEnum(AnkerstoneMode::values);

    private final String name;

    AnkerstoneMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
