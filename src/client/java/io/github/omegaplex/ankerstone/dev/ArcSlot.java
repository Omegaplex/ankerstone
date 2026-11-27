package io.github.omegaplex.ankerstone.dev;

import java.util.Random;
import java.util.List;
import java.util.random.RandomGenerator;

final class ArcSlot {
    final long seed;

    long cachedSelectionStep = -1;
    long cachedGeometryStep = -1;
    int cachedRegion = -1;
    int cachedSegments = -1;
    float cachedJitter = -1;
    float cachedLift = -1;

    ArcEdge cachedEdge;
    ArcSurface cachedSurface;
    ArcPoint cachedBend;
    ArcPoint[] cachedPoints;

    ArcSlot(long seed) {
        this.seed = seed;
    }

    boolean needsSelection(ArcTiming timing, int region) {
        return cachedSurface == null
                || timing.selectionStep() != cachedSelectionStep
                || region != cachedRegion;
    }

    boolean needsGeometry(ArcTiming timing, int region,
            int segments, float jitter, float lift) {
        return cachedPoints == null
                || needsSelection(timing, region)
                || timing.geometryStep() != cachedGeometryStep
                || segments != cachedSegments
                || jitter != cachedJitter
                || lift != cachedLift;
    }

    void refreshGeometry(ArcTiming timing, int region,
            int segments, float jitter, float lift) {
        if (!needsGeometry(timing, region, segments, jitter, lift)) {
            return;
        }

        ArcBounds bounds = cachedSurface.boundsForLift(lift, jitter);
        ArcPoint normal = cachedSurface.outward();
        var random = new Random(seed + timing.geometryStep());

        if (cachedBend != null && segments >= 2) {
            var raisedBend = new ArcPoint(
                    cachedBend.x() + normal.x() * lift,
                    cachedBend.y() + normal.y() * lift,
                    cachedBend.z() + normal.z() * lift);
            cachedPoints = ArcPath.via(
                    cachedEdge.start(), raisedBend, cachedEdge.end(),
                    segments, jitter, bounds, random);
        } else {
            cachedPoints = ArcPath.raised(
                    cachedEdge.start(), cachedEdge.end(), segments, jitter,
                    new ArcPoint(normal.x() * lift,
                            normal.y() * lift, normal.z() * lift),
                    bounds, random);
        }

        cachedSegments = segments;
        cachedJitter = jitter;
        cachedLift = lift;
        cachedGeometryStep = timing.geometryStep();
        cachedSelectionStep = timing.selectionStep();
        cachedRegion = region;
    }

    void selectSurface(List<ArcSurface> candidates, RandomGenerator random) {
        ArcSurface template = candidates.get(random.nextInt(candidates.size()));
        cachedBend = null;
        cachedEdge = template.edge().randomized(random);
        cachedSurface = new ArcSurface(
                cachedEdge, template.outward(), template.recessed());
    }
}
