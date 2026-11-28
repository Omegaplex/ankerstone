package io.github.omegaplex.ankerstone.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import io.github.omegaplex.ankerstone.AnkerstoneBlock;

/**
 * Development renderer for independently cached gem and recess paths.
 */
final class ArcRenderer {
    private static final RenderStateDataKey<List<DrawPath>> PATHS = RenderStateDataKey
            .create(() -> "ankerstone prototype paths");

    // Gem Y occupies model [7,0,7]..[9,16,9]. Inset endpoints and a 0.01-model-unit
    // lift avoid the shared edge and coplanar depth fighting without bypassing
    // occlusion.
    private static final List<ArcEdge> TOP_GEM_EDGES = List.of(
            topGemEdge(7.1F, 7.6F, 8.9F, 7.6F),
            topGemEdge(7.6F, 7.1F, 7.6F, 8.9F),
            topGemEdge(7.1F, 8.4F, 8.9F, 8.4F),
            topGemEdge(8.4F, 7.1F, 8.4F, 8.9F));
    private static final List<ArcSurface> GEM_SURFACES = TOP_GEM_EDGES.stream()
            .map(edge -> new ArcSurface(edge, new ArcPoint(0, 1, 0), false)).toList();

    private static ArcEdge topGemEdge(float x1, float z1, float x2, float z2) {
        return new ArcEdge(
                ArcPoint.fromModel(x1, 16.01F, z1),
                ArcPoint.fromModel(x2, 16.01F, z2),
                new ArcBounds(
                        ArcPoint.fromModel(7.1F, 16.01F, 7.1F),
                        ArcPoint.fromModel(8.9F, 16.01F, 8.9F)));
    }

    private ArcRenderer() {
    }

    static void initialize() {
        LevelExtractionEvents.END_EXTRACTION.register(ArcRenderer::extract);
        LevelRenderEvents.COLLECT_SUBMITS.register(ArcRenderer::submit);
    }

