package io.github.omegaplex.ankerstone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.omegaplex.ankerstone.mixin.ServerLevelAccessor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DaylightDetectorBlock;

public class ChunkReleaseTests {
    @GameTest(environment = "ankerstone-test:chunk_release", maxTicks = 1400)
    public void singleDeactivationUnloads(GameTestHelper helper) {
        new Fixture(helper, 8192, AnkerstoneMode.SINGLE, false, Change.OFF).start();
    }

    @GameTest(environment = "ankerstone-test:chunk_release", maxTicks = 1400)
    public void wideDeactivationUnloads(GameTestHelper helper) {
        new Fixture(helper, 8256, AnkerstoneMode.WIDE, false, Change.OFF).start();
    }

    @GameTest(environment = "ankerstone-test:chunk_release", maxTicks = 1400)
    public void singleRemovalUnloads(GameTestHelper helper) {
        new Fixture(helper, 8320, AnkerstoneMode.SINGLE, false, Change.REMOVE).start();
    }

    @GameTest(environment = "ankerstone-test:chunk_release", maxTicks = 1400)
    public void wideRemovalUnloads(GameTestHelper helper) {
        new Fixture(helper, 8384, AnkerstoneMode.WIDE, false, Change.REMOVE).start();
    }

    @GameTest(environment = "ankerstone-test:chunk_release", maxTicks = 1400)
    public void wideDowngradeStopsOuterEntities(GameTestHelper helper) {
        new Fixture(helper, 8448, AnkerstoneMode.WIDE, false, Change.DOWNGRADE).start();
    }

    @GameTest(environment = "ankerstone-test:chunk_release", maxTicks = 1400)
    public void sharedSingleKeepsTickingUntilLastRemoval(GameTestHelper helper) {
        new Fixture(helper, 8512, AnkerstoneMode.SINGLE, false, Change.SHARED).start();
    }

    @GameTest(environment = "ankerstone-test:chunk_release", maxTicks = 1400)
    public void sharedWideKeepsTickingUntilLastRemoval(GameTestHelper helper) {
        new Fixture(helper, 8576, AnkerstoneMode.WIDE, false, Change.SHARED).start();
    }

    @GameTest(environment = "ankerstone-test:chunk_release", maxTicks = 1400)
    public void overlappingWideReleasesOnlyItsOwnFootprint(GameTestHelper helper) {
        new Fixture(helper, 8640, AnkerstoneMode.WIDE, false, Change.OVERLAP).start();
    }

    @GameTest(environment = "ankerstone-test:chunk_release", maxTicks = 1400)
    public void netherWideRemovalUnloads(GameTestHelper helper) {
        new Fixture(helper, 8704, AnkerstoneMode.WIDE, true, Change.REMOVE).start();
    }

    private enum Change { OFF, REMOVE, DOWNGRADE, SHARED, OVERLAP }

    private static final class Fixture {
        private final GameTestHelper helper;
        private final ServerLevel level;
        private final AnkerstoneMode mode;
        private final Change change;
        private final BlockPos first;
        private final BlockPos second;
        private final List<BlockPos> anchors;
        private final List<ChunkPos> chunks = new ArrayList<>();
        private final List<ArmorStand> probes = new ArrayList<>();
        private int phase;
        private long deadline;
        private long phaseStarted;
        private long wallDeadline;
        private int[] baseline;
        private int stableTicks;
        private int stoppedCount = -1;
        private boolean finished;
        private boolean startedTicking;

        Fixture(GameTestHelper helper, int offset, AnkerstoneMode mode,
                boolean nether, Change change) {
            this.helper = helper;
            this.level = nether ? helper.getLevel().getServer().getLevel(Level.NETHER) : helper.getLevel();
            this.mode = mode;
            this.change = change;
            BlockPos origin = helper.absolutePos(BlockPos.ZERO);
            first = new BlockPos((Math.floorDiv(origin.getX(), 16) + offset) * 16 + 8,
                    200, (Math.floorDiv(origin.getZ(), 16) + offset) * 16 + 8);
            second = first.east(change == Change.OVERLAP ? 32 : 3);
            anchors = change == Change.SHARED || change == Change.OVERLAP
                    ? List.of(first, second) : List.of(first);
        }

