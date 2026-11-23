package io.github.omegaplex.ankerstone.dev;

import java.util.ArrayList;
import java.util.List;

/** Cached patches derived from the current, axis-aligned Ankerstone model. */
final class ArcModelRegions {
    static final List<ArcEdge> RECESSES = recesses();
    static final List<ArcSurface> RECESS_SURFACES = recessSurfaces();
    static final List<ArcSurface> EXTERIORS = exteriors();
    static final List<ArcSurface> MIXED = mixed();
    static final List<ArcConnection> CONNECTIONS = connections();
    static final List<ArcConnection> ROUTED = routedConnections();

    private static List<ArcConnection> routedConnections() {
        var result = new ArrayList<ArcConnection>(24);
        // All intermediate geometry lies beyond the model's outer face plane:
        // model elements stay within [0,16] on every axis. Endpoints attach to
        // the north/east frame; the bend crosses above the intervening detail.
        for (int face = 0; face < 6; face++) {
            for (int rotation = 0; rotation < 4; rotation++) {
                ArcBounds corridor = faceBounds(1.1F, 15.9F, 0.1F, 12.9F, rotation, face);
                var edge = new ArcEdge(transform(2.5F, 16.01F, 0.5F, rotation, face),
                        transform(15.5F, 16.01F, 11.5F, rotation, face), corridor);
                result.add(new ArcConnection(new ArcSurface(edge, outward(face), false),
                        faceBounds(1.1F, 3.9F, 0.1F, 0.9F, rotation, face),
                        faceBounds(15.1F, 15.9F, 10.1F, 12.9F, rotation, face),
                        transform(12.5F, 16.01F, 2.5F, rotation, face),
                        faceBounds(10.0F, 14.0F, 1.5F, 5.5F, rotation, face)));
            }
        }
        return List.copyOf(result);
    }

    private static ArcBounds faceBounds(float lowX, float highX, float lowZ, float highZ,
            int rotation, int face) {
        ArcPoint a = transform(lowX, 16.01F, lowZ, rotation, face);
        ArcPoint b = transform(highX, 16.01F, highZ, rotation, face);
        return new ArcBounds(
                new ArcPoint(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z())),
                new ArcPoint(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z())));
    }

    private static List<ArcConnection> connections() {
        var result = new ArrayList<ArcConnection>(24);
        // The outer rim is continuous across this corridor. Anchors stay on
        // separated rim stretches, so random sampling cannot collapse into a local spark.
        for (int face = 0; face < 6; face++) {
            for (int rotation = 0; rotation < 4; rotation++) {
                ArcBounds corridor = rimBounds(1.1F, 14.9F, rotation, face);
                var edge = new ArcEdge(
                        transform(1.5F, 16.01F, 0.5F, rotation, face),
                        transform(14.5F, 16.01F, 0.5F, rotation, face), corridor);
                result.add(new ArcConnection(new ArcSurface(edge, outward(face), false),
                        rimBounds(1.1F, 3.9F, rotation, face),
                        rimBounds(12.1F, 14.9F, rotation, face)));
            }
        }
        return List.copyOf(result);
    }

    private static ArcBounds rimBounds(float lowX, float highX, int rotation, int face) {
        ArcPoint a = transform(lowX, 16.01F, 0.1F, rotation, face);
        ArcPoint b = transform(highX, 16.01F, 0.9F, rotation, face);
        return new ArcBounds(
                new ArcPoint(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z())),
                new ArcPoint(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z())));
    }

    private static List<ArcSurface> recessSurfaces() {
        var result = new ArrayList<ArcSurface>(24);
        for (int i = 0; i < RECESSES.size(); i++) {
            result.add(new ArcSurface(RECESSES.get(i), outward(i / 4), true));
        }
        return List.copyOf(result);
    }

    private static List<ArcSurface> exteriors() {
        var result = new ArrayList<ArcSurface>(24);
        // Narrow strips on the outer frame, clear of model solids at 16.01.
        // Kept separate from recess patches: no path bridges the intervening geometry.
        for (int face = 0; face < 6; face++) {
            for (int rotation = 0; rotation < 4; rotation++) {
                ArcPoint a = transform(1.1F, 16.01F, 0.1F, rotation, face);
                ArcPoint b = transform(6.9F, 16.01F, 0.9F, rotation, face);
                var bounds = new ArcBounds(
                        new ArcPoint(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z())),
                        new ArcPoint(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z())));
                var edge = new ArcEdge(transform(1.2F, 16.01F, 0.5F, rotation, face),
                        transform(6.8F, 16.01F, 0.5F, rotation, face), bounds);
                result.add(new ArcSurface(edge, outward(face), false));
            }
        }
        return List.copyOf(result);
    }

    private static List<ArcSurface> mixed() {
        var result = new ArrayList<>(RECESS_SURFACES);
        result.addAll(EXTERIORS);
        return List.copyOf(result);
    }

    private static ArcPoint outward(int face) {
        return switch (face) {
            case 0 -> new ArcPoint(0, 1, 0);
            case 1 -> new ArcPoint(0, -1, 0);
            case 2 -> new ArcPoint(1, 0, 0);
            case 3 -> new ArcPoint(-1, 0, 0);
            case 4 -> new ArcPoint(0, 0, 1);
            case 5 -> new ArcPoint(0, 0, -1);
            default -> throw new IllegalArgumentException("Invalid face");
        };
    }

    private ArcModelRegions() { }

    private static List<ArcEdge> recesses() {
        var result = new ArrayList<ArcEdge>(24);
        // Four copies of the validated top pocket, transformed onto each core face.
        // Model core bounds are [0.5,0.5,0.5]..[15.5,15.5,15.5]; the 0.01 lift
        // prevents depth fighting. The surrounding rim remains above each patch.
        for (int face = 0; face < 6; face++) {
            for (int rotation = 0; rotation < 4; rotation++) {
                ArcPoint a = transform(6.1F, 15.51F, 6.1F, rotation, face);
                ArcPoint b = transform(9.9F, 15.51F, 6.9F, rotation, face);
                var bounds = new ArcBounds(
                        new ArcPoint(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z())),
                        new ArcPoint(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z())));
                result.add(new ArcEdge(
                        transform(6.2F, 15.51F, 6.5F, rotation, face),
                        transform(9.8F, 15.51F, 6.5F, rotation, face), bounds));
            }
        }
        return List.copyOf(result);
    }

    private static ArcPoint transform(float x, float y, float z, int rotation, int face) {
        for (int step = 0; step < rotation; step++) {
            float previousX = x;
            x = 16 - z;
            z = previousX;
        }
        return switch (face) {
            case 0 -> ArcPoint.fromModel(x, y, z);
            case 1 -> ArcPoint.fromModel(x, 16 - y, z);
            case 2 -> ArcPoint.fromModel(y, x, z);
            case 3 -> ArcPoint.fromModel(16 - y, x, z);
            case 4 -> ArcPoint.fromModel(x, z, y);
            case 5 -> ArcPoint.fromModel(x, z, 16 - y);
            default -> throw new IllegalArgumentException("Invalid face");
        };
    }
}
