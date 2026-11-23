package io.github.omegaplex.ankerstone.dev;

public record ArcTiming(
        long selectionStep, long geometryStep, boolean active) {

    public static ArcTiming at(
            long elapsedMillis,
            int selectionMillis, int refreshMillis,
            int burstMillis, int quietMillis) {
        if (elapsedMillis < 0
                || selectionMillis <= 0
                || refreshMillis <= 0
                || burstMillis <= 0
                || quietMillis < 0) {
            throw new IllegalArgumentException("Invalid arc timing");
        }

        long cycleMillis = (long) burstMillis + quietMillis;
        boolean active = elapsedMillis % cycleMillis < burstMillis;

        return new ArcTiming(
                elapsedMillis / selectionMillis,
                elapsedMillis / refreshMillis,
                active);
    }
}
