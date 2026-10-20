package io.github.omegaplex.ankerstone;

import static io.github.omegaplex.ankerstone.PistonTestSupport.assertAnkerstoneTicketLevel;
import static io.github.omegaplex.ankerstone.PistonTestSupport.assertNoActiveOwnersOrTickets;
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
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.PushReaction;

public class PistonTests {
    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void singleAnchorPowerLossFirst(GameTestHelper helper) {
        assertSameTickPowerLoss(helper, AnkerstoneMode.SINGLE, true);
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void singlePistonPowerLossFirst(GameTestHelper helper) {
        assertSameTickPowerLoss(helper, AnkerstoneMode.SINGLE, false);
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void wideAnchorPowerLossFirst(GameTestHelper helper) {
        assertSameTickPowerLoss(helper, AnkerstoneMode.WIDE, true);
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void widePistonPowerLossFirst(GameTestHelper helper) {
        assertSameTickPowerLoss(helper, AnkerstoneMode.WIDE, false);
    }

    private void assertSameTickPowerLoss(
            GameTestHelper helper, AnkerstoneMode mode, boolean anchorFirst) {
        BlockPos piston = new BlockPos(2, 2, 2);
        BlockPos start = piston.east(2);
        BlockPos returned = piston.east();
        AttributionData data = AttributionData.get(helper.getLevel());
        GlobalPos startPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(start));
        GlobalPos returnedPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(returned));
        UUID placer = UUID.randomUUID();

        Runnable cleanup = () -> {
            clearPistonFixture(helper, piston);
            data.removePlacer(startPosition);
            data.removePlacer(returnedPosition);
            data.removePendingCredit(placer);
        };
        try {
            data.setPlacer(startPosition, placer);
            BlockPos anchorPower = preparePoweredStickyFixture(helper, piston, mode);
            helper.assertTrue(OwnerData.get(helper.getLevel())
                    .getOwnerModes().get(helper.absolutePos(start)) == mode,
                    "Powered fixture must register its owner mode");
            helper.assertTrue(data.hasPendingCredit(placer),
                    "Initial activation must queue credit for the offline placer");
            helper.assertTrue(data.getPlacer(startPosition) == null,
                    "Queued activation credit must consume the placer record");
            if (anchorFirst) {
                helper.setBlock(anchorPower, Blocks.AIR);

                helper.assertBlockProperty(start,
                        AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                assertNoActiveOwnersOrTickets(helper, start, returned);
                helper.assertBlockProperty(piston,
                        PistonBaseBlock.EXTENDED, true);
                helper.assertBlockPresent(Blocks.PISTON_HEAD, returned);

                helper.setBlock(piston.west(), Blocks.AIR);
            } else {
                helper.setBlock(piston.west(), Blocks.AIR);

                helper.assertBlockProperty(start, AnkerstoneBlock.MODE, mode);
                helper.assertTrue(OwnerData.get(helper.getLevel())
                        .getOwnerModes().get(helper.absolutePos(start)) == mode,
                        "Removing piston power must preserve the active owner");
                assertAnkerstoneTicketLevel(helper, start,
                        mode == AnkerstoneMode.WIDE ? 30 : 31);
                helper.assertBlockProperty(piston,
                        PistonBaseBlock.EXTENDED, true);
                helper.assertBlockPresent(Blocks.PISTON_HEAD, returned);

                helper.setBlock(anchorPower, Blocks.AIR);
            }
            helper.assertBlockProperty(start, AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
            long movementDeadline = helper.getTick() + 20;
            boolean[] finished = { false };
            helper.onEachTick(() -> {
                if (finished[0]) {
                    return;
                }
                boolean movementFinished = helper.getBlockState(piston).is(Blocks.STICKY_PISTON)
                        && helper.getBlockState(returned).is(ModBlocks.ANKERSTONE)
                        && helper.getBlockState(start).is(Blocks.AIR);
                if (!movementFinished && helper.getTick() < movementDeadline) {
                    return;
                }
                finished[0] = true;
                try {
                    helper.assertBlockPresent(Blocks.STICKY_PISTON, piston);
                    helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, false);
                    helper.assertBlockPresent(ModBlocks.ANKERSTONE, returned);
                    helper.assertBlockProperty(returned, AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                    helper.assertBlockPresent(Blocks.AIR, start);
                    assertNoActiveOwnersOrTickets(helper, start, returned);
                    helper.assertTrue(
                            data.getPlacer(startPosition) == null
                                    && data.getPlacer(returnedPosition) == null,
                            "Retraction must not leave or recreate placer records");
                    helper.assertTrue(data.hasPendingCredit(placer),
                            "Retraction must preserve already-earned offline credit");
                } finally {
                    cleanup.run();
                }
                helper.succeed();
            });
        } catch (RuntimeException failure) {
            cleanup.run();
            throw failure;
        }
    }

    private void assertPoweredAnkerstoneCannotBePulled(
            GameTestHelper helper, AnkerstoneMode mode) {
        BlockPos piston = new BlockPos(2, 2, 2);
        BlockPos start = piston.east(2);
        BlockPos returned = piston.east();
        AttributionData data = AttributionData.get(helper.getLevel());
        GlobalPos startPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(start));
        GlobalPos returnedPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(returned));
        UUID placer = UUID.randomUUID();

        Runnable cleanup = () -> {
            clearPistonFixture(helper, piston);
            data.removePlacer(startPosition);
            data.removePlacer(returnedPosition);
            data.removePendingCredit(placer);
        };
        try {

            data.setPlacer(startPosition, placer);
            preparePoweredStickyFixture(helper, piston, mode);
            helper.assertTrue(OwnerData.get(helper.getLevel())
                    .getOwnerModes().get(helper.absolutePos(start)) == mode,
                    "Powered fixture must register its owner mode");
            helper.assertTrue(data.hasPendingCredit(placer),
                    "Initial activation must queue credit for the offline placer");
            helper.assertTrue(data.getPlacer(startPosition) == null,
                    "Queued activation credit must consume the placer record");
            helper.setBlock(piston.west(), Blocks.AIR);
            helper.assertBlockProperty(start, AnkerstoneBlock.MODE, mode);
            long movementDeadline = helper.getTick() + 20;
            boolean[] finished = { false };
            helper.onEachTick(() -> {
                if (finished[0]) {
                    return;
                }
                BlockState pistonState = helper.getBlockState(piston);
                boolean movementFinished = pistonState.is(Blocks.STICKY_PISTON)
                        && !pistonState.getValue(PistonBaseBlock.EXTENDED);
                if (!movementFinished && helper.getTick() < movementDeadline) {
                    return;
                }
                finished[0] = true;
                try {
                    helper.assertBlockPresent(Blocks.STICKY_PISTON, piston);
                    helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, false);
                    helper.assertBlockPresent(Blocks.AIR, returned);
                    helper.assertBlockPresent(ModBlocks.ANKERSTONE, start);
                    helper.assertBlockProperty(start, AnkerstoneBlock.MODE, mode);
                    OwnerData savedData = OwnerData.get(helper.getLevel());
                    helper.assertTrue(savedData.getOwnerModes().get(helper.absolutePos(start)) == mode,
                            "Blocked pull must retain the active owner mode");
                    helper.assertFalse(savedData.getOwnerModes().containsKey(helper.absolutePos(returned)),
                            "Blocked pull must not create an owner beside the piston");
                    assertAnkerstoneTicketLevel(helper, start,
                            mode == AnkerstoneMode.WIDE ? 30 : 31);
                    helper.assertTrue(
                            data.getPlacer(startPosition) == null
                                    && data.getPlacer(returnedPosition) == null,
                            "Retraction must not leave or recreate placer records");
                    helper.assertTrue(data.hasPendingCredit(placer),
                            "Retraction must preserve already-earned offline credit");
                } finally {
                    cleanup.run();
                }
                assertNoActiveOwnersOrTickets(helper, start, returned);
                helper.succeed();
            });
        } catch (RuntimeException failure) {
            cleanup.run();
            throw failure;
        }
    }

