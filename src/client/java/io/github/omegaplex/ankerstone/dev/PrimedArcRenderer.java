package io.github.omegaplex.ankerstone.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.Random;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.omegaplex.ankerstone.PrimedAnkerstoneBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class PrimedArcRenderer implements
        BlockEntityRenderer<PrimedAnkerstoneBlockEntity, PrimedArcRenderer.State> {

    private final WeakHashMap<PrimedAnkerstoneBlockEntity, Playback> instances = new WeakHashMap<>();

    private static final class Playback {
        final ArcInstance instance;
        int appliedTick = -1;
        long elapsedNanos;

        Playback(long seed) {
            instance = new ArcInstance(
                    seed, 0, ArcIntensity.at(0, 300, 40, 500), 300);
        }
    }

    public static final class State extends BlockEntityRenderState {
        final BlockModelRenderState model = new BlockModelRenderState();
        List<ArcRenderer.DrawPath> paths = List.of();
        float shakeX;
        float shakeY;
        float shakeZ;
        float modelScale = 1;
        float rayProgress;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            PrimedAnkerstoneBlockEntity entity, State state,
            float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        models.update(state.model, entity.getBlockState(), DISPLAY_CONTEXT);
        state.paths = List.of();
        state.shakeX = 0;
        state.shakeY = 0;
        state.shakeZ = 0;
        double collapse = Math.clamp(
                (entity.visualFinaleTicks(partialTick)
                        - PrimedAnkerstoneBlockEntity.EXPANSION_TICKS
                        - PrimedAnkerstoneBlockEntity.HOLD_TICKS)
                        / PrimedAnkerstoneBlockEntity.COLLAPSE_TICKS,
                0, 1);
        state.modelScale = (float) (1 - collapse * collapse);
        state.rayProgress = (float) collapse;
        if (entity.remainingTicks() == 0) {
            return;
        }

        var playback = instances.computeIfAbsent(entity,
                key -> new Playback(key.getBlockPos().asLong()));
        long elapsed = (long) (entity.visualElapsedTicks(partialTick)
                * 50_000_000.0);
        playback.elapsedNanos = Math.max(playback.elapsedNanos, elapsed);
        int countdownTick = (int) Math.min(
                PrimedAnkerstoneBlockEntity.COUNTDOWN_TICKS,
                playback.elapsedNanos / 50_000_000L);

        // Apply missed countdown transitions at their original timestamps.
        while (playback.appliedTick < countdownTick) {
            playback.appliedTick++;
            var scaled = ArcIntensity.at(
                    intensityAtTick(playback.appliedTick), 300, 40, 500);
            var intensity = new ArcIntensity(
                    scaled.selectionMillis(),
                    scaled.refreshMillis(),
                    criticalAtTick(playback.appliedTick) > 0
                            ? 0
                            : scaled.quietMillis());
            playback.instance.changeTimingAt(
                    playback.appliedTick * 50_000_000L, intensity, 300);
        }

        double elapsedTicks = playback.elapsedNanos / 50_000_000.0;
        double progress = intensityAtTick(elapsedTicks);
        double tint = criticalAtTick(elapsedTicks);
        int red = (int) Math.round(221 + (208 - 221) * tint);
        int green = (int) Math.round(238 + (176 - 238) * tint);
        int color = 0xFF000000 | red << 16 | green << 8 | 255;
        var style = ArcRenderer.GAMEPLAY_STYLE.withColor(color);
        double critical = criticalAtTick(elapsedTicks);
        double amplitude = 0.035 * critical * critical;
        double phase = (entity.getBlockPos().asLong() & 1023) * 0.1;
        state.shakeX = (float) (Math.sin(elapsedTicks * 2.3 + phase)
                * amplitude);
        state.shakeY = (float) (Math.sin(elapsedTicks * 3.1 + phase)
                * amplitude * 0.5);
        state.shakeZ = (float) (Math.sin(elapsedTicks * 2.7 + phase)
                * amplitude);
        int baseline = ArcInstance.slotCount(Math.min(1, progress), 4);
        int count = baseline + (int) Math.round((8 - baseline) * critical);
        var paths = new ArrayList<ArcRenderer.DrawPath>(count);
        for (int index = 0; index < count; index++) {
            var path = ArcRenderer.extractPath(
                    playback.instance, playback.instance.slot(index),
                    playback.elapsedNanos, 0, 0, 0, style);
            if (path != null) {
                paths.add(path);
            }
        }
        state.paths = List.copyOf(paths);
    }

    @Override
    public void submit(State state, PoseStack poses,
            SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.modelScale <= 0) {
            return;
        }
        poses.pushPose();
        poses.translate(state.shakeX, state.shakeY, state.shakeZ);
        poses.translate(0.5, 0.5, 0.5);
        poses.scale(state.modelScale, state.modelScale, state.modelScale);
        poses.translate(-0.5, -0.5, -0.5);
        state.model.submit(
                poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        for (var path : state.paths) {
            collector.submitCustomGeometry(
                    poses, RenderTypes.linesTranslucent(), path::draw);
        }
        poses.popPose();
        if (state.rayProgress > 0 && state.rayProgress < 1) {
            poses.pushPose();
            poses.translate(0.5, 0.5, 0.5);
            float progress = state.rayProgress;
            long seed = state.blockPos.asLong();
            collector.submitCustomGeometry(poses, RenderTypes.dragonRays(),
                    (pose, vertices) -> drawRays(pose, vertices, progress, seed));
            poses.popPose();
        }
    }

    private static void drawRays(PoseStack.Pose pose, VertexConsumer vertices,
            float progress, long seed) {
        var random = new Random(seed);
        float strength = (float) Math.sin(Math.PI * progress);
        int centerColor = Math.round(180 * strength) << 24 | 0xFFFFFF;
        for (int ray = 0; ray < 12; ray++) {
            var rotation = new Quaternionf().rotationXYZ(
                    random.nextFloat() * (float) (Math.PI * 2),
                    random.nextFloat() * (float) (Math.PI * 2) + progress * 0.5F,
                    random.nextFloat() * (float) (Math.PI * 2));
            float length = (1.2F + random.nextFloat() * 0.8F)
                    * (0.5F + 0.5F * strength);
            float width = 0.15F + random.nextFloat() * 0.15F;
            var tips = new Vector3f[] {
                    new Vector3f(-0.866F * width, length, -0.5F * width).rotate(rotation),
                    new Vector3f(0.866F * width, length, -0.5F * width).rotate(rotation),
                    new Vector3f(0, length, width).rotate(rotation)
            };
            for (int face = 0; face < 3; face++) {
                vertices.addVertex(pose, 0, 0, 0).setColor(centerColor);
                vertices.addVertex(pose, tips[face]).setColor(0x00CC88FF);
                vertices.addVertex(pose, tips[(face + 1) % 3]).setColor(0x00CC88FF);
            }
        }
    }

    private static double intensityAtTick(double tick) {
        if (tick <= 60) {
            return 0.2 * Math.clamp(tick / 60.0, 0, 1);
        }
        return 0.2 + 0.8 * Math.clamp(
                (tick - 60)
                        / (PrimedAnkerstoneBlockEntity.COUNTDOWN_TICKS - 60.0),
                0, 1);
    }

    private static double criticalAtTick(double tick) {
        return Math.clamp(
                (tick - (PrimedAnkerstoneBlockEntity.COUNTDOWN_TICKS - 60))
                        / 40.0,
                0, 1);
    }

    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver models;

    public PrimedArcRenderer(BlockModelResolver models) {
        this.models = models;
    }
}
