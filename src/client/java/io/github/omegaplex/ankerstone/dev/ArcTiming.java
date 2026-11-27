package io.github.omegaplex.ankerstone.dev;

public record ArcTiming(long selectionStep, long geometryStep) {
    public static ArcTiming at(
            long elapsedMillis,
            int selectionMillis, int refreshMillis) {
        if (elapsedMillis < 0
                || selectionMillis <= 0
                || refreshMillis <= 0) {
            throw new IllegalArgumentException("Invalid arc timing");
        }

        return new ArcTiming(
                elapsedMillis / selectionMillis,
                elapsedMillis / refreshMillis);
    }
}
