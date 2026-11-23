package io.github.omegaplex.ankerstone.dev;

public record ArcIntensity(
        int selectionMillis, int refreshMillis, int quietMillis) {

    public static ArcIntensity at(
            double intensity,
            int baseSelection, int baseRefresh, int baseQuiet) {
        if (!Double.isFinite(intensity)
                || intensity < 0 || intensity > 1
                || baseSelection <= 0
                || baseRefresh <= 0
                || baseQuiet < 0) {
            throw new IllegalArgumentException("Invalid arc intensity");
        }

        return new ArcIntensity(
                Math.max(16, (int) Math.round(
                        baseSelection * (1.0 - 0.65 * intensity))),
                Math.max(1, (int) Math.round(
                        baseRefresh * (1.0 - 0.50 * intensity))),
                Math.max(0, (int) Math.round(
                        baseQuiet * (1.0 - 0.90 * intensity))));
    }
}
