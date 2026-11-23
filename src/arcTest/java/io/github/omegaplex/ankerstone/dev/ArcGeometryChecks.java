package io.github.omegaplex.ankerstone.dev;

import java.util.Arrays;
import java.util.Random;

public final class ArcGeometryChecks {
    private static int checks;

    private ArcGeometryChecks() { }

    public static void main(String[] args) {
        conversionAndStraightPaths();
        jitterPreservesEndpointsAndBounds();
        deterministicAndZeroJitter();
        invalidInputs();
        tuningLimits();
        raisedPaths();
        raisedBoundsAndInvalidInputs();
        timingBoundaries();
        timingSampling();
        endpointSampling();
        endpointSamplingFallback();
        independentInstances();
        irregularActivityWindows();
        irregularActivitySampling();
        invalidActivityInputs();
        intensityScaling();
        modelRecessRegions();
        surfaceLiftDirections();
        longConnections();
        routedPaths();
        randomizedBends();
        System.out.println("Arc geometry and timing: " + checks + " checks passed.");
    }

    private static void conversionAndStraightPaths() {
        check(ArcPoint.fromModel(4, 8, 16).equals(new ArcPoint(0.25F, 0.5F, 1)),
                "Model coordinates must be divided by 16");
        var start = new ArcPoint(0, 0.25F, 0.5F);
        var end = new ArcPoint(1, 0.75F, 0.5F);
        for (int segments = 1; segments <= 16; segments++) {
            var points = ArcPath.between(start, end, segments);
            check(points.length == segments + 1, "N segments require N+1 points");
            check(points[0].equals(start), "Start must be unchanged");
            check(points[segments].equals(end), "End must be unchanged");
            for (int i = 1; i < segments; i++) {
                float fraction = (float) i / segments;
                check(points[i].equals(new ArcPoint(fraction,
                        0.25F + 0.5F * fraction, 0.5F)), "Intermediate points must interpolate");
            }
        }
        check(Arrays.stream(ArcPath.between(start, start, 4)).allMatch(start::equals),
                "Coincident endpoints must not produce nonfinite coordinates");
    }

    private static void jitterPreservesEndpointsAndBounds() {
        var bounds = new ArcBounds(new ArcPoint(0, 0, 0.5F), new ArcPoint(1, 1, 0.5F));
        var start = new ArcPoint(0, 0, 0.5F);
        var end = new ArcPoint(1, 1, 0.5F);
        for (int seed = 0; seed < 100; seed++) {
            for (int segments = 1; segments <= 16; segments++) {
                var points = ArcPath.jittered(start, end, segments, 0.25F, bounds, new Random(seed));
                check(points.length == segments + 1, "Jitter must preserve point count");
                check(points[0].equals(start) && points[segments].equals(end),
                        "Jitter must preserve both endpoints");
                for (int i = 0; i <= segments; i++) {
                    var point = points[i];
                    check(bounds.contains(point), "Every point must remain inside bounds");
                    check(point.z() == 0.5F, "A planar region must stay planar");
                    float fraction = (float) i / segments;
                    check(Math.abs(point.x() - fraction) <= 0.250001F
                            && Math.abs(point.y() - fraction) <= 0.250001F,
                            "Displacement must not exceed the requested amplitude");
                }
            }
        }
        check(bounds.constrain(new ArcPoint(-1, 2, 3)).equals(new ArcPoint(0, 1, 0.5F)),
                "Constrain must clamp every axis");
    }

    private static void deterministicAndZeroJitter() {
        var bounds = new ArcBounds(new ArcPoint(0, 0, 0), new ArcPoint(1, 1, 1));
        var start = new ArcPoint(0.25F, 0.25F, 0.25F);
        var end = new ArcPoint(0.75F, 0.75F, 0.75F);
        var first = ArcPath.jittered(start, end, 4, 0.1F, bounds, new Random(42));
        var repeated = ArcPath.jittered(start, end, 4, 0.1F, bounds, new Random(42));
        var different = ArcPath.jittered(start, end, 4, 0.1F, bounds, new Random(43));
        check(Arrays.equals(first, repeated), "The same seed must reproduce the path");
        check(!Arrays.equals(first, different), "Different seeds must change intermediate geometry");
        check(!Arrays.equals(first, ArcPath.between(start, end, 4)), "Positive jitter must move points");
        check(Arrays.equals(ArcPath.between(start, end, 4),
                ArcPath.jittered(start, end, 4, 0, bounds, new Random(42))),
                "Zero jitter must reproduce the straight path");
    }