    private static void extract(LevelExtractionContext context) {
        var instances = ArcPrototype.instances(context.level());
        if (instances.isEmpty() || ArcOption.MAX_LINES.value() == 0) {
            context.levelState().setData(PATHS, List.of());
            return;
        }
        long now = System.nanoTime();
        int count = ArcInstance.slotCount(ArcOption.INTENSITY.value(), (int) ArcOption.MAX_LINES.value());
        var paths = new ArrayList<DrawPath>(instances.size() * count);
        var entries = instances.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            BlockPos position = entry.getKey();
            if (!context.level().hasChunkAt(position)) {
                continue;
            }
            if (!(context.level().getBlockState(position).getBlock() instanceof AnkerstoneBlock)) {
                entries.remove();
                continue;
            }
            var camera = context.levelState().cameraRenderState;
            double x = position.getX() - camera.pos.x;
            double y = position.getY() - camera.pos.y;
            double z = position.getZ() - camera.pos.z;
            if (x * x + y * y + z * z > 64 * 64
                    || !camera.cullFrustum.isVisible(new AABB(position).inflate(0.4))) {
                continue;
            }
            ArcInstance instance = entry.getValue();
            for (int index = 0; index < count; index++) {
                DrawPath path = extractPath(
                        instance, instance.slot(index), now, x, y, z);
                if (path != null) {
                    paths.add(path);
                }
            }
        }
        context.levelState().setData(PATHS, List.copyOf(paths));
    }

    static DrawPath extractPath(
            ArcInstance instance, ArcSlot slot, long now,
            double x, double y, double z) {
        float brightness = (float) ArcOption.BRIGHTNESS.value();
        int color = 0xFF000000
                | Math.round((float) ArcOption.RED.value() * brightness * 255) << 16
                | Math.round((float) ArcOption.GREEN.value() * brightness * 255) << 8
                | Math.round((float) ArcOption.BLUE.value() * brightness * 255);
        var style = new Style(
                (int) ArcOption.REGION.value(),
                (int) ArcOption.SEGMENTS.value(),
                (float) ArcOption.JITTER.value(),
                (float) ArcOption.LIFT.value(),
                ArcOption.MAX_LENGTH.value(),
                ArcOption.CONNECTION_CHANCE.value(),
                ArcOption.ROUTED_CHANCE.value(), color,
                (float) ArcOption.WIDTH.value());
        return extractPath(instance, slot, now, x, y, z, style);
    }

    static DrawPath extractPath(
            ArcInstance instance, ArcSlot slot, long now,
            double x, double y, double z, Style style) {
        var timing = instance.timingAt(now);

        if (!instance.activeAt(now, slot.seed)) {
            return null;
        }
        int region = style.region();
        var candidates = switch (region) {
            case 1 -> ArcModelRegions.RECESS_SURFACES;
            case 2 -> ArcModelRegions.MIXED;
            default -> GEM_SURFACES;
        };
        if (slot.needsSelection(timing, region)) {
            var selectionRandom = new SplittableRandom(slot.seed + timing.selectionStep());
            slot.selectSurface(candidates, selectionRandom);
            if (region == 2 && selectionRandom.nextDouble() < style.connectionChance()) {
                var routes = selectionRandom.nextDouble() < style.routedChance()
                        ? ArcModelRegions.ROUTED
                        : ArcModelRegions.CONNECTIONS;
                var connection = routes.get(selectionRandom.nextInt(routes.size()));
                ArcSurface sampled = connection.sample(selectionRandom);
                ArcPoint sampledBend = connection.sampleBend(selectionRandom);
                if (length(sampled.edge(), sampledBend) <= style.maxLength()) {
                    slot.cachedSurface = sampled;
                    slot.cachedEdge = sampled.edge();
                    slot.cachedBend = sampledBend;
                }
            }
        }
        ArcEdge edge = slot.cachedEdge;
        int segments = style.segments();
        float jitter = style.jitter();
        ArcSurface surface = slot.cachedSurface;
        float lift = surface.recessed() ? 0 : style.lift();

        slot.refreshGeometry(timing, region, segments, jitter, lift);

        if (length(edge, slot.cachedBend) > style.maxLength()) {
            return null;
        }
        int color = style.color();
        float width = style.width();
        // Submitted arrays are replaced on refresh, never modified while a frame may
        // still use them.
        return new DrawPath(x, y, z, slot.cachedPoints, color, width);
    }

    private static double length(ArcEdge edge) {
        double dx = edge.end().x() - edge.start().x();
        double dy = edge.end().y() - edge.start().y();
        double dz = edge.end().z() - edge.start().z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double length(ArcEdge edge, ArcPoint bend) {
        if (bend == null) {
            return length(edge);
        }
        return distance(edge.start(), bend) + distance(bend, edge.end());
    }

    private static double distance(ArcPoint a, ArcPoint b) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double dz = b.z() - a.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void submit(LevelRenderContext context) {
        List<DrawPath> paths = context.levelState().getData(PATHS);
        if (paths == null || paths.isEmpty()) {
            return;
        }
        var poses = context.poseStack();
        for (DrawPath path : paths) {
            poses.pushPose();
            poses.translate(path.x(), path.y(), path.z());
            context.submitNodeCollector().submitCustomGeometry(poses,
                    RenderTypes.linesTranslucent(), path::draw);
            poses.popPose();
        }
    }

    record DrawPath(double x, double y, double z,
            ArcPoint[] points, int color, float width) {
        void draw(PoseStack.Pose pose, VertexConsumer vertices) {
            for (int i = 1; i < points.length; i++) {
                ArcPoint a = points[i - 1];
                ArcPoint b = points[i];
                float dx = b.x() - a.x();
                float dy = b.y() - a.y();
                float dz = b.z() - a.z();
                float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (length == 0) {
                    continue;
                }
                vertex(vertices, pose, a, dx / length, dy / length, dz / length);
                vertex(vertices, pose, b, dx / length, dy / length, dz / length);
            }
        }

        private void vertex(VertexConsumer vertices, PoseStack.Pose pose, ArcPoint point,
                float nx, float ny, float nz) {
            vertices.addVertex(pose, point.x(), point.y(), point.z())
                    .setColor(color)
                    .setNormal(pose, nx, ny, nz)
                    .setLineWidth(width);
        }
    }

    record Style(int region, int segments, float jitter, float lift,
            double maxLength, double connectionChance, double routedChance,
            int color, float width) {
        Style withColor(int updatedColor) {
            return new Style(region, segments, jitter, lift,
                    maxLength, connectionChance, routedChance,
                    updatedColor, width);
        }
    }

    static final Style GAMEPLAY_STYLE = new Style(2, 4, 0.015F, 0.02F, 1.5, 0.125, 0.5,
            0xFFFFFFFF, 1.0F);
}
