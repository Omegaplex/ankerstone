package io.github.omegaplex.ankerstone.dev;

final class ArcClock {
    private long lastMillis;
    private long nextMillis;
    private long step;
    private int intervalMillis;

    ArcClock(int intervalMillis) {
        if (intervalMillis <= 0) {
            throw new IllegalArgumentException("Invalid clock interval");
        }
        this.intervalMillis = intervalMillis;
        nextMillis = intervalMillis;
    }

    long advanceTo(long elapsedMillis) {
        if (elapsedMillis < lastMillis) {
            throw new IllegalArgumentException("Clock cannot move backward");
        }
        if (elapsedMillis >= nextMillis) {
            long elapsedSteps = 1 + (elapsedMillis - nextMillis) / intervalMillis;
            step += elapsedSteps;
            nextMillis += elapsedSteps * intervalMillis;
        }
        lastMillis = elapsedMillis;
        return step;
    }

    void setIntervalAt(long elapsedMillis, int newIntervalMillis) {
        if (newIntervalMillis <= 0) {
            throw new IllegalArgumentException("Invalid clock interval");
        }
        advanceTo(elapsedMillis);
        intervalMillis = newIntervalMillis;
    }
}