    private static void invalidInputs() {
        var low = new ArcPoint(0, 0, 0);
        var high = new ArcPoint(1, 1, 1);
        var bounds = new ArcBounds(low, high);
        for (int segments : new int[] {0, -1, 17}) {
            rejects(() -> ArcPath.between(low, high, segments), "Invalid segment count");
        }
        for (float amplitude : new float[] {-1, Float.NaN, Float.POSITIVE_INFINITY}) {
            rejects(() -> ArcPath.jittered(low, high, 4, amplitude, bounds, new Random(1)),
                    "Invalid jitter amplitude");
        }
        rejects(() -> new ArcBounds(high, low), "Reversed bounds");
        rejects(() -> new ArcBounds(null, high), "Null bounds point");
        rejects(() -> new ArcBounds(low, new ArcPoint(1, Float.NaN, 1)), "Nonfinite bounds");
        rejects(() -> ArcPath.jittered(new ArcPoint(-1, 0, 0), high, 4, 0.1F, bounds, new Random(1)),
                "Endpoint outside region");
        check(!bounds.contains(new ArcPoint(Float.NaN, 0, 0)), "Nonfinite points must be outside");
    }

    private static void tuningLimits() {
        for (var option : ArcOption.values()) {
            option.reset();
            double original = option.value();
            check(option.set(original), "Default tuning must be accepted");
            check(!option.set(Double.NaN) && !option.set(Double.POSITIVE_INFINITY),
                    "Nonfinite tuning must be rejected");
            check(!option.set(option.minimum() - 1) && !option.set(option.maximum() + 1),
                    "Out-of-range tuning must be rejected");
            check(option.value() == original, "Rejected tuning must not change the setting");
            check(option.set(option.minimum()) && option.set(option.maximum()),
                    "Tuning endpoints must be accepted");
            option.reset();
            check(option.value() == original, "Reset must restore the default");
        }
        check(!ArcOption.SEGMENTS.set(2.5), "Segment count must be a whole number");
    }

    private static void raisedPaths() {
        var start = new ArcPoint(0, 0, 0.5F);
        var end = new ArcPoint(1, 0, 0.5F);
        var bounds = new ArcBounds(new ArcPoint(0, 0, 0), new ArcPoint(1, 1, 1));
        var lift = new ArcPoint(0, 0.25F, 0);
        for (int segments = 1; segments <= 16; segments++) {
            var points = ArcPath.raised(start, end, segments, 0, lift, bounds, new Random(42));
            check(points.length == segments + 1, "Raised paths must preserve point count");
            check(points[0].equals(start) && points[segments].equals(end),
                    "Lift must leave both attachments unchanged");
            for (int i = 1; i < segments; i++) {
                float fraction = (float) i / segments;
                float expectedHeight = fraction * (1 - fraction);
                check(Math.abs(points[i].y() - expectedHeight) < 0.000001F,
                        "Lift must follow the specified outward envelope");
                check(points[i].x() == fraction && points[i].z() == 0.5F,
                        "A vertical lift must not move the other axes");
            }
        }
        var first = ArcPath.raised(start, end, 4, 0.1F, lift, bounds, new Random(42));
        check(Arrays.equals(first,
                ArcPath.raised(start, end, 4, 0.1F, lift, bounds, new Random(42))),
                "Raised jitter must be reproducible from its seed");
        check(Arrays.equals(ArcPath.jittered(start, end, 4, 0.1F, bounds, new Random(42)),
                ArcPath.raised(start, end, 4, 0.1F, new ArcPoint(0, 0, 0), bounds, new Random(42))),
                "Zero lift must preserve the existing jittered result");
        var centeredStart = new ArcPoint(0.25F, 0.5F, 0.5F);
        var centeredEnd = new ArcPoint(0.75F, 0.5F, 0.5F);
        for (var direction : new ArcPoint[] {
                new ArcPoint(0.25F, 0, 0), new ArcPoint(-0.25F, 0, 0),
                new ArcPoint(0, 0.25F, 0), new ArcPoint(0, -0.25F, 0),
                new ArcPoint(0, 0, 0.25F), new ArcPoint(0, 0, -0.25F)}) {
            var points = ArcPath.raised(centeredStart, centeredEnd, 4, 0,
                    direction, bounds, new Random(42));
            check(points[2].equals(new ArcPoint(0.5F + direction.x(),
                    0.5F + direction.y(), 0.5F + direction.z())),
                    "Lift must support outward offsets for all six face directions");
        }
    }

