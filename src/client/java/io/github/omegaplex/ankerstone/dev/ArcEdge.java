package io.github.omegaplex.ankerstone.dev;

import java.util.random.RandomGenerator;

public record ArcEdge(
        ArcPoint start, ArcPoint end, ArcBounds bounds) {
    public ArcEdge {
        if (!bounds.contains(start) || !bounds.contains(end)) {
            throw new IllegalArgumentException(
                    "Edge endpoints must be inside their allowed region");
        }
        if (start.equals(end)) {
            throw new IllegalArgumentException(
                    "An eligible edge must have distinct endpoints");
        }
    }

    public ArcEdge randomized(RandomGenerator random) {
        for (int attempt = 0; attempt < 8; attempt++) {
            ArcPoint a = randomPoint(random);
            ArcPoint b = randomPoint(random);

            if (!a.equals(b)) {
                return new ArcEdge(a, b, bounds);
            }
        }

        return this;
    }

    private ArcPoint randomPoint(RandomGenerator random) {
        ArcPoint minimum = bounds.minimum();
        ArcPoint maximum = bounds.maximum();

        return new ArcPoint(
                minimum.x()
                        + (maximum.x() - minimum.x()) * random.nextFloat(),
                minimum.y()
                        + (maximum.y() - minimum.y()) * random.nextFloat(),
                minimum.z()
                        + (maximum.z() - minimum.z()) * random.nextFloat());
    }
}
