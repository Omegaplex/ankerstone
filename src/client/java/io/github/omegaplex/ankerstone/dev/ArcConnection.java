package io.github.omegaplex.ankerstone.dev;

import java.util.random.RandomGenerator;

public record ArcConnection(
        ArcSurface route,
        ArcBounds startAnchor,
        ArcBounds endAnchor,
        ArcPoint bend,
        ArcBounds bendRegion) {

    public ArcConnection(ArcSurface route, ArcBounds startAnchor, ArcBounds endAnchor) {
        this(route, startAnchor, endAnchor, null, null);
    }

    public ArcConnection(
            ArcSurface route, ArcBounds startAnchor,
            ArcBounds endAnchor, ArcPoint bend) {
        this(route, startAnchor, endAnchor, bend, null);
    }

    public ArcConnection {
        ArcBounds corridor = route.edge().bounds();
        if (bendRegion != null
                && (bend == null
                        || !bendRegion.contains(bend)
                        || !corridor.contains(bendRegion.minimum())
                        || !corridor.contains(bendRegion.maximum()))) {
            throw new IllegalArgumentException(
                    "Bend region must contain the reference bend and fit inside the corridor");
        }

        if (bend != null && !corridor.contains(bend)) {
            throw new IllegalArgumentException("Connection bend must fit inside the corridor");
        }

        if (!corridor.contains(startAnchor.minimum())
                || !corridor.contains(startAnchor.maximum())
                || !corridor.contains(endAnchor.minimum())
                || !corridor.contains(endAnchor.maximum())) {
            throw new IllegalArgumentException(
                    "Connection anchors must fit inside the corridor");
        }
    }

    public ArcSurface sample(RandomGenerator random) {
        ArcEdge edge = new ArcEdge(
                pointWithin(startAnchor, random),
                pointWithin(endAnchor, random),
                route.edge().bounds());

        return new ArcSurface(edge, route.outward(), route.recessed());
    }

    public ArcPoint sampleBend(RandomGenerator random) {
        return bendRegion == null ? bend : pointWithin(bendRegion, random);
    }

    private static ArcPoint pointWithin(
            ArcBounds bounds, RandomGenerator random) {
        ArcPoint low = bounds.minimum();
        ArcPoint high = bounds.maximum();

        return new ArcPoint(
                low.x() + (high.x() - low.x()) * random.nextFloat(),
                low.y() + (high.y() - low.y()) * random.nextFloat(),
                low.z() + (high.z() - low.z()) * random.nextFloat());
    }
}
