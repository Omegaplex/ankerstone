package io.github.omegaplex.ankerstone;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class AnkerstonePistonGameTests {
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

    @GameTest(maxTicks = 40)
    public void offAnkerstoneCanBePushed(GameTestHelper helper) {
        BlockPos piston = new BlockPos(2, 2, 2);
        BlockPos start = piston.east();
        BlockPos destination = start.east();
        BlockPos power = piston.west();

        helper.setBlock(piston, Blocks.PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(start, ModBlocks.ANKERSTONE);
        helper.assertBlockProperty(start, AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
        helper.assertBlockPresent(Blocks.AIR, destination);

        helper.setBlock(power, Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(10, () -> {
            try {
                helper.assertBlockPresent(Blocks.PISTON, piston);
                helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, true);
                helper.assertBlockPresent(Blocks.PISTON_HEAD, start);
                helper.assertBlockPresent(ModBlocks.ANKERSTONE, destination);
                helper.assertBlockProperty(destination,
                        AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                AnkerstoneSavedData savedData = AnkerstoneSavedData.get(helper.getLevel());
                helper.assertFalse(savedData.getOwnerModes().containsKey(
                        helper.absolutePos(start)),
                        "Off push must not leave an owner at the old position");
                helper.assertFalse(savedData.getOwnerModes().containsKey(
                        helper.absolutePos(destination)),
                        "Off push must not register an owner at the destination");
            } finally {
                clearPistonFixture(helper, piston);
            }
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void offAnkerstoneCanBePulled(GameTestHelper helper) {
        BlockPos piston = new BlockPos(2, 2, 2);
        BlockPos start = piston.east();
        BlockPos destination = start.east();
        BlockPos power = piston.west();

        helper.setBlock(piston, Blocks.STICKY_PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(start, ModBlocks.ANKERSTONE);
        helper.assertBlockProperty(start, AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
        helper.setBlock(power, Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(10, () -> {
            try {
                helper.assertBlockPresent(Blocks.STICKY_PISTON, piston);
                helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, true);
                helper.assertBlockPresent(Blocks.PISTON_HEAD, start);
                helper.assertBlockPresent(ModBlocks.ANKERSTONE, destination);
                helper.assertBlockProperty(destination,
                        AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                helper.setBlock(power, Blocks.AIR);
            } catch (RuntimeException failure) {
                clearPistonFixture(helper, piston);
                throw failure;
            }
        });

        helper.runAfterDelay(20, () -> {
            try {
                helper.assertBlockPresent(Blocks.STICKY_PISTON, piston);
                helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, false);
                helper.assertBlockPresent(ModBlocks.ANKERSTONE, start);
                helper.assertBlockProperty(start,
                        AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                helper.assertBlockPresent(Blocks.AIR, destination);
            } finally {
                clearPistonFixture(helper, piston);
            }
            helper.succeed();
        });
    }

    @GameTest(environment = "ankerstone-test:pistons", maxTicks = 40)
    public void singleAnkerstoneCannotBePushed(GameTestHelper helper) {
        assertActiveAnkerstoneCannotBePushed(helper, AnkerstoneMode.SINGLE);
    }

    @GameTest(environment = "ankerstone-test:pistons", maxTicks = 40)
    public void wideAnkerstoneCannotBePushed(GameTestHelper helper) {
        assertActiveAnkerstoneCannotBePushed(helper, AnkerstoneMode.WIDE);
    }

    @GameTest(environment = "ankerstone-test:pistons", maxTicks = 40)
    public void movedAnkerstoneActivatesSingle(GameTestHelper helper) {
        assertMovedAnkerstoneActivates(helper, AnkerstoneMode.SINGLE);
    }

    @GameTest(environment = "ankerstone-test:pistons", maxTicks = 40)
    public void movedAnkerstoneActivatesWide(GameTestHelper helper) {
        assertMovedAnkerstoneActivates(helper, AnkerstoneMode.WIDE);
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

    private void assertMovedAnkerstoneActivates(GameTestHelper helper,
            AnkerstoneMode mode) {
        BlockPos piston = new BlockPos(2, 2, 2);
        BlockPos start = piston.east();
        BlockPos destination = start.east();
        AnkerstoneSavedData savedData = AnkerstoneSavedData.get(helper.getLevel());
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
            helper.assertBlockProperty(start, AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
            helper.assertBlockPresent(Blocks.AIR, destination);
            helper.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);
            helper.runAfterDelay(10, () -> {
                try {
                    helper.assertBlockPresent(Blocks.PISTON, piston);
                    helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, true);
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, start);
                    helper.assertBlockPresent(ModBlocks.ANKERSTONE, destination);
                    helper.assertBlockProperty(destination, AnkerstoneBlock.MODE, mode);
                    helper.assertTrue(savedData.getOwnerModes().get(
                            helper.absolutePos(destination)) == mode,
                            "Destination must register the expected owner mode");
                    helper.assertFalse(savedData.getOwnerModes().containsKey(
                            helper.absolutePos(start)),
                            "Old position must not retain an owner");
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

    private void clearPistonFixture(GameTestHelper helper, BlockPos piston) {
        helper.setBlock(piston.east(2), Blocks.AIR);
        helper.setBlock(piston, Blocks.AIR);
        helper.setBlock(piston.east(), Blocks.AIR);
        helper.setBlock(piston.west(), Blocks.AIR);

        for (int distance = 1; distance <= 3; distance++) {
            BlockPos pos = piston.east().south(distance);
            helper.setBlock(pos, Blocks.AIR);
            helper.setBlock(pos.below(), Blocks.AIR);
        }
        for (int distance = 1; distance <= 3; distance++) {
            BlockPos pos = piston.east(2 + distance);
            helper.setBlock(pos, Blocks.AIR);
            helper.setBlock(pos.below(), Blocks.AIR);
        }
    }

}