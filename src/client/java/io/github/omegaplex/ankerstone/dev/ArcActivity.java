package io.github.omegaplex.ankerstone.dev;

public final class ArcActivity {
    private ArcActivity() {
    }

    public static boolean activeAt(
            long elapsedMillis, int burstMillis,
            int quietMillis, long seed) {
        if (elapsedMillis < 0 || burstMillis <= 0 || quietMillis < 0) {
            throw new IllegalArgumentException("Invalid activity timing");
        }
        if (quietMillis == 0) {
            return true;
        }

        long windowMillis = (long) burstMillis + quietMillis;
        long window = elapsedMillis / windowMillis;
        long position = elapsedMillis % windowMillis;

        long minimumBurst = Math.max(1, burstMillis / 2L);
        long maximumBurst = Math.min(
                windowMillis, burstMillis + burstMillis / 2L);

        long random = mix(seed + window * 0x9E3779B97F4A7C15L);
        long duration = minimumBurst
                + Math.floorMod(random, maximumBurst - minimumBurst + 1);
        long offset = Math.floorMod(
                mix(random), windowMillis - duration + 1);

        return position >= offset && position - offset < duration;
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
