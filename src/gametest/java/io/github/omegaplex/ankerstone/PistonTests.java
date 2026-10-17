package io.github.omegaplex.ankerstone;

import static io.github.omegaplex.ankerstone.PistonTestSupport.assertOffAnkerstoneCanBePulled;
import static io.github.omegaplex.ankerstone.PistonTestSupport.assertOffAnkerstoneCanBePushed;
import static io.github.omegaplex.ankerstone.PistonTestSupport.clearPistonFixture;

import java.util.UUID;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class PistonTests {

    @GameTest
    public void pistonReactionMatchesMode(GameTestHelper helper) {
        for (AnkerstoneMode mode : AnkerstoneMode.values()) {
            BlockState state = ModBlocks.ANKERSTONE.defaultBlockState()
                    .setValue(AnkerstoneBlock.MODE, mode);
            PushReaction expected = mode == AnkerstoneMode.OFF
                    ? PushReaction.NORMAL
                    : PushReaction.BLOCK;
            PushReaction actual = state.getPistonPushReaction();

            helper.assertTrue(actual == expected,
                    "Expected " + expected + " for " + mode + ", got " + actual);
        }
        helper.succeed();
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void singleAnkerstoneCannotBePushed(GameTestHelper helper) {
        assertActiveAnkerstoneCannotBePushed(helper, AnkerstoneMode.SINGLE);
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void wideAnkerstoneCannotBePushed(GameTestHelper helper) {
        assertActiveAnkerstoneCannotBePushed(helper, AnkerstoneMode.WIDE);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 40)
    public void offAnkerstoneCanBePulled(GameTestHelper helper) {
        assertOffAnkerstoneCanBePulled(helper, Direction.EAST);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 40)
    public void offAnkerstoneCanBePulledWest(GameTestHelper helper) {
        assertOffAnkerstoneCanBePulled(helper, Direction.WEST);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 40)
    public void offAnkerstoneCanBePulledNorth(GameTestHelper helper) {
        assertOffAnkerstoneCanBePulled(helper, Direction.NORTH);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 40)
    public void offAnkerstoneCanBePulledSouth(GameTestHelper helper) {
        assertOffAnkerstoneCanBePulled(helper, Direction.SOUTH);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 40)
    public void offAnkerstoneCanBePulledUp(GameTestHelper helper) {
        assertOffAnkerstoneCanBePulled(helper, Direction.UP);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 40)
    public void offAnkerstoneCanBePulledDown(GameTestHelper helper) {
        assertOffAnkerstoneCanBePulled(helper, Direction.DOWN);
    }

    @GameTest(maxTicks = 40)
    public void adjacentPushKeepsBothPlacers(GameTestHelper helper) {
        BlockPos piston = new BlockPos(2, 2, 2);
        BlockPos first = piston.east();
        BlockPos second = piston.east(2);
        BlockPos destination = piston.east(3);
        GlobalPos firstPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(first));
        GlobalPos secondPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(second));
        GlobalPos destinationPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(destination));
        UUID firstPlacer = UUID.randomUUID();
        UUID secondPlacer = UUID.randomUUID();
        AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
        try {
            helper.setBlock(piston, Blocks.PISTON.defaultBlockState()
                    .setValue(PistonBaseBlock.FACING, Direction.EAST));
            helper.setBlock(first, ModBlocks.ANKERSTONE);
            helper.setBlock(second, ModBlocks.ANKERSTONE);
            data.setPlacer(firstPosition, firstPlacer);
            data.setPlacer(secondPosition, secondPlacer);

            helper.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);

            long movementDeadline = helper.getTick() + 20;
            boolean[] finished = { false };

            helper.onEachTick(() -> {
                if (finished[0]) {
                    return;
                }
                boolean movementFinished = helper.getBlockState(first).is(Blocks.PISTON_HEAD)
                        && helper.getBlockState(second).is(ModBlocks.ANKERSTONE)
                        && helper.getBlockState(destination).is(ModBlocks.ANKERSTONE);

                if (!movementFinished && helper.getTick() < movementDeadline) {
                    return;
                }
                finished[0] = true;

                try {
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, first);
                    helper.assertBlockPresent(ModBlocks.ANKERSTONE, second);
                    helper.assertBlockPresent(ModBlocks.ANKERSTONE, destination);
                    helper.assertTrue(firstPlacer.equals(data.getPlacer(secondPosition)),
                            "Rear block must keep its original placer");
                    helper.assertTrue(secondPlacer.equals(data.getPlacer(destinationPosition)),
                            "Front block must keep its original placer");
                    helper.assertTrue(data.getPlacer(firstPosition) == null,
                            "The vacated position must not retain attribution");
                } finally {
                    helper.setBlock(destination, Blocks.AIR);
                    clearPistonFixture(helper, piston);
                    data.removePlacer(firstPosition);
                    data.removePlacer(secondPosition);
                    data.removePlacer(destinationPosition);
                }
                helper.succeed();
            });
        } catch (RuntimeException failure) {
            helper.setBlock(destination, Blocks.AIR);
            clearPistonFixture(helper, piston);
            data.removePlacer(firstPosition);
            data.removePlacer(secondPosition);
            data.removePlacer(destinationPosition);
            throw failure;
        }
    }

    @GameTest(maxTicks = 40)
    public void blockedPushKeepsPlacer(GameTestHelper helper) {
        BlockPos piston = new BlockPos(2, 2, 2);
        BlockPos start = piston.east();
        BlockPos destination = start.east();
        GlobalPos oldPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(start));
        GlobalPos newPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(destination));
        UUID placer = UUID.randomUUID();
        AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
        try {
            helper.setBlock(piston, Blocks.PISTON.defaultBlockState()
                    .setValue(PistonBaseBlock.FACING, Direction.EAST));
            helper.setBlock(start, ModBlocks.ANKERSTONE);
            data.setPlacer(oldPosition, placer);

            helper.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);
            // Obstruct the queued push before its block event runs.
            helper.setBlock(destination, Blocks.OBSIDIAN);

            // Give the powered piston time to attempt movement.
            helper.runAfterDelay(10, () -> {
                try {
                    helper.assertTrue(helper.getLevel().getBestNeighborSignal(
                            helper.absolutePos(piston)) > 0, "Piston must be powered");
                    helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, false);
                    helper.assertBlockPresent(ModBlocks.ANKERSTONE, start);
                    helper.assertBlockProperty(start,
                            AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                    helper.assertBlockPresent(Blocks.OBSIDIAN, destination);
                    helper.assertTrue(placer.equals(data.getPlacer(oldPosition)),
                            "Blocked movement must retain the original placer");
                    helper.assertTrue(data.getPlacer(newPosition) == null,
                            "Blocked movement must not create destination attribution");
                } finally {
                    clearPistonFixture(helper, piston);
                    data.removePlacer(oldPosition);
                    data.removePlacer(newPosition);
                }
                helper.succeed();
            });
        } catch (RuntimeException failure) {
            clearPistonFixture(helper, piston);
            data.removePlacer(oldPosition);
            data.removePlacer(newPosition);
            throw failure;
        }
    }

    @GameTest(maxTicks = 40)
    public void removingMovingAnkerstoneClearsPlacer(GameTestHelper helper) {
        BlockPos piston = new BlockPos(2, 2, 2);
        BlockPos start = piston.east();
        BlockPos destination = start.east();
        GlobalPos oldPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(start));
        GlobalPos newPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(destination));
        UUID placer = UUID.randomUUID();
        AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
        try {
            helper.setBlock(piston, Blocks.PISTON.defaultBlockState()
                    .setValue(PistonBaseBlock.FACING, Direction.EAST));
            helper.setBlock(start, ModBlocks.ANKERSTONE);
            data.setPlacer(oldPosition, placer);
            helper.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);

            // Remove the block during movement, before it settles.
            helper.runAfterDelay(1, () -> {
                try {
                    helper.assertBlockPresent(Blocks.MOVING_PISTON, destination);
                    helper.assertTrue(placer.equals(data.getPlacer(newPosition)),
                            "Moving Ankerstone must already have destination attribution");

                    helper.setBlock(destination, Blocks.AIR);

                    helper.assertBlockPresent(Blocks.AIR, destination);
                    helper.assertTrue(data.getPlacer(newPosition) == null,
                            "Removing moving Ankerstone must clear destination attribution");
                    helper.assertTrue(data.getPlacer(oldPosition) == null,
                            "Interrupted movement must not restore old attribution");
                } finally {
                    clearPistonFixture(helper, piston);
                    data.removePlacer(oldPosition);
                    data.removePlacer(newPosition);
                }
                helper.succeed();
            });
        } catch (RuntimeException failure) {
            clearPistonFixture(helper, piston);
            data.removePlacer(oldPosition);
            data.removePlacer(newPosition);
            throw failure;
        }
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 40)
    public void offAnkerstoneCanBePushed(GameTestHelper helper) {
        assertOffAnkerstoneCanBePushed(helper, Direction.EAST);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 40)
    public void offAnkerstoneCanBePushedWest(GameTestHelper helper) {
        assertOffAnkerstoneCanBePushed(helper, Direction.WEST);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 40)
    public void offAnkerstoneCanBePushedNorth(GameTestHelper helper) {
        assertOffAnkerstoneCanBePushed(helper, Direction.NORTH);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 40)
    public void offAnkerstoneCanBePushedSouth(GameTestHelper helper) {
        assertOffAnkerstoneCanBePushed(helper, Direction.SOUTH);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 40)
    public void offAnkerstoneCanBePushedUp(GameTestHelper helper) {
        assertOffAnkerstoneCanBePushed(helper, Direction.UP);
    }

    @GameTest(environment = "ankerstone-test:off_movement", maxTicks = 40)
    public void offAnkerstoneCanBePushedDown(GameTestHelper helper) {
        assertOffAnkerstoneCanBePushed(helper, Direction.DOWN);
    }

    private void assertActiveAnkerstoneCannotBePushed(GameTestHelper helper,
            AnkerstoneMode mode) {
        BlockPos piston = new BlockPos(2, 2, 2);
        BlockPos start = piston.east();
        int wireLength = mode == AnkerstoneMode.SINGLE ? 2 : 1;
        try {
            helper.setBlock(piston, Blocks.PISTON.defaultBlockState()
                    .setValue(PistonBaseBlock.FACING, Direction.EAST));

            for (int distance = 1; distance <= wireLength; distance++) {
                BlockPos wire = start.south(distance);
                helper.setBlock(wire.below(), Blocks.GLASS);
                helper.setBlock(wire, Blocks.REDSTONE_WIRE);
            }

            helper.setBlock(start.south(wireLength + 1), Blocks.REDSTONE_BLOCK);
            helper.setBlock(start, ModBlocks.ANKERSTONE);
            helper.assertBlockProperty(start, AnkerstoneBlock.MODE, mode);
            helper.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);

            // Give the powered piston time to attempt movement.
            helper.runAfterDelay(10, () -> {
                try {
                    helper.assertTrue(helper.getLevel().getBestNeighborSignal(
                            helper.absolutePos(piston)) > 0, "Piston must be powered");
                    helper.assertBlockPresent(Blocks.PISTON, piston);
                    helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, false);
                    helper.assertBlockPresent(ModBlocks.ANKERSTONE, start);
                    helper.assertBlockProperty(start, AnkerstoneBlock.MODE, mode);
                    helper.assertTrue(AnkerstoneSavedData.get(helper.getLevel())
                            .getOwnerModes().get(helper.absolutePos(start)) == mode,
                            "Blocked Ankerstone must retain its owner mode");
                    helper.assertBlockPresent(Blocks.AIR, start.east());
                } finally {
                    clearPistonFixture(helper, piston);
                }
                helper.succeed();
            });
        } catch (RuntimeException failure) {
            clearPistonFixture(helper, piston);
            throw failure;
        }
    }

}