    private static void raisedBoundsAndInvalidInputs() {
        var start = new ArcPoint(0, 0, 0.5F);
        var end = new ArcPoint(1, 0, 0.5F);
        var bounds = new ArcBounds(new ArcPoint(0, 0, 0), new ArcPoint(1, 0.1F, 1));
        for (int seed = 0; seed < 100; seed++) {
            var points = ArcPath.raised(start, end, 16, 0.25F,
                    new ArcPoint(0, 0.5F, 0), bounds, new Random(seed));
            check(Arrays.stream(points).allMatch(bounds::contains),
                    "Raised jitter must stay within its permitted volume");
        }
        var plane = new ArcBounds(new ArcPoint(0, 0, 0), new ArcPoint(1, 0, 1));
        check(Arrays.stream(ArcPath.raised(start, end, 4, 0.1F,
                new ArcPoint(0, 0.25F, 0), plane, new Random(42))).allMatch(p -> p.y() == 0),
                "Flat bounds must not permit lift through the surface");
        for (ArcPoint invalid : new ArcPoint[] {null, new ArcPoint(Float.NaN, 0, 0),
                new ArcPoint(0, Float.POSITIVE_INFINITY, 0), new ArcPoint(0, 0, Float.NEGATIVE_INFINITY)}) {
            rejects(() -> ArcPath.raised(start, end, 4, 0, invalid, bounds, new Random(42)),
                    "Nonfinite lift");
        }
        rejects(() -> ArcPath.raised(start, end, 0, 0, new ArcPoint(0, 0.1F, 0), bounds, new Random(42)),
                "Invalid raised segment count");
        rejects(() -> new ArcEdge(start, start, bounds), "Zero-length candidate edge");
        rejects(() -> new ArcEdge(start, new ArcPoint(2, 0, 0), bounds), "Candidate outside its region");
    }

    private static void timingBoundaries() {
        check(ArcTiming.at(0, 120, 33, 400, 200).equals(new ArcTiming(0, 0, true)),
                "Timing must start active at step zero");
        check(ArcTiming.at(119, 120, 33, 400, 200).selectionStep() == 0
                && ArcTiming.at(120, 120, 33, 400, 200).selectionStep() == 1,
                "Selection must advance exactly at its interval");
        check(ArcTiming.at(32, 120, 33, 400, 200).geometryStep() == 0
                && ArcTiming.at(33, 120, 33, 400, 200).geometryStep() == 1,
                "Geometry must advance independently at its interval");
        for (long time : new long[] {399, 400, 599, 600}) {
            boolean expected = time == 399 || time == 600;
            check(ArcTiming.at(time, 120, 33, 400, 200).active() == expected,
                    "Burst and quiet boundaries must be exact at " + time);
        }
        check(ArcTiming.at(1_000_000, 120, 33, 400, 0).active(), "Zero quiet time must stay active");
        check(ArcTiming.at(100, 500, 25, 400, 200).equals(new ArcTiming(0, 4, true)),
                "Geometry refresh must not wait for selection");
        check(ArcTiming.at(100, 25, 500, 400, 200).equals(new ArcTiming(4, 0, true)),
                "Selection must not wait for geometry refresh");
        check(ArcTiming.at(Long.MAX_VALUE, 120, 33, 400, 200).selectionStep() == Long.MAX_VALUE / 120,
                "Large elapsed times must not narrow to an integer");
        check(ArcTiming.at(3_000_000_000L, 120, 33, Integer.MAX_VALUE, Integer.MAX_VALUE).active() == false,
                "Cycle length must not overflow an integer");
        rejects(() -> ArcTiming.at(-1, 120, 33, 400, 200), "Negative elapsed time");
        rejects(() -> ArcTiming.at(0, 0, 33, 400, 200), "Zero selection interval");
        rejects(() -> ArcTiming.at(0, 120, 0, 400, 200), "Zero geometry interval");
        rejects(() -> ArcTiming.at(0, 120, 33, 0, 200), "Zero burst interval");
        rejects(() -> ArcTiming.at(0, 120, 33, 400, -1), "Negative quiet interval");
    }

    private static void timingSampling() {
        // Frame sampling may omit intermediate states, but must not change the state at a given time.
        for (long stride : new long[] {7, 16, 33, 100, 1000}) {
            for (long time = 0; time <= 10_000; time += stride) {
                var timing = ArcTiming.at(time, 120, 33, 400, 200);
                check(timing.selectionStep() == time / 120 && timing.geometryStep() == time / 33
                        && timing.active() == (time % 600 < 400),
                        "Sampling cadence must not alter the state at " + time);
            }
        }
        for (long time : new long[] {120, 400, 600, 1000}) {
            var original = ArcTiming.at(time, 120, 33, 400, 200);
            ArcTiming.at(time + 100_000, 500, 200, 900, 0);
            check(original.equals(ArcTiming.at(time, 120, 33, 400, 200)),
                    "Timing must not depend on earlier calls or replay skipped frames");
        }
    }

