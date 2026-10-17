package io.github.omegaplex.ankerstone;

import static io.github.omegaplex.ankerstone.PistonTestSupport.assertOffAnkerstoneCanBePulled;
import static io.github.omegaplex.ankerstone.PistonTestSupport.assertOffAnkerstoneCanBePushed;
import static io.github.omegaplex.ankerstone.PistonTestSupport.assertPistonChunksReady;
import static io.github.omegaplex.ankerstone.PistonTestSupport.clearOffPistonFixture;
import static io.github.omegaplex.ankerstone.PistonTestSupport.loadNegativeBoundaryChunks;
import static io.github.omegaplex.ankerstone.PistonTestSupport.pistonBeforeChunkBoundary;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;

public class PistonBoundaryTests {
    @GameTest(environment = "ankerstone-test:off_movement", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void offPushCrossesXChunkBoundary(GameTestHelper helper) {
        assertOffAnkerstoneCanBePushed(helper, Direction.EAST,
                pistonBeforeChunkBoundary(helper, Direction.EAST));
    }

    @GameTest(environment = "ankerstone-test:off_movement", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void offPushCrossesZChunkBoundary(GameTestHelper helper) {
        assertOffAnkerstoneCanBePushed(helper, Direction.SOUTH,
                pistonBeforeChunkBoundary(helper, Direction.SOUTH));
    }

    @GameTest(environment = "ankerstone-test:off_movement", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void stickyRoundTripCrossesXChunkBoundary(GameTestHelper helper) {
        assertOffAnkerstoneCanBePulled(helper, Direction.EAST,
                pistonBeforeChunkBoundary(helper, Direction.EAST));
    }

    @GameTest(environment = "ankerstone-test:off_movement", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void stickyRoundTripCrossesZChunkBoundary(GameTestHelper helper) {
        assertOffAnkerstoneCanBePulled(helper, Direction.SOUTH,
                pistonBeforeChunkBoundary(helper, Direction.SOUTH));
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 200)
    public void offPushCrossesNegativeXChunkBoundary(GameTestHelper helper) {
        assertNegativeBoundaryMove(helper,
                new BlockPos(-18, 100, -1024), Direction.EAST, false);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 200)
    public void offPushCrossesNegativeZChunkBoundary(GameTestHelper helper) {
        assertNegativeBoundaryMove(helper,
                new BlockPos(-1024, 100, -18), Direction.SOUTH, false);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 200)
    public void stickyRoundTripCrossesNegativeXChunkBoundary(GameTestHelper helper) {
        assertNegativeBoundaryMove(helper,
                new BlockPos(-18, 100, -2048), Direction.EAST, true);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 200)
    public void stickyRoundTripCrossesNegativeZChunkBoundary(GameTestHelper helper) {
        assertNegativeBoundaryMove(helper,
                new BlockPos(-2048, 100, -18), Direction.SOUTH, true);
    }

    private void assertNegativeBoundaryMove(GameTestHelper helper,
            BlockPos worldPiston, Direction direction, boolean sticky) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BlockPos piston = worldPiston.subtract(origin);
        ChunkPos startChunk = ChunkPos.containing(worldPiston.relative(direction));
        ChunkPos destinationChunk = ChunkPos.containing(worldPiston.relative(direction, 2));
        helper.assertTrue(startChunk.x() < 0 && startChunk.z() < 0
                && destinationChunk.x() < 0 && destinationChunk.z() < 0,
                "Both chunks must have negative coordinates");
        helper.assertFalse(startChunk.equals(destinationChunk),
                "The negative-coordinate fixture must cross a chunk boundary");
        try {
            loadNegativeBoundaryChunks(helper, piston, direction);
            helper.runAtTickTime(160, () -> {
                try {
                    assertPistonChunksReady(helper, worldPiston, direction);
                } catch (RuntimeException failure) {
                    clearOffPistonFixture(helper, piston, direction);
                    throw failure;
                }
            });
            helper.startSequence()
                    .thenWaitUntil(() -> assertPistonChunksReady(helper, worldPiston, direction))
                    .thenExecute(() -> {
                        try {
                            if (sticky) {
                                assertOffAnkerstoneCanBePulled(helper, direction, piston);
                            } else {
                                assertOffAnkerstoneCanBePushed(helper, direction, piston);
                            }
                        } catch (RuntimeException failure) {
                            clearOffPistonFixture(helper, piston, direction);
                            throw failure;
                        }
                    });
        } catch (RuntimeException failure) {
            clearOffPistonFixture(helper, piston, direction);
            throw failure;
        }
    }

}
