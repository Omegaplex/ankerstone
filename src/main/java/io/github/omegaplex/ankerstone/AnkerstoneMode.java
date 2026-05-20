package io.github.omegaplex.ankerstone;

import net.minecraft.util.StringRepresentable;

public enum AnkerstoneMode implements StringRepresentable {
    OFF("off"),
    SINGLE("single"),
    WIDE("wide");

    private final String name;

    AnkerstoneMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