    private static void endpointSampling() {
        // Synthetic planes exercise all three orientations independently of the top-face renderer fixture.
        var regions = new ArcBounds[] {
                new ArcBounds(new ArcPoint(0.5F, 0, 0), new ArcPoint(0.5F, 1, 1)),
                new ArcBounds(new ArcPoint(0, 0.5F, 0), new ArcPoint(1, 0.5F, 1)),
                new ArcBounds(new ArcPoint(0, 0, 0.5F), new ArcPoint(1, 1, 0.5F)),
                new ArcBounds(ArcPoint.fromModel(6.1F, 15.51F, 6.1F),
                        ArcPoint.fromModel(9.9F, 15.51F, 6.9F))};
        for (var bounds : regions) {
            var original = new ArcEdge(bounds.minimum(), bounds.maximum(), bounds);
            for (int seed = 0; seed < 100; seed++) {
                var sampled = original.randomized(new java.util.SplittableRandom(seed));
                check(bounds.contains(sampled.start()) && bounds.contains(sampled.end()),
                        "Sampled endpoints must remain in their permitted region");
                check(!sampled.start().equals(sampled.end()), "Sampled endpoints must be distinct");
                check(sampled.bounds().equals(bounds), "Sampling must preserve the permitted region");
                check(sampled.equals(original.randomized(new java.util.SplittableRandom(seed))),
                        "The same seed must reproduce endpoint selection");
                check(!sampled.equals(original.randomized(new java.util.SplittableRandom(seed + 1))),
                        "Changing the seed must allow endpoint relocation");
                if (bounds.minimum().x() == bounds.maximum().x()) {
                    check(sampled.start().x() == bounds.minimum().x()
                            && sampled.end().x() == bounds.minimum().x(), "X-plane attachment must remain fixed");
                }
                if (bounds.minimum().y() == bounds.maximum().y()) {
                    check(sampled.start().y() == bounds.minimum().y()
                            && sampled.end().y() == bounds.minimum().y(), "Y-plane attachment must remain fixed");
                }
                if (bounds.minimum().z() == bounds.maximum().z()) {
                    check(sampled.start().z() == bounds.minimum().z()
                            && sampled.end().z() == bounds.minimum().z(), "Z-plane attachment must remain fixed");
                }
            }
            check(original.start().equals(bounds.minimum()) && original.end().equals(bounds.maximum()),
                    "Sampling must not mutate the original candidate");
        }
    }

    private static void endpointSamplingFallback() {
        var bounds = new ArcBounds(new ArcPoint(0, 0, 0), new ArcPoint(1, 1, 1));
        var original = new ArcEdge(bounds.minimum(), bounds.maximum(), bounds);
        int[] calls = {0};
        var repeatedPointRandom = new Random(42) {
            @Override
            public float nextFloat() {
                if (++calls[0] > 48) {
                    throw new AssertionError("Endpoint sampling exceeded its eight-attempt budget");
                }
                return 0;
            }
        };
        check(original.randomized(repeatedPointRandom).equals(original),
                "Repeated identical samples must fall back to the valid original candidate");
        check(calls[0] == 48, "Eight attempts must sample two three-coordinate points each");
    }

    private static void independentInstances() {
        var first = new ArcInstance(123, 1_000_000_000L);
        var second = new ArcInstance(456, 1_200_000_000L);
        check(first.seed == 123 && second.seed == 456, "Each instance must retain its own seed");
        long now = 1_450_000_000L;
        var firstTiming = ArcTiming.at((now - first.startedNanos) / 1_000_000L, 120, 33, 400, 200);
        var secondTiming = ArcTiming.at((now - second.startedNanos) / 1_000_000L, 120, 33, 400, 200);
        check(!firstTiming.active() && secondTiming.active(),
                "Different start times must permit different activity phases at the same instant");
        var bounds = new ArcBounds(new ArcPoint(0, 0, 0), new ArcPoint(1, 0, 1));
        var candidate = new ArcEdge(bounds.minimum(), bounds.maximum(), bounds);
        var a = first.slot(0);
        var b = second.slot(0);
        a.cachedEdge = candidate.randomized(new java.util.SplittableRandom(a.seed));
        b.cachedEdge = candidate.randomized(new java.util.SplittableRandom(b.seed));
        check(!a.cachedEdge.equals(b.cachedEdge), "Distinct seeds must permit distinct paths");
        a.cachedSelectionStep = 7;
        a.cachedGeometryStep = 9;
        a.cachedRegion = 1;
        a.cachedSegments = 8;
        a.cachedJitter = 0.02F;
        a.cachedLift = 0.04F;
        a.cachedPoints = new ArcPoint[] {a.cachedEdge.start(), a.cachedEdge.end()};
        check(b.cachedSelectionStep == -1 && b.cachedGeometryStep == -1
                && b.cachedRegion == -1 && b.cachedSegments == -1
                && b.cachedJitter == -1 && b.cachedLift == -1 && b.cachedPoints == null,
                "Updating one instance must not change another instance's caches");
        check(first.slot(0) == a, "Revisiting a slot must preserve its existing cache");
        var seeds = new java.util.HashSet<Long>();
        for (int index = 0; index < 32; index++) {
            var slot = first.slot(index);
            check(seeds.add(slot.seed), "Slots within one block must have distinct seeds");
            if (index > 0) {
                check(slot.cachedPoints == null && slot.cachedSelectionStep == -1,
                        "Updating one slot must not modify sibling slots");
            }
        }
        rejects(() -> first.slot(-1), "Negative slot index");
        rejects(() -> first.slot(32), "Slot index beyond budget");
        for (int maximum = 0; maximum <= 32; maximum++) {
            int previous = 0;
            for (int step = 0; step <= 100; step++) {
                int count = ArcInstance.slotCount(step / 100.0, maximum);
                check(count >= previous && count <= maximum, "Slot count must rise monotonically within its cap");
                check(maximum == 0 ? count == 0 : count >= 1, "Zero cap must disable slots; positive caps retain one");
                previous = count;
            }
            check(ArcInstance.slotCount(0, maximum) == (maximum == 0 ? 0 : 1), "Baseline must retain one slot");
            check(ArcInstance.slotCount(1, maximum) == maximum, "Critical intensity must reach the configured cap");
        }
        rejects(() -> ArcInstance.slotCount(Double.NaN, 3), "Nonfinite slot intensity");
        rejects(() -> ArcInstance.slotCount(-1, 3), "Negative slot intensity");
        rejects(() -> ArcInstance.slotCount(2, 3), "Excessive slot intensity");
        rejects(() -> ArcInstance.slotCount(0, -1), "Negative slot cap");
        rejects(() -> ArcInstance.slotCount(0, 33), "Excessive slot cap");
    }

