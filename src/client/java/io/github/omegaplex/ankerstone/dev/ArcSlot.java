package io.github.omegaplex.ankerstone.dev;

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
}
