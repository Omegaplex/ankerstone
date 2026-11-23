package io.github.omegaplex.ankerstone.dev;

final class ArcInstance {
    final long seed;
    final long startedNanos;

    private final ArcSlot[] slots = new ArcSlot[32];

    ArcInstance(long seed, long startedNanos) {
        this.seed = seed;
        this.startedNanos = startedNanos;
    }

    ArcSlot slot(int index) {
        if (index < 0 || index >= slots.length) {
            throw new IllegalArgumentException("Invalid arc slot index");
        }
        if (slots[index] == null) {
            slots[index] = new ArcSlot(seed + index * 0x9E3779B97F4A7C15L);
        }
        return slots[index];
    }

    static int slotCount(double intensity, int maximum) {
        if (!Double.isFinite(intensity) || intensity < 0 || intensity > 1
                || maximum < 0 || maximum > 32) {
            throw new IllegalArgumentException("Invalid arc slot count");
        }
        return maximum == 0 ? 0 : 1 + (int) Math.round(intensity * (maximum - 1));
    }
}
