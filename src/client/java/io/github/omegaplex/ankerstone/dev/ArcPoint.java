package io.github.omegaplex.ankerstone.dev;

public record ArcPoint(float x, float y, float z) {
    public static ArcPoint fromModel(float x, float y, float z) {
        return new ArcPoint(x / 16.0F, y / 16.0F, z / 16.0F);
    }
}
