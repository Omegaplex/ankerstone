package io.github.omegaplex.ankerstone;

import java.util.List;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.TicketStorage;

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

    private void assertOffAnkerstoneCanBePushed(
            GameTestHelper helper, Direction direction) {
        BlockPos piston = new BlockPos(3, 3, 3);
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

    private void assertOffAnkerstoneCanBePulled(
            GameTestHelper helper, Direction direction) {
        BlockPos piston = new BlockPos(3, 3, 3);
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

            helper.runAfterDelay(10, () -> {
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
                } catch (RuntimeException failure) {
                    clearOffPistonFixture(helper, piston, direction);
                    data.removePlacer(oldPosition);
                    data.removePlacer(newPosition);
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
    public void pistonPushKeepsPlacer(GameTestHelper helper) {
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

            helper.runAfterDelay(10, () -> {
                try {
                    helper.assertBlockPresent(ModBlocks.ANKERSTONE, destination);
                    helper.assertBlockProperty(destination,
                            AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                    helper.assertTrue(placer.equals(data.getPlacer(newPosition)),
                            "Piston push must keep the original placer at the destination");
                    helper.assertTrue(data.getPlacer(oldPosition) == null,
                            "Piston push must clear attribution at the old position");
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
    public void stickyPistonPullKeepsPlacer(GameTestHelper helper) {
        BlockPos piston = new BlockPos(2, 2, 2);
        BlockPos start = piston.east(2);
        BlockPos destination = piston.east();
        GlobalPos oldPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(start));
        GlobalPos newPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(destination));
        UUID placer = UUID.randomUUID();
        AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
        try {
            helper.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);
            helper.setBlock(piston, Blocks.STICKY_PISTON.defaultBlockState()
                    .setValue(PistonBaseBlock.FACING, Direction.EAST)
                    .setValue(PistonBaseBlock.EXTENDED, true));
            helper.setBlock(destination, Blocks.PISTON_HEAD.defaultBlockState()
                    .setValue(PistonHeadBlock.FACING, Direction.EAST)
                    .setValue(PistonHeadBlock.TYPE, PistonType.STICKY));
            helper.setBlock(start, ModBlocks.ANKERSTONE);
            data.setPlacer(oldPosition, placer);

            helper.setBlock(piston.west(), Blocks.AIR);

            helper.runAfterDelay(10, () -> {
                try {
                    helper.assertBlockPresent(Blocks.STICKY_PISTON, piston);
                    helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, false);
                    helper.assertBlockPresent(ModBlocks.ANKERSTONE, destination);
                    helper.assertBlockProperty(destination,
                            AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                    helper.assertBlockPresent(Blocks.AIR, start);
                    helper.assertTrue(placer.equals(data.getPlacer(newPosition)),
                            "Sticky pull must keep the original placer at the destination");
                    helper.assertTrue(data.getPlacer(oldPosition) == null,
                            "Sticky pull must clear attribution at the old position");
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

            helper.runAfterDelay(10, () -> {
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
    }

    private void clearOffPistonFixture(GameTestHelper helper,
            BlockPos piston, Direction direction) {
        helper.setBlock(piston.relative(direction, 2), Blocks.AIR);
        helper.setBlock(piston, Blocks.AIR);
        helper.setBlock(piston.relative(direction), Blocks.AIR);
        helper.setBlock(piston.relative(direction.getOpposite()), Blocks.AIR);
    }
}