    private static void irregularActivityWindows() {
        var durations = new java.util.HashSet<Integer>();
        var offsets = new java.util.HashSet<Integer>();
        for (long seed : new long[] {0, 1, 42, -1, Long.MIN_VALUE, Long.MAX_VALUE}) {
            for (long window = 0; window < 20; window++) {
                int first = -1;
                int last = -1;
                int count = 0;
                for (int position = 0; position < 800; position++) {
                    if (ArcActivity.activeAt(window * 800 + position, 300, 500, seed)) {
                        if (first < 0) {
                            first = position;
                        }
                        last = position;
                        count++;
                    }
                }
                check(count >= 150 && count <= 450, "Burst duration must stay within the chosen limits");
                check(last - first + 1 == count, "Each scheduling window must contain one contiguous burst");
                check(first >= 0 && last < 800, "Activity must stay within its scheduling window");
                check(ArcActivity.activeAt(window * 800 + first, 300, 500, seed)
                        && ArcActivity.activeAt(window * 800 + last, 300, 500, seed),
                        "First and last burst milliseconds must be active");
                if (first > 0) {
                    check(!ArcActivity.activeAt(window * 800 + first - 1, 300, 500, seed),
                            "The millisecond before the burst must be quiet");
                }
                if (last < 799) {
                    check(!ArcActivity.activeAt(window * 800 + last + 1, 300, 500, seed),
                            "The millisecond after the burst must be quiet");
                }
                durations.add(count);
                offsets.add(first);
            }
        }
        check(durations.size() > 1 && offsets.size() > 1,
                "The fixed seed/window fixtures must vary both burst duration and placement");
    }

    private static void irregularActivitySampling() {
        boolean[] dense = new boolean[10_001];
        boolean differentSeed = false;
        for (int time = 0; time < dense.length; time++) {
            dense[time] = ArcActivity.activeAt(time, 300, 500, 42);
            differentSeed |= dense[time] != ArcActivity.activeAt(time, 300, 500, 43);
            check(ArcActivity.activeAt(time, 300, 0, 42), "Zero quiet time must disable activity gaps");
        }
        check(differentSeed, "Different instance seeds must permit different activity patterns");
        for (int stride : new int[] {7, 16, 33, 100, 1000}) {
            for (int time = dense.length - 1; time >= 0; time -= stride) {
                check(dense[time] == ArcActivity.activeAt(time, 300, 500, 42),
                        "Reverse/sparse sampling must agree with the dense reference at the same time");
            }
        }
        for (long time : new long[] {0, 1, 2, Integer.MAX_VALUE, Long.MAX_VALUE}) {
            check(ArcActivity.activeAt(time, 1, 0, Long.MAX_VALUE), "Minimum burst with no gaps must stay active");
            boolean first = ArcActivity.activeAt(time, Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE);
            check(first == ArcActivity.activeAt(time, Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE),
                    "Large timestamps and duration sums must remain deterministic without overflow failures");
        }
    }