        void start() {
            try {
                ChunkPos center = ChunkPos.containing(first);
                for (int dx = -3; dx <= (change == Change.OVERLAP ? 5 : 3); dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        ChunkPos chunk = new ChunkPos(center.x() + dx, center.z() + dz);
                        helper.assertTrue(level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) == null,
                                "Release fixture must start unloaded: " + chunk);
                        chunks.add(chunk);
                        level.getChunk(chunk.x(), chunk.z());
                    }
                }
                for (BlockPos anchor : anchors) {
                    power(anchor, mode == AnkerstoneMode.WIDE ? 15 : 1);
                    level.setBlock(anchor, ModBlocks.ANKERSTONE.defaultBlockState(), Block.UPDATE_ALL);
                }
                if (change == Change.OVERLAP) {
                    probes.add(probe(first.west(16).above(2)));
                    probes.add(probe(first.east(16).above(2)));
                    probes.add(probe(second.east(16).above(2)));
                } else {
                    probes.add(probe(first.above(2)));
                    if (mode == AnkerstoneMode.WIDE) probes.add(probe(first.offset(16, 2, 16)));
                }
                var entities = ((ServerLevelAccessor) level).ankerstone$getEntityManager();
                long started = System.nanoTime();
                level.getServer().managedBlock(() -> {
                    entities.processPendingLoads();
                    return probes.stream().allMatch(p -> level.areEntitiesLoaded(
                            ChunkPos.containing(p.blockPosition()).pack()))
                            || System.nanoTime() - started >= TimeUnit.SECONDS.toNanos(10);
                });
                for (ArmorStand probe : probes) {
                    helper.assertTrue(level.areEntitiesLoaded(ChunkPos.containing(probe.blockPosition()).pack()),
                            "Probe entity data must finish loading");
                    helper.assertTrue(level.addFreshEntity(probe), "Probe must be added");
                }
                nextPhase(0);
                helper.onEachTick(this::tick);
            } catch (RuntimeException failure) {
                cleanup();
                throw failure;
            }
        }

        private void tick() {
            if (finished) return;
            try {
                // Other fixtures may still be preparing before this test gets its first tick.
                if (!startedTicking) {
                    startedTicking = true;
                    wallDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                }
                helper.assertTrue(helper.getTick() < deadline && System.nanoTime() < wallDeadline,
                        "Release phase " + phase + " timed out: " + diagnostics());
                if (phase == 0) {
                    if (!probes.stream().allMatch(p -> p.tickCount >= 10)) return;
                    baseline = counts();
                    if (change == Change.OFF) power(first, 0);
                    else if (change == Change.DOWNGRADE) power(first, 1);
                    else level.setBlock(first, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    if (change != Change.DOWNGRADE) {
                        helper.assertFalse(OwnerData.get(level).getOwnerModes().containsKey(first),
                                "Released anchor must not remain a saved owner");
                    }
                    nextPhase(change == Change.DOWNGRADE || change == Change.SHARED
                            || change == Change.OVERLAP ? 1 : 2);
                } else if (phase == 1) {
                    boolean remainsActive;
                    if (change == Change.SHARED) {
                        remainsActive = probes.stream().allMatch(p -> p.tickCount - baseline[probes.indexOf(p)] >= 20);
                        helper.assertTrue(OwnerData.get(level).getOwnerModes().get(second) == mode,
                                "The surviving shared owner must remain saved");
                    } else {
                        int stopped = change == Change.OVERLAP ? 0 : 1;
                        boolean noLongerTicking = !level.isPositionEntityTicking(probes.get(stopped).blockPosition());
                        int count = probes.get(stopped).tickCount;
                        stableTicks = noLongerTicking && count == stoppedCount ? stableTicks + 1 : 0;
                        stoppedCount = count;
                        remainsActive = stableTicks >= 20
                                && probes.get(change == Change.OVERLAP ? 1 : 0).tickCount
                                        - baseline[change == Change.OVERLAP ? 1 : 0] >= 20;
                        if (change == Change.OVERLAP) {
                            remainsActive &= probes.get(2).tickCount - baseline[2] >= 20;
                        } else {
                            helper.assertTrue(OwnerData.get(level).getOwnerModes().get(first) == AnkerstoneMode.SINGLE,
                                    "Downgrade must preserve the Single owner");
                        }
                    }
                    if (!remainsActive) return;
                    BlockPos last = change == Change.DOWNGRADE ? first : second;
                    level.setBlock(last, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    nextPhase(2);
                } else if (phase == 2) {
                    for (BlockPos anchor : anchors) {
                        helper.assertFalse(OwnerData.get(level).getOwnerModes().containsKey(anchor),
                                "Last removal must clear every saved owner");
                        helper.assertTrue(ticketCount(anchor) == 0,
                                "Last release must remove Ankerstone tickets");
                    }
                    // Neither of these queries requests or reloads a chunk.
                    if (chunks.stream().anyMatch(c -> level.getChunkSource().getChunkNow(c.x(), c.z()) != null
                            || level.areEntitiesLoaded(c.pack()))) return;
                    helper.assertTrue(probes.stream().noneMatch(p -> level.isPositionEntityTicking(p.blockPosition())),
                            "Unloaded probes must not be entity-ticking");
                    baseline = counts();
                    nextPhase(3);
                } else {
                    if (helper.getTick() - phaseStarted < 20) return;
                    helper.assertTrue(java.util.Arrays.equals(baseline, counts()),
                            "Released probe tick counts must stay unchanged");
                    finished = true;
                    cleanup();
                    helper.succeed();
                }
            } catch (RuntimeException failure) {
                finished = true;
                cleanup();
                throw failure;
            }
        }

        private void nextPhase(int phase) {
            this.phase = phase;
            phaseStarted = helper.getTick();
            deadline = helper.getTick() + 400;
            wallDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        }

        private int[] counts() {
            return probes.stream().mapToInt(p -> p.tickCount).toArray();
        }

        private long ticketCount(BlockPos pos) {
            TicketStorage storage = level.getChunkSource().getDataStorage().computeIfAbsent(TicketStorage.TYPE);
            return storage.getTickets(ChunkPos.containing(pos).pack()).stream()
                    .filter(t -> t.getType() == ChunkTickets.ANKERSTONE_TICKET).count();
        }

        private String diagnostics() {
            return anchors.stream().map(a -> {
                ChunkPos c = ChunkPos.containing(a);
                return c + " loaded=" + (level.getChunkSource().getChunkNow(c.x(), c.z()) != null)
                        + ", entityTicking=" + level.isPositionEntityTicking(a)
                        + ", tickets=" + ticketCount(a);
            }).toList().toString();
        }

        private ArmorStand probe(BlockPos pos) {
            ArmorStand probe = new ArmorStand(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) {
                @Override
                public boolean shouldBeSaved() { return false; }
            };
            probe.setNoGravity(true);
            return probe;
        }

        private void power(BlockPos anchor, int strength) {
            level.setBlock(anchor.below(), Blocks.DAYLIGHT_DETECTOR.defaultBlockState()
                    .setValue(DaylightDetectorBlock.POWER, strength), Block.UPDATE_ALL);
            level.removeBlockEntity(anchor.below());
        }

        private void cleanup() {
            probes.forEach(ArmorStand::discard);
            for (BlockPos anchor : anchors) {
                ChunkPos chunk = ChunkPos.containing(anchor);
                if (level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) != null) {
                    level.setBlock(anchor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(anchor.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                // Cleanup may remove tickets directly, but only after the assertions.
                for (AnkerstoneMode active : List.of(AnkerstoneMode.SINGLE, AnkerstoneMode.WIDE)) {
                    ChunkTickets.remove(level, anchor, active);
                    level.getChunkSource().removeTicketWithRadius(ChunkTickets.ANKERSTONE_TICKET,
                            chunk, active == AnkerstoneMode.WIDE ? 3 : 2);
                }
            }
        }
    }
}
