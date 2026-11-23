package io.github.omegaplex.ankerstone.dev;

public record ArcSurface(
        ArcEdge edge, ArcPoint outward, boolean recessed) {

    public ArcBounds boundsForLift(float lift, float jitter) {
        if (!Float.isFinite(lift) || !Float.isFinite(jitter) || lift < 0 || jitter < 0) {
            throw new IllegalArgumentException("Invalid surface lift");
        }
        ArcBounds bounds = edge.bounds();
        if (recessed || lift == 0) {
            return bounds;
        }
        ArcPoint low = bounds.minimum();
        ArcPoint high = bounds.maximum();
        float reach = lift + jitter;
        return new ArcBounds(
                new ArcPoint(low.x() + Math.min(0, outward.x() * reach),
                        low.y() + Math.min(0, outward.y() * reach),
                        low.z() + Math.min(0, outward.z() * reach)),
                new ArcPoint(high.x() + Math.max(0, outward.x() * reach),
                        high.y() + Math.max(0, outward.y() * reach),
                        high.z() + Math.max(0, outward.z() * reach)));
    }

    public ArcSurface {
        if (edge == null || outward == null
                || !Float.isFinite(outward.x())
                || !Float.isFinite(outward.y())
                || !Float.isFinite(outward.z())) {
            throw new IllegalArgumentException("Invalid arc surface");
        }

        int axes = (outward.x() != 0 ? 1 : 0)
                + (outward.y() != 0 ? 1 : 0)
                + (outward.z() != 0 ? 1 : 0);
        float lengthSquared = outward.x() * outward.x()
                + outward.y() * outward.y()
                + outward.z() * outward.z();

        if (axes != 1 || lengthSquared != 1) {
            throw new IllegalArgumentException(
                    "Outward direction must be one signed unit axis");
        }
    }
}