    private static void invalidActivityInputs() {
        rejects(() -> ArcActivity.activeAt(-1, 300, 500, 42), "Negative activity elapsed time");
        rejects(() -> ArcActivity.activeAt(0, 0, 500, 42), "Zero activity burst");
        rejects(() -> ArcActivity.activeAt(0, -1, 500, 42), "Negative activity burst");
        rejects(() -> ArcActivity.activeAt(0, 300, -1, 42), "Negative activity quiet interval");
    }

    private static void intensityScaling() {
        check(ArcIntensity.at(0, 120, 33, 500).equals(new ArcIntensity(120, 33, 500)),
                "Zero intensity must preserve the supported baseline settings");
        check(ArcIntensity.at(1, 120, 33, 500).equals(new ArcIntensity(42, 17, 50)),
                "Full intensity must apply the chosen timing factors");
        for (int[] base : new int[][] {{16, 1, 0}, {120, 33, 500}, {5000, 5000, 10000}}) {
            var previous = ArcIntensity.at(0, base[0], base[1], base[2]);
            for (int step = 0; step <= 100; step++) {
                var current = ArcIntensity.at(step / 100.0, base[0], base[1], base[2]);
                check(current.selectionMillis() >= 16 && current.refreshMillis() >= 1
                        && current.quietMillis() >= 0, "Intensity must preserve valid interval floors");
                check(current.selectionMillis() <= previous.selectionMillis()
                        && current.refreshMillis() <= previous.refreshMillis()
                        && current.quietMillis() <= previous.quietMillis(),
                        "Rising intensity must not slow the effect or lengthen gaps");
                if (base[2] == 0) {
                    check(current.quietMillis() == 0, "Intensity must preserve the continuous diagnostic setting");
                }
                previous = current;
            }
        }
        double width = ArcOption.WIDTH.value();
        ArcIntensity.at(1, 120, 33, 500);
        check(ArcOption.WIDTH.value() == width, "Intensity calculation must not change configured line width");
        for (double invalid : new double[] {-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            rejects(() -> ArcIntensity.at(invalid, 120, 33, 500), "Invalid intensity");
        }
        rejects(() -> ArcIntensity.at(0, 0, 33, 500), "Invalid base selection interval");
        rejects(() -> ArcIntensity.at(0, 120, 0, 500), "Invalid base refresh interval");
        rejects(() -> ArcIntensity.at(0, 120, 33, -1), "Invalid base quiet interval");
    }

    private static void modelRecessRegions() {
        var regions = ArcModelRegions.RECESSES;
        check(regions.size() == 24 && new java.util.HashSet<>(regions).size() == 24,
                "Model recesses must contain 24 distinct patches");
        for (int face = 0; face < 6; face++) {
            for (int pocket = 0; pocket < 4; pocket++) {
                ArcEdge edge = regions.get(face * 4 + pocket);
                ArcPoint low = edge.bounds().minimum();
                ArcPoint high = edge.bounds().maximum();
                int axis = face < 2 ? 1 : face < 4 ? 0 : 2;
                float[] minima = {low.x(), low.y(), low.z()};
                float[] maxima = {high.x(), high.y(), high.z()};
                float expected = (face % 2 == 0 ? 15.51F : 0.49F) / 16;
                check(Math.abs(minima[axis] - expected) < 0.000001F && minima[axis] == maxima[axis],
                        "Each patch must lie on its intended recessed core face");
                for (int a = 0; a < 3; a++) {
                    check(minima[a] >= 0 && maxima[a] <= 1, "Recess regions must stay within the block");
                }
                for (int seed = 0; seed < 20; seed++) {
                    var sampled = edge.randomized(new java.util.SplittableRandom(seed));
                    var points = ArcPath.jittered(sampled.start(), sampled.end(), 4, 0.02F,
                            edge.bounds(), new Random(seed));
                    check(Arrays.stream(points).allMatch(edge.bounds()::contains),
                            "Transformed recess paths must stay in their assigned patch");
                }
            }
        }
    }

    private static void surfaceLiftDirections() {
        check(ArcModelRegions.EXTERIORS.size() == 24 && ArcModelRegions.MIXED.size() == 48,
                "Mixed mode must contain the 24 recessed and 24 exterior patches");
        for (ArcSurface surface : ArcModelRegions.MIXED) {
            ArcBounds original = surface.edge().bounds();
            ArcBounds lifted = surface.boundsForLift(0.04F, 0.02F);
            check(surface.boundsForLift(0, 0.02F).equals(original), "Zero lift must retain planar bounds");
            if (surface.recessed()) {
                check(lifted.equals(original), "Recesses must reject outward volume expansion");
                continue;
            }
            ArcPoint normal = surface.outward();
            float[] direction = {normal.x(), normal.y(), normal.z()};
            float[] beforeLow = {original.minimum().x(), original.minimum().y(), original.minimum().z()};
            float[] beforeHigh = {original.maximum().x(), original.maximum().y(), original.maximum().z()};
            float[] afterLow = {lifted.minimum().x(), lifted.minimum().y(), lifted.minimum().z()};
            float[] afterHigh = {lifted.maximum().x(), lifted.maximum().y(), lifted.maximum().z()};
            for (int axis = 0; axis < 3; axis++) {
                check(Math.abs(afterLow[axis] - (beforeLow[axis] + Math.min(0, direction[axis] * 0.06F))) < 0.000001F
                        && Math.abs(afterHigh[axis] - (beforeHigh[axis] + Math.max(0, direction[axis] * 0.06F))) < 0.000001F,
                        "Exterior volume must expand outward only, leaving tangential bounds unchanged");
            }
            ArcEdge edge = surface.edge();
            var points = ArcPath.raised(edge.start(), edge.end(), 4, 0,
                    new ArcPoint(normal.x() * 0.04F, normal.y() * 0.04F, normal.z() * 0.04F),
                    lifted, new Random(42));
            check(points[0].equals(edge.start()) && points[4].equals(edge.end()), "Surface lift must preserve attachments");
            float[] middle = {points[2].x(), points[2].y(), points[2].z()};
            for (int axis = 0; axis < 3; axis++) {
                if (direction[axis] != 0) {
                    check(Math.abs((middle[axis] - beforeLow[axis]) * direction[axis] - 0.04F) < 0.000001F,
                            "Midpoint must lift away from every face, never into the model");
                }
            }
        }
        ArcEdge edge = ArcModelRegions.RECESSES.get(0);
        rejects(() -> new ArcSurface(edge, new ArcPoint(0, 0, 0), false), "Zero outward direction");
        rejects(() -> new ArcSurface(edge, new ArcPoint(1, 1, 0), false), "Diagonal outward direction");
        rejects(() -> new ArcSurface(edge, new ArcPoint(Float.NaN, 0, 0), false), "Nonfinite outward direction");
        rejects(() -> ArcModelRegions.EXTERIORS.get(0).boundsForLift(-1, 0), "Negative surface lift");
    }

    private static void longConnections() {
        check(ArcModelRegions.CONNECTIONS.size() == 24, "Each face must have four long rim routes");
        for (ArcConnection connection : ArcModelRegions.CONNECTIONS) {
            for (int seed = 0; seed < 100; seed++) {
                ArcSurface sampled = connection.sample(new java.util.SplittableRandom(seed));
                ArcEdge edge = sampled.edge();
                check(connection.startAnchor().contains(edge.start())
                        && connection.endAnchor().contains(edge.end()),
                        "Long connections must retain their separated endpoint anchors");
                double dx = edge.end().x() - edge.start().x();
                double dy = edge.end().y() - edge.start().y();
                double dz = edge.end().z() - edge.start().z();
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                check(length > 0.5 && length < 1, "Long route fixtures must span separated regions within the test cap");
                ArcBounds volume = sampled.boundsForLift(0.02F, 0.015F);
                ArcPoint normal = sampled.outward();
                var points = ArcPath.raised(edge.start(), edge.end(), 4, 0.015F,
                        new ArcPoint(normal.x() * 0.02F, normal.y() * 0.02F, normal.z() * 0.02F),
                        volume, new Random(seed));
                check(Arrays.stream(points).allMatch(volume::contains),
                        "Long path geometry must stay in its permitted lifted corridor");
                check(sampled.equals(connection.sample(new java.util.SplittableRandom(seed))),
                        "Connection sampling must be deterministic");
            }
        }
        ArcConnection first = ArcModelRegions.CONNECTIONS.get(0);
        var outside = new ArcBounds(new ArcPoint(-1, -1, -1), new ArcPoint(0, 0, 0));
        rejects(() -> new ArcConnection(first.route(), outside, first.endAnchor()), "Start anchor outside corridor");
        rejects(() -> new ArcConnection(first.route(), first.startAnchor(), outside), "End anchor outside corridor");
        check(ArcOption.MAX_LENGTH.value() == 1.5 && ArcOption.WIDTH.value() == 1,
                "Review defaults must permit long paths without changing line width");
    }

    private static void routedPaths() {
        check(ArcModelRegions.ROUTED.size() == 24, "Routed candidates must cover all face orientations");
        for (ArcConnection connection : ArcModelRegions.ROUTED) {
            for (int seed = 0; seed < 20; seed++) {
                var surface = connection.sample(new java.util.SplittableRandom(seed));
                var edge = surface.edge();
                var normal = surface.outward();
                var baseBend = connection.bend();
                var bend = new ArcPoint(baseBend.x() + normal.x() * 0.02F,
                        baseBend.y() + normal.y() * 0.02F, baseBend.z() + normal.z() * 0.02F);
                var volume = surface.boundsForLift(0.02F, 0.015F);
                for (int segments = 2; segments <= 16; segments++) {
                    var points = ArcPath.via(edge.start(), bend, edge.end(), segments,
                            0.015F, volume, new Random(seed));
                    check(points.length == segments + 1, "Routed paths must retain the requested segment count");
                    check(points[0].equals(edge.start()) && points[segments].equals(edge.end()),
                            "Routed paths must preserve their attachments");
                    check(points[segments / 2].equals(bend), "Routed paths must retain the deliberate turn");
                    check(Arrays.stream(points).allMatch(volume::contains),
                            "Routed geometry must remain in the checked outward corridor");
                    check(Arrays.equals(points, ArcPath.via(edge.start(), bend, edge.end(), segments,
                            0.015F, volume, new Random(seed))), "Routed jitter must be deterministic");
                }
            }
        }
        var fixture = ArcModelRegions.ROUTED.get(0);
        var edge = fixture.route().edge();
        var bounds = edge.bounds();
        for (int invalid : new int[] {0, 1, 17}) {
            rejects(() -> ArcPath.via(edge.start(), fixture.bend(), edge.end(), invalid, 0, bounds,
                    new Random(42)), "Invalid routed segment count");
        }
        rejects(() -> new ArcConnection(fixture.route(), fixture.startAnchor(), fixture.endAnchor(),
                new ArcPoint(-1, -1, -1)), "Bend outside routed corridor");
        rejects(() -> ArcPath.via(edge.start(), new ArcPoint(-1, -1, -1), edge.end(), 4, 0, bounds,
                new Random(42)), "Routed path bend outside bounds");
    }

    private static void randomizedBends() {
        for (ArcConnection connection : ArcModelRegions.ROUTED) {
            var unique = new java.util.HashSet<ArcPoint>();
            for (int seed = 0; seed < 100; seed++) {
                var random = new java.util.SplittableRandom(seed);
                ArcSurface surface = connection.sample(random);
                ArcPoint bend = connection.sampleBend(random);
                unique.add(bend);
                check(connection.bendRegion().contains(bend)
                        && surface.edge().bounds().contains(bend),
                        "Sampled turn must stay in its allowed region and route corridor");
                var repeated = new java.util.SplittableRandom(seed);
                connection.sample(repeated);
                check(bend.equals(connection.sampleBend(repeated)),
                        "Same selection seed must reproduce its endpoint and bend sequence");
                ArcPoint normal = surface.outward();
                var raised = new ArcPoint(bend.x() + normal.x() * 0.02F,
                        bend.y() + normal.y() * 0.02F, bend.z() + normal.z() * 0.02F);
                var volume = surface.boundsForLift(0.02F, 0.015F);
                var points = ArcPath.via(surface.edge().start(), raised, surface.edge().end(),
                        4, 0.015F, volume, new Random(seed));
                check(points[2].equals(raised) && Arrays.stream(points).allMatch(volume::contains),
                        "Randomized turn must remain fixed during geometry generation and preserve containment");
                var refreshed = ArcPath.via(surface.edge().start(), raised, surface.edge().end(),
                        4, 0.015F, volume, new Random(seed + 1));
                check(refreshed[0].equals(points[0]) && refreshed[2].equals(points[2])
                        && refreshed[4].equals(points[4]),
                        "Jitter refresh must not move the selected turn or attachments");
            }
            check(unique.size() > 1, "Different seeds must permit different turn positions");
        }
        ArcConnection straight = ArcModelRegions.CONNECTIONS.get(0);
        check(straight.sampleBend(new Random(42)) == null, "Straight connections must remain straight");
        ArcConnection fixture = ArcModelRegions.ROUTED.get(0);
        var fixed = new ArcConnection(fixture.route(), fixture.startAnchor(), fixture.endAnchor(), fixture.bend());
        check(fixed.sampleBend(new Random(42)).equals(fixed.bend()),
                "Fixed-turn fixtures must retain their reference turn");
        rejects(() -> new ArcConnection(fixture.route(), fixture.startAnchor(), fixture.endAnchor(),
                fixture.bend(), new ArcBounds(new ArcPoint(-1, -1, -1), new ArcPoint(2, 2, 2))),
                "Turn region outside corridor");
        rejects(() -> new ArcConnection(fixture.route(), fixture.startAnchor(), fixture.endAnchor(),
                null, fixture.bendRegion()), "Turn region with no reference turn");
        rejects(() -> new ArcConnection(fixture.route(), fixture.startAnchor(), fixture.endAnchor(),
                fixture.route().edge().start(), fixture.bendRegion()), "Reference turn outside turn region");
    }

    private static void rejects(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }
        throw new AssertionError(message + " was accepted");
    }

    private static void check(boolean passed, String message) {
        checks++;
        if (!passed) {
            throw new AssertionError(message);
        }
    }
}
