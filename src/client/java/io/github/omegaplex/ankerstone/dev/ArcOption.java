package io.github.omegaplex.ankerstone.dev;

import java.util.Locale;

/**
 * Bounded, session-only tuning; these values never enter the player's saved
 * settings.
 */
public enum ArcOption {
    MAX_LINES(4, 0, 32, true),
    SELECTION_MS(300, 16, 5000, true),
    REFRESH_MS(40, 1, 5000, true),
    SEGMENTS(4, 1, 16, true),
    JITTER(0.015, 0, 0.25, false),
    LIFT(0.02, 0, 0.125, false),
    // Maximum base route length in blocks, before jitter and outward lift.
    MAX_LENGTH(1.5, 0.01, 2, false),
    CONNECTION_CHANCE(0.125, 0, 1, false),
    ROUTED_CHANCE(0.5, 0, 1, false),
    WIDTH(1, 0.25, 4, false),
    BRIGHTNESS(1, 0, 1, false),
    RED(1, 0, 1, false),
    GREEN(1, 0, 1, false),
    BLUE(1, 0, 1, false),
    BURST_MS(300, 1, 10000, true),
    QUIET_MS(500, 0, 10000, true),
    REGION(2, 0, 2, true),
    INTENSITY(0.5, 0, 1, false),
    ;

    private final double defaultValue;
    private final double minimum;
    private final double maximum;
    private final boolean wholeNumber;
    private double value;

    ArcOption(double defaultValue, double minimum, double maximum, boolean wholeNumber) {
        this.defaultValue = defaultValue;
        this.minimum = minimum;
        this.maximum = maximum;
        this.wholeNumber = wholeNumber;
        value = defaultValue;
    }

    public String commandName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public double value() {
        return value;
    }

    public double minimum() {
        return minimum;
    }

    public double maximum() {
        return maximum;
    }

    public boolean set(double updated) {
        if (!Double.isFinite(updated) || updated < minimum || updated > maximum
                || (wholeNumber && updated != Math.rint(updated))) {
            return false;
        }
        value = updated;
        return true;
    }

    public void reset() {
        value = defaultValue;
    }
}
