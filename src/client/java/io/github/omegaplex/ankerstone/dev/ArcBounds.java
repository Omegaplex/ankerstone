package io.github.omegaplex.ankerstone.dev;

public record ArcBounds(ArcPoint minimum, ArcPoint maximum) {
    public ArcBounds {
        if (!finite(minimum) || !finite(maximum)
                || minimum.x() > maximum.x()
                || minimum.y() > maximum.y()
                || minimum.z() > maximum.z()) {
            throw new IllegalArgumentException("Invalid arc bounds");
        }
    }

    public boolean contains(ArcPoint point) {
        return finite(point)
                && point.x() >= minimum.x() && point.x() <= maximum.x()
                && point.y() >= minimum.y() && point.y() <= maximum.y()
                && point.z() >= minimum.z() && point.z() <= maximum.z();
    }

    public ArcPoint constrain(ArcPoint point) {
        return new ArcPoint(
                Math.clamp(point.x(), minimum.x(), maximum.x()),
                Math.clamp(point.y(), minimum.y(), maximum.y()),
                Math.clamp(point.z(), minimum.z(), maximum.z()));
    }

    private static boolean finite(ArcPoint point) {
        return point != null
                && Float.isFinite(point.x())
                && Float.isFinite(point.y())
                && Float.isFinite(point.z());
    }
}