    private BlockPos preparePoweredStickyFixture(
            GameTestHelper helper, BlockPos piston, AnkerstoneMode mode) {
        BlockPos start = piston.east(2);
        int wireLength = mode == AnkerstoneMode.SINGLE ? 2 : 1;
        BlockPos anchorPower = start.east(wireLength + 1);

        helper.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(piston, Blocks.STICKY_PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.EAST)
                .setValue(PistonBaseBlock.EXTENDED, true));
        helper.setBlock(piston.east(), Blocks.PISTON_HEAD.defaultBlockState()
                .setValue(PistonHeadBlock.FACING, Direction.EAST)
                .setValue(PistonHeadBlock.TYPE, PistonType.STICKY));

        for (int distance = 1; distance <= wireLength; distance++) {
            BlockPos wire = start.east(distance);
            helper.setBlock(wire.below(), Blocks.GLASS);
            helper.setBlock(wire, Blocks.REDSTONE_WIRE);
        }

        helper.setBlock(anchorPower, Blocks.REDSTONE_BLOCK);
        helper.setBlock(start, ModBlocks.ANKERSTONE);
        helper.assertBlockProperty(start, AnkerstoneBlock.MODE, mode);
        assertAnkerstoneTicketLevel(helper, start,
                mode == AnkerstoneMode.WIDE ? 30 : 31);
        return anchorPower;
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void singleCannotBePulledWhilePowered(GameTestHelper helper) {
        assertPoweredAnkerstoneCannotBePulled(helper, AnkerstoneMode.SINGLE);
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void wideCannotBePulledWhilePowered(GameTestHelper helper) {
        assertPoweredAnkerstoneCannotBePulled(helper, AnkerstoneMode.WIDE);
    }

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
        AttributionData data = AttributionData.get(helper.getLevel());
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
        AttributionData data = AttributionData.get(helper.getLevel());
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
        AttributionData data = AttributionData.get(helper.getLevel());
        try {
            helper.setBlock(piston, Blocks.PISTON.defaultBlockState()
                    .setValue(PistonBaseBlock.FACING, Direction.EAST));
            helper.setBlock(start, ModBlocks.ANKERSTONE);
            data.setPlacer(oldPosition, placer);
            helper.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);

            long deadline = helper.getTick() + 20;
            boolean[] finished = { false };
            helper.onEachTick(() -> {
                if (finished[0]) {
                    return;
                }
                if (!helper.getBlockState(destination).is(Blocks.MOVING_PISTON)
                        && helper.getTick() < deadline) {
                    return;
                }
                finished[0] = true;
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
    public void removingRetractingAnkerstoneClearsPlacer(GameTestHelper helper) {
        BlockPos piston = new BlockPos(2, 2, 2);
        BlockPos start = piston.east(2);
        BlockPos destination = piston.east();
        GlobalPos oldPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(start));
        GlobalPos newPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(destination));
        UUID placer = UUID.randomUUID();
        AttributionData data = AttributionData.get(helper.getLevel());
        try {
            helper.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);
            helper.setBlock(piston, Blocks.STICKY_PISTON.defaultBlockState()
                    .setValue(PistonBaseBlock.FACING, Direction.EAST)
                    .setValue(PistonBaseBlock.EXTENDED, true));
            helper.setBlock(destination, Blocks.PISTON_HEAD.defaultBlockState()
                    .setValue(PistonHeadBlock.FACING, Direction.EAST)
                    .setValue(PistonHeadBlock.TYPE, PistonType.STICKY));
            helper.setBlock(start, ModBlocks.ANKERSTONE);
            helper.assertBlockProperty(start,
                    AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
            data.setPlacer(oldPosition, placer);
            helper.setBlock(piston.west(), Blocks.AIR);

            long deadline = helper.getTick() + 20;
            boolean[] finished = { false };
            helper.onEachTick(() -> {
                if (finished[0]) {
                    return;
                }
                if (!helper.getBlockState(destination).is(Blocks.MOVING_PISTON)
                        && helper.getTick() < deadline) {
                    return;
                }
                finished[0] = true;
                try {
                    helper.assertBlockPresent(Blocks.MOVING_PISTON, destination);
                    helper.assertTrue(placer.equals(data.getPlacer(newPosition)),
                            "Moving Ankerstone must already have destination attribution");

                    helper.setBlock(destination, Blocks.AIR);

                    helper.assertBlockPresent(Blocks.AIR, destination);
                    helper.assertBlockPresent(Blocks.AIR, start);
                    assertNoActiveOwnersOrTickets(helper, start, destination);
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
                    helper.assertTrue(OwnerData.get(helper.getLevel())
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
