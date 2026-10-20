package io.github.omegaplex.ankerstone;

import static io.github.omegaplex.ankerstone.PistonTestSupport.assertAnkerstoneTicketLevel;
import static io.github.omegaplex.ankerstone.PistonTestSupport.assertNoActiveOwnersOrTickets;
import static io.github.omegaplex.ankerstone.PistonTestSupport.assertNoAnkerstoneTickets;
import static io.github.omegaplex.ankerstone.PistonTestSupport.assertPistonChunksReady;
import static io.github.omegaplex.ankerstone.PistonTestSupport.clearPistonFixture;
import static io.github.omegaplex.ankerstone.PistonTestSupport.loadNegativeBoundaryChunks;
import static io.github.omegaplex.ankerstone.PistonTestSupport.pistonBeforeChunkBoundary;

import java.util.UUID;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

public class PistonActivationTests {
    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void movedAnkerstoneActivatesSingle(GameTestHelper helper) {
        assertMovedAnkerstoneActivates(helper, AnkerstoneMode.SINGLE);
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void movedAnkerstoneActivatesWide(GameTestHelper helper) {
        assertMovedAnkerstoneActivates(helper, AnkerstoneMode.WIDE);
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void crossingChunkBoundaryActivatesSingle(GameTestHelper helper) {
        assertMovedAnkerstoneActivates(helper, AnkerstoneMode.SINGLE,
                pistonBeforeChunkBoundary(helper, Direction.EAST));
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void crossingChunkBoundaryActivatesWide(GameTestHelper helper) {
        assertMovedAnkerstoneActivates(helper, AnkerstoneMode.WIDE,
                pistonBeforeChunkBoundary(helper, Direction.EAST));
    }

    @GameTest(environment = "ankerstone-test:negative_boundaries", maxTicks = 200)
    public void negativeBoundaryArrivalActivatesSingle(GameTestHelper helper) {
        assertNegativeBoundaryActivation(helper,
                new BlockPos(-18, 100, -3072), AnkerstoneMode.SINGLE);
    }

    @GameTest(environment = "ankerstone-test:negative_boundaries", maxTicks = 200)
    public void negativeBoundaryArrivalActivatesWide(GameTestHelper helper) {
        assertNegativeBoundaryActivation(helper,
                new BlockPos(-18, 100, -4096), AnkerstoneMode.WIDE);
    }

    private void assertMovedAnkerstoneActivates(GameTestHelper helper,
            AnkerstoneMode mode) {
        assertMovedAnkerstoneActivates(helper, mode, new BlockPos(2, 2, 2));
    }

    private void assertMovedAnkerstoneActivates(GameTestHelper helper,
            AnkerstoneMode mode, BlockPos piston) {

        BlockPos start = piston.east();
        BlockPos destination = start.east();
        OwnerData savedData = OwnerData.get(helper.getLevel());
        AttributionData attribution = AttributionData.get(helper.getLevel());
        GlobalPos oldPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(start));
        GlobalPos newPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(destination));
        UUID placer = UUID.randomUUID();
        int wireLength = mode == AnkerstoneMode.SINGLE ? 2 : 1;
        try {
            helper.setBlock(piston, Blocks.PISTON.defaultBlockState()
                    .setValue(PistonBaseBlock.FACING, Direction.EAST));
            for (int distance = 1; distance <= wireLength; distance++) {
                BlockPos wire = destination.east(distance);
                helper.setBlock(wire.below(), Blocks.GLASS);
                helper.setBlock(wire, Blocks.REDSTONE_WIRE);
            }
            helper.setBlock(destination.east(wireLength + 1), Blocks.REDSTONE_BLOCK);
            helper.setBlock(start, ModBlocks.ANKERSTONE);
            attribution.setPlacer(oldPosition, placer);
            helper.assertBlockProperty(start, AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
            helper.assertBlockPresent(Blocks.AIR, destination);
            assertNoActiveOwnersOrTickets(helper, start, destination);
            helper.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);
            long movementDeadline = helper.getTick() + 20;
            boolean[] finished = { false };

            helper.onEachTick(() -> {
                if (finished[0]) {
                    return;
                }
                boolean movementFinished = helper.getBlockState(start).is(Blocks.PISTON_HEAD)
                        && helper.getBlockState(destination).is(ModBlocks.ANKERSTONE);

                if (!movementFinished && helper.getTick() < movementDeadline) {
                    return;
                }

                finished[0] = true;
                try {
                    helper.assertBlockPresent(Blocks.PISTON, piston);
                    helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, true);
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, start);
                    helper.assertBlockPresent(ModBlocks.ANKERSTONE, destination);
                    helper.assertBlockProperty(destination, AnkerstoneBlock.MODE, mode);
                    helper.assertTrue(savedData.getOwnerModes().get(
                            helper.absolutePos(destination)) == mode,
                            "Destination must register the expected owner mode");
                    assertAnkerstoneTicketLevel(helper, destination,
                            mode == AnkerstoneMode.WIDE ? 30 : 31);
                    if (!ChunkPos.containing(oldPosition.pos()).equals(
                            ChunkPos.containing(newPosition.pos()))) {
                        assertNoAnkerstoneTickets(helper, start);
                    }
                    helper.assertFalse(savedData.getOwnerModes().containsKey(
                            helper.absolutePos(start)),
                            "Old position must not retain an owner");
                    helper.assertTrue(attribution.hasPendingCredit(placer),
                            "Activation after movement must queue credit for the original placer");
                    helper.assertTrue(attribution.getPlacer(oldPosition) == null,
                            "Movement must clear the old placer record");
                    helper.assertTrue(attribution.getPlacer(newPosition) == null,
                            "Queued activation credit no longer needs a placement record");
                } finally {
                    clearPistonFixture(helper, piston);
                    attribution.removePlacer(oldPosition);
                    attribution.removePlacer(newPosition);
                    attribution.removePendingCredit(placer);
                }
                assertNoActiveOwnersOrTickets(helper, start, destination);
                helper.succeed();
            });
        } catch (RuntimeException failure) {
            clearPistonFixture(helper, piston);
            attribution.removePlacer(oldPosition);
            attribution.removePlacer(newPosition);
            attribution.removePendingCredit(placer);
            throw failure;
        }
    }

    private void assertNegativeBoundaryActivation(GameTestHelper helper,
            BlockPos worldPiston, AnkerstoneMode mode) {
        BlockPos piston = worldPiston.subtract(helper.absolutePos(BlockPos.ZERO));
        ChunkPos start = ChunkPos.containing(worldPiston.east());
        ChunkPos destination = ChunkPos.containing(worldPiston.east(2));
        helper.assertTrue(start.x() < 0 && start.z() < 0
                && destination.x() < 0 && destination.z() < 0,
                "Both chunks must have negative coordinates");
        helper.assertFalse(start.equals(destination),
                "Powered arrival must cross a negative chunk boundary");

        try {
            loadNegativeBoundaryChunks(helper, piston, Direction.EAST);
            helper.runAtTickTime(160, () -> {
                try {
                    assertPistonChunksReady(helper, worldPiston, Direction.EAST);
                } catch (RuntimeException failure) {
                    clearPistonFixture(helper, piston);
                    throw failure;
                }
            });
            helper.startSequence()
                    .thenWaitUntil(() -> assertPistonChunksReady(helper, worldPiston, Direction.EAST))
                    .thenExecute(() -> {
                        try {
                            assertMovedAnkerstoneActivates(helper, mode, piston);
                        } catch (RuntimeException failure) {
                            clearPistonFixture(helper, piston);
                            throw failure;
                        }
                    });
        } catch (RuntimeException failure) {
            clearPistonFixture(helper, piston);
            throw failure;
        }
    }

}
