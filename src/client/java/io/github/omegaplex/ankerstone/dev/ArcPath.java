package io.github.omegaplex.ankerstone.dev;

import java.util.random.RandomGenerator;

public final class ArcPath {
    private ArcPath() {
    }

    public static ArcPoint[] between(

            ArcPoint start, ArcPoint end, int segments) {
        if (segments < 1 || segments > 16) {
            throw new IllegalArgumentException(
                    "Segment count must be between 1 and 16");
        }

        ArcPoint[] points = new ArcPoint[segments + 1];
        points[0] = start;
        points[segments] = end;

        for (int i = 1; i < segments; i++) {
            float fraction = (float) i / segments;
            points[i] = new ArcPoint(
                    start.x() + (end.x() - start.x()) * fraction,
                    start.y() + (end.y() - start.y()) * fraction,
                    start.z() + (end.z() - start.z()) * fraction);
        }

        return points;
    }

    public static ArcPoint[] jittered(
            ArcPoint start, ArcPoint end, int segments,
            float amplitude, ArcBounds bounds, RandomGenerator random) {
        if (!Float.isFinite(amplitude) || amplitude < 0) {
            throw new IllegalArgumentException(
                    "Jitter amplitude must be finite and nonnegative");
        }
        if (!bounds.contains(start) || !bounds.contains(end)) {
            throw new IllegalArgumentException(
                    "Arc endpoints must be inside the allowed region");
        }

        ArcPoint[] points = between(start, end, segments);

        for (int i = 1; i < segments; i++) {
            ArcPoint point = points[i];
            float dx = (random.nextFloat() * 2.0F - 1.0F) * amplitude;
            float dy = (random.nextFloat() * 2.0F - 1.0F) * amplitude;
            float dz = (random.nextFloat() * 2.0F - 1.0F) * amplitude;

            points[i] = bounds.constrain(new ArcPoint(
                    point.x() + dx,
                    point.y() + dy,
                    point.z() + dz));
        }

        return points;
    }

    public static ArcPoint[] raised(
            ArcPoint start, ArcPoint end, int segments,
            float amplitude, ArcPoint lift,
            ArcBounds bounds, RandomGenerator random) {
        if (lift == null
                || !Float.isFinite(lift.x())
                || !Float.isFinite(lift.y())
                || !Float.isFinite(lift.z())) {
            throw new IllegalArgumentException("Lift must be finite");
        }

        ArcPoint[] points = jittered(
                start, end, segments, amplitude, bounds, random);

        for (int i = 1; i < segments; i++) {
            float fraction = (float) i / segments;
            float envelope = 4.0F * fraction * (1.0F - fraction);
            ArcPoint point = points[i];

            points[i] = bounds.constrain(new ArcPoint(
                    point.x() + lift.x() * envelope,
                    point.y() + lift.y() * envelope,
                    point.z() + lift.z() * envelope));
        }

        return points;
    }

    public static ArcPoint[] via(
            ArcPoint start, ArcPoint bend, ArcPoint end,
            int segments, float amplitude,
            ArcBounds bounds, RandomGenerator random) {
        if (segments < 2 || segments > 16) {
            throw new IllegalArgumentException(
                    "A routed arc requires between 2 and 16 segments");
        }

        int firstSegments = segments / 2;
        int secondSegments = segments - firstSegments;

        ArcPoint[] first = jittered(
                start, bend, firstSegments, amplitude, bounds, random);
        ArcPoint[] second = jittered(
                bend, end, secondSegments, amplitude, bounds, random);

        ArcPoint[] result = new ArcPoint[segments + 1];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 1, result, first.length, second.length - 1);

        return result;
    }
}
