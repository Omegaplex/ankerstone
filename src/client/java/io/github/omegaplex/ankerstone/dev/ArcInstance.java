package io.github.omegaplex.ankerstone.dev;

import java.util.SplittableRandom;

final class ArcInstance {
    final long seed;
    final long startedNanos;
    private final ArcClock selectionClock;
    private final ArcClock geometryClock;
    private long activityWindow;
    private long activityStartMillis;
    private long activityLastMillis;
    private int activityBurstMillis;
    private int activityQuietMillis;
    private int pendingBurstMillis;
    private int pendingQuietMillis;

    private final ArcSlot[] slots = new ArcSlot[32];

    ArcInstance(long seed, long startedNanos) {
        this(seed, startedNanos,
                ArcIntensity.at(
                        ArcOption.INTENSITY.value(),
                        (int) ArcOption.SELECTION_MS.value(),
                        (int) ArcOption.REFRESH_MS.value(),
                        (int) ArcOption.QUIET_MS.value()),
                (int) ArcOption.BURST_MS.value());
    }

    ArcInstance(long seed, long startedNanos,
            ArcIntensity initial, int burstMillis) {
        if (burstMillis <= 0) {
            throw new IllegalArgumentException("Invalid burst interval");
        }

        this.seed = seed;
        this.startedNanos = startedNanos;
        selectionClock = new ArcClock(initial.selectionMillis());
        geometryClock = new ArcClock(initial.refreshMillis());
        activityBurstMillis = burstMillis;
        activityQuietMillis = initial.quietMillis();
        pendingBurstMillis = activityBurstMillis;
        pendingQuietMillis = activityQuietMillis;
    }

    ArcTiming timingAt(long nowNanos) {
        long elapsedMillis = elapsedMillis(nowNanos);
        return new ArcTiming(
                selectionClock.advanceTo(elapsedMillis),
                geometryClock.advanceTo(elapsedMillis));
    }

    void changeTimingAt(long nowNanos, ArcIntensity intensity,
            int burstMillis) {
        if (burstMillis <= 0) {
            throw new IllegalArgumentException("Invalid burst interval");
        }

        long elapsed = elapsedMillis(nowNanos);
        selectionClock.setIntervalAt(elapsed, intensity.selectionMillis());
        geometryClock.setIntervalAt(elapsed, intensity.refreshMillis());
        advanceActivityTo(elapsed);
        pendingBurstMillis = burstMillis;
        pendingQuietMillis = intensity.quietMillis();
    }

    private long elapsedMillis(long nowNanos) {
        return (nowNanos - startedNanos) / 1_000_000L;
    }

    ArcSlot slot(int index) {
        if (index < 0 || index >= slots.length) {
            throw new IllegalArgumentException("Invalid arc slot index");
        }
        if (slots[index] == null) {
            long slotSeed = new SplittableRandom(
                    seed + index * 0x9E3779B97F4A7C15L).nextLong();
            slots[index] = new ArcSlot(slotSeed);
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

    boolean activeAt(long nowNanos, long slotSeed) {
        long elapsed = elapsedMillis(nowNanos);
        advanceActivityTo(elapsed);
        return ArcActivity.activeInWindow(
                activityWindow, elapsed - activityStartMillis,
                activityBurstMillis, activityQuietMillis, slotSeed);
    }

    private void advanceActivityTo(long elapsedMillis) {
        if (elapsedMillis < activityLastMillis) {
            throw new IllegalArgumentException("Activity cannot move backward");
        }

        long end = activityStartMillis
                + (long) activityBurstMillis + activityQuietMillis;
        if (elapsedMillis >= end) {
            activityStartMillis = end;
            activityWindow++;
            activityBurstMillis = pendingBurstMillis;
            activityQuietMillis = pendingQuietMillis;

            long windowMillis = (long) activityBurstMillis + activityQuietMillis;
            long skipped = (elapsedMillis - activityStartMillis) / windowMillis;
            activityStartMillis += skipped * windowMillis;
            activityWindow += skipped;
        }
        activityLastMillis = elapsedMillis;
    }
}
