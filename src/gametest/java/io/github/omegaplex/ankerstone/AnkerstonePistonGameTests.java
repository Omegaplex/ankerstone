package io.github.omegaplex.ankerstone;

import java.util.List;
import java.util.UUID;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.TicketStorage;

public class AnkerstonePistonGameTests {
    private static final TicketType NEGATIVE_TEST_TICKET = new TicketType(
            240, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION);

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

    private BlockPos pistonBeforeChunkBoundary(GameTestHelper helper, Direction direction) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        boolean alongX = direction == Direction.EAST;
        int coordinate = alongX ? origin.getX() : origin.getZ();
        int boundary = (Math.floorDiv(coordinate + 4, 16) + 1) * 16;
        int offset = boundary - coordinate - 2;
        BlockPos piston = alongX
                ? new BlockPos(offset, 3, 3)
                : new BlockPos(3, 3, offset);
        ChunkPos start = ChunkPos.containing(
                helper.absolutePos(piston.relative(direction)));
        ChunkPos destination = ChunkPos.containing(
                helper.absolutePos(piston.relative(direction, 2)));

        helper.assertFalse(start.equals(destination),
                "The fixture must cross an actual chunk boundary");
        return piston;
    }

    private void assertOffAnkerstoneCanBePushed(
            GameTestHelper helper, Direction direction) {
        assertOffAnkerstoneCanBePushed(helper, direction, new BlockPos(3, 3, 3));
    }

    private void assertOffAnkerstoneCanBePushed(
            GameTestHelper helper, Direction direction, BlockPos piston) {
        BlockPos start = piston.relative(direction);
        BlockPos destination = start.relative(direction);
        assertNoAnkerstoneTickets(helper, start);
        assertNoAnkerstoneTickets(helper, destination);
        BlockPos power = piston.relative(direction.getOpposite());
        GlobalPos oldPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(start));
        GlobalPos newPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(destination));
        UUID placer = UUID.randomUUID();
        AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
        try {
            helper.setBlock(piston, Blocks.PISTON.defaultBlockState()
                    .setValue(PistonBaseBlock.FACING, direction));
            helper.setBlock(start, ModBlocks.ANKERSTONE);
            data.setPlacer(oldPosition, placer);
            helper.assertBlockProperty(start, AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
            helper.assertBlockPresent(Blocks.AIR, destination);
            helper.setBlock(power, Blocks.REDSTONE_BLOCK);

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
                    helper.assertBlockProperty(destination,
                            AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                    AnkerstoneSavedData savedData = AnkerstoneSavedData.get(helper.getLevel());
                    helper.assertFalse(savedData.getOwnerModes().containsKey(
                            helper.absolutePos(start)),
                            "Off push must not leave an owner at the old position");
                    helper.assertFalse(savedData.getOwnerModes().containsKey(
                            helper.absolutePos(destination)),
                            "Off push must not register an owner at the destination");
                    helper.assertTrue(placer.equals(data.getPlacer(newPosition)),
                            "Off push must preserve the placer at the destination");
                    helper.assertTrue(data.getPlacer(oldPosition) == null,
                            "Off push must clear the old placer record");
                    assertNoAnkerstoneTickets(helper, start);
                    assertNoAnkerstoneTickets(helper, destination);
                } finally {
                    clearOffPistonFixture(helper, piston, direction);
                    data.removePlacer(oldPosition);
                    data.removePlacer(newPosition);
                }
                helper.succeed();
            });
        } catch (RuntimeException failure) {
            clearOffPistonFixture(helper, piston, direction);
            data.removePlacer(oldPosition);
            data.removePlacer(newPosition);
            throw failure;
        }
    }

    private void assertAnkerstoneTicketLevel(GameTestHelper helper,
            BlockPos localPos, int expectedLevel) {
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(localPos));
        TicketStorage storage = helper.getLevel().getChunkSource()
                .getDataStorage().computeIfAbsent(TicketStorage.TYPE);
        List<Integer> levels = storage.getTickets(chunk.pack()).stream()
                .filter(ticket -> ticket.getType() == AnkerstoneChunkTickets.ANKERSTONE_TICKET)
                .map(ticket -> ticket.getTicketLevel())
                .sorted()
                .toList();

        helper.assertTrue(levels.equals(List.of(expectedLevel)),
                "Expected Ankerstone ticket level " + expectedLevel
                        + " at " + chunk + ", got " + levels);
    }

    private void assertOffAnkerstoneCanBePulled(
            GameTestHelper helper, Direction direction) {
        assertOffAnkerstoneCanBePulled(helper, direction, new BlockPos(3, 3, 3));
    }

    private void assertOffAnkerstoneCanBePulled(
            GameTestHelper helper, Direction direction, BlockPos piston) {
        BlockPos start = piston.relative(direction);
        BlockPos destination = start.relative(direction);
        BlockPos power = piston.relative(direction.getOpposite());
        GlobalPos oldPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(start));
        GlobalPos newPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(destination));
        UUID placer = UUID.randomUUID();
        AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
        try {
            helper.setBlock(piston, Blocks.STICKY_PISTON.defaultBlockState()
                    .setValue(PistonBaseBlock.FACING, direction));
            helper.setBlock(start, ModBlocks.ANKERSTONE);
            data.setPlacer(oldPosition, placer);
            helper.assertBlockProperty(start, AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
            assertNoActiveOwnersOrTickets(helper, start, destination);
            helper.setBlock(power, Blocks.REDSTONE_BLOCK);

            long extensionDeadline = helper.getTick() + 15;
            long retractionDeadline = helper.getTick() + 30;
            boolean[] retracting = { false };
            boolean[] finished = { false };

            helper.onEachTick(() -> {
                if (finished[0]) {
                    return;
                }

                if (!retracting[0]) {
                    boolean extensionFinished = helper.getBlockState(start).is(Blocks.PISTON_HEAD)
                            && helper.getBlockState(destination).is(ModBlocks.ANKERSTONE);

                    if (!extensionFinished && helper.getTick() < extensionDeadline) {
                        return;
                    }

                    try {
                        helper.assertBlockPresent(Blocks.STICKY_PISTON, piston);
                        helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, true);
                        helper.assertBlockPresent(Blocks.PISTON_HEAD, start);
                        helper.assertBlockPresent(ModBlocks.ANKERSTONE, destination);
                        helper.assertBlockProperty(destination,
                                AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                        helper.assertTrue(placer.equals(data.getPlacer(newPosition)),
                                "Sticky extension must preserve the placer");
                        helper.assertTrue(data.getPlacer(oldPosition) == null,
                                "Sticky extension must clear the old placer record");
                        assertNoActiveOwnersOrTickets(helper, start, destination);
                        helper.setBlock(power, Blocks.AIR);
                        retracting[0] = true;
                    } catch (RuntimeException failure) {
                        finished[0] = true;
                        clearOffPistonFixture(helper, piston, direction);
                        data.removePlacer(oldPosition);
                        data.removePlacer(newPosition);
                        throw failure;
                    }

                    return;
                }

                boolean retractionFinished = helper.getBlockState(start).is(ModBlocks.ANKERSTONE)
                        && helper.getBlockState(destination).is(Blocks.AIR);

                if (!retractionFinished && helper.getTick() < retractionDeadline) {
                    return;
                }
                finished[0] = true;
                try {
                    helper.assertBlockPresent(Blocks.STICKY_PISTON, piston);
                    helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, false);
                    helper.assertBlockPresent(ModBlocks.ANKERSTONE, start);
                    helper.assertBlockProperty(start,
                            AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                    helper.assertBlockPresent(Blocks.AIR, destination);
                    helper.assertTrue(placer.equals(data.getPlacer(oldPosition)),
                            "Sticky retraction must return the original placer");
                    helper.assertTrue(data.getPlacer(newPosition) == null,
                            "Sticky retraction must clear the outer placer record");
                    assertNoActiveOwnersOrTickets(helper, start, destination);
                } finally {
                    clearOffPistonFixture(helper, piston, direction);
                    data.removePlacer(oldPosition);
                    data.removePlacer(newPosition);
                }
                helper.succeed();
            });
        } catch (RuntimeException failure) {
            clearOffPistonFixture(helper, piston, direction);
            data.removePlacer(oldPosition);
            data.removePlacer(newPosition);
            throw failure;
        }
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void singleAnkerstoneCannotBePushed(GameTestHelper helper) {
        assertActiveAnkerstoneCannotBePushed(helper, AnkerstoneMode.SINGLE);
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void wideAnkerstoneCannotBePushed(GameTestHelper helper) {
        assertActiveAnkerstoneCannotBePushed(helper, AnkerstoneMode.WIDE);
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void movedAnkerstoneActivatesSingle(GameTestHelper helper) {
        assertMovedAnkerstoneActivates(helper, AnkerstoneMode.SINGLE);
    }

    @GameTest(environment = "ankerstone-test:pistons", structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
    public void movedAnkerstoneActivatesWide(GameTestHelper helper) {
        assertMovedAnkerstoneActivates(helper, AnkerstoneMode.WIDE);
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

    @GameTest(environment = "ankerstone-test:negative_boundaries", maxTicks = 60)
    public void negativeBoundaryArrivalActivatesSingle(GameTestHelper helper) {
        assertNegativeBoundaryActivation(helper,
                new BlockPos(-18, 100, -3072), AnkerstoneMode.SINGLE);
    }

    @GameTest(environment = "ankerstone-test:negative_boundaries", maxTicks = 60)
    public void negativeBoundaryArrivalActivatesWide(GameTestHelper helper) {
        assertNegativeBoundaryActivation(helper,
                new BlockPos(-18, 100, -4096), AnkerstoneMode.WIDE);
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
                    helper.assertTrue(
                            helper.getLevel().isPositionEntityTicking(worldPiston)
                                    && helper.getLevel().isPositionEntityTicking(
                                            worldPiston.east(2)),
                            "Negative arrival chunks were not ready by tick 160");
                } catch (RuntimeException failure) {
                    clearPistonFixture(helper, piston);
                    throw failure;
                }
            });
            helper.startSequence()
                    .thenWaitUntil(() -> {

                        helper.assertTrue(
                                helper.getLevel().isPositionEntityTicking(worldPiston),
                                "Source chunk must be ticking before movement starts");
                        helper.assertTrue(
                                helper.getLevel().isPositionEntityTicking(worldPiston.east(2)),
                                "Destination chunk must be ticking before movement starts");
                    })
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

    private void loadNegativeBoundaryChunks(GameTestHelper helper,
            BlockPos piston, Direction direction) {
        for (BlockPos pos : List.of(piston.relative(direction),
                piston.relative(direction, 2))) {
            ChunkPos chunk = ChunkPos.containing(helper.absolutePos(pos));
            helper.getLevel().getChunkSource().addTicketWithRadius(
                    NEGATIVE_TEST_TICKET, chunk, 2);
            helper.getLevel().getChunk(chunk.x(), chunk.z());
        }
    }

    private void releaseNegativeTestTickets(GameTestHelper helper,
            BlockPos piston, Direction direction) {
        for (BlockPos pos : List.of(piston.relative(direction),
                piston.relative(direction, 2))) {
            ChunkPos chunk = ChunkPos.containing(helper.absolutePos(pos));
            helper.getLevel().getChunkSource().removeTicketWithRadius(
                    NEGATIVE_TEST_TICKET, chunk, 2);
        }
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
                    helper.assertTrue(
                            helper.getLevel().isPositionEntityTicking(worldPiston)
                                    && helper.getLevel().isPositionEntityTicking(
                                            worldPiston.relative(direction, 2)),
                            "Negative boundary chunks were not ready by tick 160");
                } catch (RuntimeException failure) {
                    clearOffPistonFixture(helper, piston, direction);
                    throw failure;
                }
            });
            helper.startSequence()
                    .thenWaitUntil(() -> {
                        helper.assertTrue(
                                helper.getLevel().isPositionEntityTicking(worldPiston),
                                "Source chunk must be ticking before movement starts");
                        helper.assertTrue(
                                helper.getLevel().isPositionEntityTicking(
                                        worldPiston.relative(direction, 2)),
                                "Destination chunk must be ticking before movement starts");
                    })
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

    private void assertNoAnkerstoneTickets(GameTestHelper helper, BlockPos localPos) {
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(localPos));
        TicketStorage storage = helper.getLevel().getChunkSource()
                .getDataStorage().computeIfAbsent(TicketStorage.TYPE);
        long count = storage.getTickets(chunk.pack()).stream()
                .filter(ticket -> ticket.getType() == AnkerstoneChunkTickets.ANKERSTONE_TICKET)
                .count();

        helper.assertTrue(count == 0,
                "Expected no Ankerstone tickets at " + chunk + ", got " + count);
    }

    private void assertNoActiveOwnersOrTickets(GameTestHelper helper,
            BlockPos first, BlockPos second) {
        AnkerstoneSavedData savedData = AnkerstoneSavedData.get(helper.getLevel());
        for (BlockPos pos : List.of(first, second)) {
            helper.assertFalse(savedData.getOwnerModes().containsKey(
                    helper.absolutePos(pos)),
                    "Off movement must not register an owner at " + pos);
            assertNoAnkerstoneTickets(helper, pos);
        }
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

    private void assertMovedAnkerstoneActivates(GameTestHelper helper,
            AnkerstoneMode mode) {
        assertMovedAnkerstoneActivates(helper, mode, new BlockPos(2, 2, 2));
    }

    private void assertMovedAnkerstoneActivates(GameTestHelper helper,
            AnkerstoneMode mode, BlockPos piston) {

        BlockPos start = piston.east();
        BlockPos destination = start.east();
        AnkerstoneSavedData savedData = AnkerstoneSavedData.get(helper.getLevel());
        AnkerstoneAttributionData attribution = AnkerstoneAttributionData.get(helper.getLevel());
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
        releaseNegativeTestTickets(helper, piston, Direction.EAST);
    }

    private void clearOffPistonFixture(GameTestHelper helper,
            BlockPos piston, Direction direction) {
        helper.setBlock(piston.relative(direction, 2), Blocks.AIR);
        helper.setBlock(piston, Blocks.AIR);
        helper.setBlock(piston.relative(direction), Blocks.AIR);
        helper.setBlock(piston.relative(direction.getOpposite()), Blocks.AIR);
        releaseNegativeTestTickets(helper, piston, direction);
    }
}
