package io.github.omegaplex.ankerstone;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class ChunkTickingTests {
    private static final TicketType LOAD_ONLY_TICKET = new TicketType(
            240, TicketType.FLAG_LOADING);

    @GameTest(environment = "ankerstone-test:entity_ticking", maxTicks = 200)
    public void singleTicksOnlyCenter(GameTestHelper helper) {
        assertFootprint(helper, helper.getLevel(), AnkerstoneMode.SINGLE, 1024);
    }

    @GameTest(environment = "ankerstone-test:entity_ticking", maxTicks = 200)
    public void wideTicksThreeByThree(GameTestHelper helper) {
        assertFootprint(helper, helper.getLevel(), AnkerstoneMode.WIDE, 2048);
    }

    @GameTest(environment = "ankerstone-test:entity_ticking", maxTicks = 200)
    public void offDoesNotTickEntities(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = remoteAnchor(helper, 3072);
        ArmorStand probe = newProbe(level, anchor.above(2));
        ArmorStand control = newProbe(level,
                helper.absolutePos(new BlockPos(2, 3, 2)));
        List<ArmorStand> probes = List.of(probe, control);
        boolean[] added = { false };
        boolean[] finished = { false };

        try {
            helper.assertFalse(level.isPositionEntityTicking(anchor),
                    "Remote fixture must not already be entity-ticking");
            prepareTerrain(level, anchor);
            level.getChunkSource().addTicketWithRadius(
                    LOAD_ONLY_TICKET, ChunkPos.containing(anchor), 2);
            level.setBlock(anchor, ModBlocks.ANKERSTONE.defaultBlockState(),
                    Block.UPDATE_ALL);

            helper.onEachTick(() -> {
                if (finished[0]) {
                    return;
                }
                try {
                    if (!added[0]) {
                        boolean ready = probes.stream().allMatch(
                                entity -> level.areEntitiesLoaded(
                                        ChunkPos.containing(entity.blockPosition()).pack()));
                        if (!ready && helper.getTick() < 160) {
                            return;
                        }
                        helper.assertTrue(ready,
                                "Off fixture must finish loading entity data");
                        for (ArmorStand entity : probes) {
                            helper.assertTrue(level.addFreshEntity(entity),
                                    "Probe must be added at " + entity.blockPosition());
                        }
                        added[0] = true;
                        return;
                    }

                    if (control.tickCount < 10 && helper.getTick() < 180) {
                        return;
                    }
                    finished[0] = true;
                    helper.assertTrue(control.tickCount >= 10,
                            "Control probe must tick at least ten times");
                    helper.assertTrue(level.getEntity(probe.getUUID()) == probe,
                            "Off probe must remain tracked during measurement");
                    helper.assertTrue(probe.tickCount == 0,
                            "Off must not tick its probe; got " + probe.tickCount);
                    helper.assertTrue(level.getBlockState(anchor)
                            .getValue(AnkerstoneBlock.MODE) == AnkerstoneMode.OFF,
                            "Anchor must remain Off during measurement");
                    clearFixture(level, anchor, probes);
                    helper.succeed();
                } catch (RuntimeException failure) {
                    finished[0] = true;
                    clearFixture(level, anchor, probes);
                    throw failure;
                }
            });
        } catch (RuntimeException failure) {
            clearFixture(level, anchor, probes);
            throw failure;
        }
    }

    @GameTest(environment = "ankerstone-test:entity_ticking", maxTicks = 200)
    public void wideTicksInNether(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Nether must be available");
        assertFootprint(helper, nether, AnkerstoneMode.WIDE, 4096);
    }

    private void assertFootprint(
            GameTestHelper helper, ServerLevel level,
            AnkerstoneMode mode, int chunkOffset) {
        boolean wide = mode == AnkerstoneMode.WIDE;
        BlockPos anchor = remoteAnchor(helper, chunkOffset);
        ArmorStand probe = newProbe(level, anchor.above(2));
        List<ArmorStand> neighbors = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    neighbors.add(newProbe(level,
                            anchor.offset(dx * 16, 2, dz * 16)));
                }
            }
        }
        List<ArmorStand> outside = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    outside.add(newProbe(level,
                            anchor.offset(dx * 16, 2, dz * 16)));
                }
            }
        }
        List<ArmorStand> probes = new ArrayList<>(neighbors);
        probes.add(probe);
        probes.addAll(outside);
        boolean[] added = { false };
        boolean[] finished = { false };

        try {
            helper.assertFalse(level.isPositionEntityTicking(anchor),
                    "Remote fixture must not already be entity-ticking");
            prepareTerrain(level, anchor);
            setAnchorPower(level, anchor, wide ? 15 : 1);
            level.setBlock(anchor, ModBlocks.ANKERSTONE.defaultBlockState(),
                    Block.UPDATE_ALL);
            helper.assertTrue(level.getBlockState(anchor)
                    .getValue(AnkerstoneBlock.MODE) == mode,
                    "Fixture must activate in " + mode);

            helper.onEachTick(() -> {
                if (finished[0]) {
                    return;
                }

                try {
                    if (!added[0]) {
                        boolean entitiesLoaded = probes.stream().allMatch(
                                entity -> level.areEntitiesLoaded(
                                        ChunkPos.containing(entity.blockPosition()).pack()));
                        boolean entityTicking = level.isPositionEntityTicking(anchor);
                        boolean ready = entitiesLoaded && entityTicking;
                        if (!ready && helper.getTick() < 160) {
                            return;
                        }

                        helper.assertTrue(ready,
                                mode + " fixture not ready: entitiesLoaded=" + entitiesLoaded
                                        + ", entityTicking=" + entityTicking);
                        for (ArmorStand entity : probes) {
                            helper.assertTrue(level.addFreshEntity(entity),
                                    "Probe must be added at " + entity.blockPosition());
                        }
                        added[0] = true;
                        return;
                    }

                    boolean observed = probe.tickCount >= 5
                            && (!wide || neighbors.stream().allMatch(
                                    neighbor -> neighbor.tickCount >= 5));
                    if (!observed && helper.getTick() < 180) {
                        return;
                    }

                    finished[0] = true;
                    helper.assertTrue(level.getBlockState(anchor)
                            .getValue(AnkerstoneBlock.MODE) == mode,
                            "Anchor must remain in " + mode + " during measurement");
                    helper.assertFalse(probe.isRemoved(),
                            "Probe must remain present during measurement");
                    helper.assertTrue(probe.tickCount >= 5,
                            "Center must tick the probe at least five times; got "
                                    + probe.tickCount);
                    for (ArmorStand neighbor : neighbors) {
                        ChunkPos chunk = ChunkPos.containing(neighbor.blockPosition());
                        helper.assertTrue(level.getEntity(neighbor.getUUID()) == neighbor,
                                "Neighbor probe must remain tracked in " + chunk);
                        helper.assertTrue(
                                wide ? neighbor.tickCount >= 5 : neighbor.tickCount == 0,
                                mode + " unexpected ticks in neighboring chunk " + chunk
                                        + ": " + neighbor.tickCount);
                    }
                    for (ArmorStand entity : outside) {
                        ChunkPos chunk = ChunkPos.containing(entity.blockPosition());
                        helper.assertTrue(level.getEntity(entity.getUUID()) == entity,
                                "Outside probe must remain tracked in " + chunk);
                        helper.assertTrue(entity.tickCount == 0,
                                mode + " must not tick outside chunk " + chunk
                                        + "; got " + entity.tickCount + " ticks");
                    }
                    clearFixture(level, anchor, probes);
                    helper.succeed();
                } catch (RuntimeException failure) {
                    finished[0] = true;
                    clearFixture(level, anchor, probes);
                    throw failure;
                }
            });
        } catch (RuntimeException failure) {
            clearFixture(level, anchor, probes);
            throw failure;
        }
    }

    private void prepareTerrain(ServerLevel level, BlockPos anchor) {
        ChunkPos center = ChunkPos.containing(anchor);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.getChunk(center.x() + dx, center.z() + dz);
            }
        }
    }

    private BlockPos remoteAnchor(GameTestHelper helper, int chunkOffset) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return new BlockPos(
                (Math.floorDiv(origin.getX(), 16) + chunkOffset) * 16 + 8,
                200,
                (Math.floorDiv(origin.getZ(), 16) + chunkOffset) * 16 + 8);
    }

    private void setAnchorPower(ServerLevel level, BlockPos anchor, int power) {
        level.setBlock(anchor.below(), Blocks.DAYLIGHT_DETECTOR.defaultBlockState()
                .setValue(DaylightDetectorBlock.POWER, power), Block.UPDATE_ALL);
        level.removeBlockEntity(anchor.below());
    }

    private ArmorStand newProbe(ServerLevel level, BlockPos position) {
        ArmorStand probe = new ArmorStand(level,
                position.getX() + 0.5, position.getY(), position.getZ() + 0.5) {
            @Override
            public boolean shouldBeSaved() {
                return false;
            }
        };
        probe.setNoGravity(true);
        return probe;
    }

    private void clearFixture(
            ServerLevel level, BlockPos anchor, List<ArmorStand> probes) {
        try {
            probes.forEach(ArmorStand::discard);
            level.setBlock(anchor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(anchor.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        } finally {
            level.getChunkSource().removeTicketWithRadius(
                    LOAD_ONLY_TICKET, ChunkPos.containing(anchor), 2);
        }
    }
}
