package io.github.omegaplex.ankerstone;

import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class PlacementTests {
        @GameTest
        public void alreadyWidePlacementRegistersAndReleasesOwner(GameTestHelper helper) {
                BlockPos localPos = new BlockPos(2, 2, 2);
                BlockPos ownerPos = helper.absolutePos(localPos);
                AnkerstoneSavedData savedData = AnkerstoneSavedData.get(helper.getLevel());
                BlockState wideState = ModBlocks.ANKERSTONE.defaultBlockState()
                                .setValue(AnkerstoneBlock.MODE, AnkerstoneMode.WIDE);

                try {
                        helper.setBlock(localPos.below(), Blocks.REDSTONE_BLOCK);
                        helper.setBlock(localPos, wideState);

                        helper.assertBlockProperty(localPos, AnkerstoneBlock.MODE,
                                        AnkerstoneMode.WIDE);
                        helper.assertTrue(
                                        savedData.getOwnerModes().get(ownerPos) == AnkerstoneMode.WIDE,
                                        "Already-Wide placement must register a Wide owner");
                } finally {
                        helper.setBlock(localPos, Blocks.AIR);
                        helper.setBlock(localPos.below(), Blocks.AIR);
                }

                helper.assertTrue(
                                !savedData.getOwnerModes().containsKey(ownerPos),
                                "Removing Ankerstone must remove its saved owner entry");
                helper.succeed();
        }

        @GameTest
        public void powerChangesUpdateModeAndOwner(GameTestHelper helper) {
                BlockPos localPos = new BlockPos(2, 2, 2);

                try {
                        helper.setBlock(localPos, ModBlocks.ANKERSTONE);

                        setPowerAndAssert(helper, localPos, 0, AnkerstoneMode.OFF);
                        setPowerAndAssert(helper, localPos, 1, AnkerstoneMode.SINGLE);
                        setPowerAndAssert(helper, localPos, 14, AnkerstoneMode.SINGLE);
                        setPowerAndAssert(helper, localPos, 15, AnkerstoneMode.WIDE);
                        setPowerAndAssert(helper, localPos, 14, AnkerstoneMode.SINGLE);
                        setPowerAndAssert(helper, localPos, 0, AnkerstoneMode.OFF);
                        setPowerAndAssert(helper, localPos, 15, AnkerstoneMode.WIDE);
                        setPowerAndAssert(helper, localPos, 0, AnkerstoneMode.OFF);
                } finally {
                        helper.setBlock(localPos, Blocks.AIR);
                        helper.setBlock(localPos.below(), Blocks.AIR);
                }

                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:ticket_ownership")
        public void sharedTicketRemainsUntilLastOwnerIsRemoved(GameTestHelper helper) {
                BlockPos first = new BlockPos(2, 2, 2);
                BlockPos second = new BlockPos(2, 4, 2);
                ChunkPos chunk = ChunkPos.containing(helper.absolutePos(first));

                assertTicketCount(helper, chunk, 0);
                try {
                        helper.setBlock(first.below(), Blocks.REDSTONE_BLOCK);
                        helper.setBlock(second.below(), Blocks.REDSTONE_BLOCK);

                        helper.setBlock(first, ModBlocks.ANKERSTONE);
                        assertTicketCount(helper, chunk, 1);
                        helper.setBlock(second, ModBlocks.ANKERSTONE);
                        assertTicketCount(helper, chunk, 1);

                        helper.setBlock(first, Blocks.AIR);
                        assertTicketCount(helper, chunk, 1);
                        helper.setBlock(second, Blocks.AIR);
                        assertTicketCount(helper, chunk, 0);
                } finally {
                        helper.setBlock(first, Blocks.AIR);
                        helper.setBlock(second, Blocks.AIR);
                        helper.setBlock(first.below(), Blocks.AIR);
                        helper.setBlock(second.below(), Blocks.AIR);
                }
                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:ticket_ownership")
        public void mixedModesKeepTheirOwnTickets(GameTestHelper helper) {
                BlockPos single = new BlockPos(2, 2, 2);
                BlockPos wide = new BlockPos(2, 6, 2);
                ChunkPos chunk = ChunkPos.containing(helper.absolutePos(single));

                assertTicketLevels(helper, chunk, List.of());
                try {
                        helper.setBlock(single, ModBlocks.ANKERSTONE);
                        setPowerAndAssert(helper, single, 1, AnkerstoneMode.SINGLE);
                        assertTicketLevels(helper, chunk, List.of(31));

                        helper.setBlock(wide, ModBlocks.ANKERSTONE);
                        setPowerAndAssert(helper, wide, 15, AnkerstoneMode.WIDE);
                        assertTicketLevels(helper, chunk, List.of(30, 31));

                        helper.setBlock(wide, Blocks.AIR);
                        assertTicketLevels(helper, chunk, List.of(31));

                        helper.setBlock(wide, ModBlocks.ANKERSTONE);
                        setPowerAndAssert(helper, wide, 15, AnkerstoneMode.WIDE);
                        assertTicketLevels(helper, chunk, List.of(30, 31));

                        helper.setBlock(single, Blocks.AIR);
                        assertTicketLevels(helper, chunk, List.of(30));
                        helper.setBlock(wide, Blocks.AIR);
                        assertTicketLevels(helper, chunk, List.of());
                } finally {
                        helper.setBlock(single, Blocks.AIR);
                        helper.setBlock(wide, Blocks.AIR);
                        helper.setBlock(single.below(), Blocks.AIR);
                        helper.setBlock(wide.below(), Blocks.AIR);
                }
                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:ticket_ownership")
        public void singleOwnerSurvivesModeChanges(GameTestHelper helper) {
                BlockPos steady = new BlockPos(2, 2, 2);
                BlockPos changing = new BlockPos(2, 6, 2);
                ChunkPos chunk = ChunkPos.containing(helper.absolutePos(steady));
                AnkerstoneSavedData savedData = AnkerstoneSavedData.get(helper.getLevel());

                assertTicketLevels(helper, chunk, List.of());
                try {
                        helper.setBlock(steady, ModBlocks.ANKERSTONE);
                        helper.setBlock(changing, ModBlocks.ANKERSTONE);
                        setPowerAndAssert(helper, steady, 1, AnkerstoneMode.SINGLE);
                        setPowerAndAssert(helper, changing, 1, AnkerstoneMode.SINGLE);
                        assertTicketLevels(helper, chunk, List.of(31));

                        setPowerAndAssert(helper, changing, 15, AnkerstoneMode.WIDE);
                        assertTicketLevels(helper, chunk, List.of(30, 31));
                        helper.assertTrue(
                                        savedData.getOwnerModes()
                                                        .get(helper.absolutePos(steady)) == AnkerstoneMode.SINGLE,
                                        "Upgrade must preserve the other Single owner");

                        setPowerAndAssert(helper, changing, 1, AnkerstoneMode.SINGLE);
                        assertTicketLevels(helper, chunk, List.of(31));
                        helper.assertTrue(
                                        savedData.getOwnerModes()
                                                        .get(helper.absolutePos(steady)) == AnkerstoneMode.SINGLE,
                                        "Downgrade must preserve the other Single owner");

                        setPowerAndAssert(helper, changing, 0, AnkerstoneMode.OFF);
                        assertTicketLevels(helper, chunk, List.of(31));
                        helper.assertBlockProperty(steady, AnkerstoneBlock.MODE, AnkerstoneMode.SINGLE);
                        helper.assertTrue(
                                        savedData.getOwnerModes()
                                                        .get(helper.absolutePos(steady)) == AnkerstoneMode.SINGLE,
                                        "Deactivation must preserve the other Single owner");

                        helper.setBlock(changing, Blocks.AIR);
                        assertTicketLevels(helper, chunk, List.of(31));
                        helper.setBlock(steady, Blocks.AIR);
                        assertTicketLevels(helper, chunk, List.of());
                } finally {
                        helper.setBlock(steady, Blocks.AIR);
                        helper.setBlock(changing, Blocks.AIR);
                        helper.setBlock(steady.below(), Blocks.AIR);
                        helper.setBlock(changing.below(), Blocks.AIR);
                }
                helper.assertFalse(savedData.getOwnerModes().containsKey(helper.absolutePos(steady)),
                                "Removed steady owner must not remain saved");
                helper.assertFalse(savedData.getOwnerModes().containsKey(helper.absolutePos(changing)),
                                "Removed changing owner must not remain saved");
                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:ticket_ownership")
        public void wideOwnerSurvivesModeChanges(GameTestHelper helper) {
                BlockPos steady = new BlockPos(2, 2, 2);
                BlockPos changing = new BlockPos(2, 6, 2);
                ChunkPos chunk = ChunkPos.containing(helper.absolutePos(steady));
                AnkerstoneSavedData savedData = AnkerstoneSavedData.get(helper.getLevel());

                assertTicketLevels(helper, chunk, List.of());
                try {
                        helper.setBlock(steady, ModBlocks.ANKERSTONE);
                        helper.setBlock(changing, ModBlocks.ANKERSTONE);
                        setPowerAndAssert(helper, steady, 15, AnkerstoneMode.WIDE);
                        setPowerAndAssert(helper, changing, 15, AnkerstoneMode.WIDE);
                        assertTicketLevels(helper, chunk, List.of(30));

                        setPowerAndAssert(helper, changing, 1, AnkerstoneMode.SINGLE);
                        assertTicketLevels(helper, chunk, List.of(30, 31));
                        helper.assertTrue(
                                        savedData.getOwnerModes()
                                                        .get(helper.absolutePos(steady)) == AnkerstoneMode.WIDE,
                                        "Downgrade must preserve the other Wide owner");

                        setPowerAndAssert(helper, changing, 15, AnkerstoneMode.WIDE);
                        assertTicketLevels(helper, chunk, List.of(30));
                        helper.assertTrue(
                                        savedData.getOwnerModes()
                                                        .get(helper.absolutePos(steady)) == AnkerstoneMode.WIDE,
                                        "Upgrade must preserve the other Wide owner");

                        setPowerAndAssert(helper, changing, 0, AnkerstoneMode.OFF);
                        assertTicketLevels(helper, chunk, List.of(30));
                        helper.assertBlockProperty(steady, AnkerstoneBlock.MODE, AnkerstoneMode.WIDE);
                        helper.assertTrue(
                                        savedData.getOwnerModes()
                                                        .get(helper.absolutePos(steady)) == AnkerstoneMode.WIDE,
                                        "Deactivation must preserve the other Wide owner");

                        helper.setBlock(changing, Blocks.AIR);
                        assertTicketLevels(helper, chunk, List.of(30));
                        helper.setBlock(steady, Blocks.AIR);
                        assertTicketLevels(helper, chunk, List.of());
                } finally {
                        helper.setBlock(steady, Blocks.AIR);
                        helper.setBlock(changing, Blocks.AIR);
                        helper.setBlock(steady.below(), Blocks.AIR);
                        helper.setBlock(changing.below(), Blocks.AIR);
                }
                helper.assertFalse(savedData.getOwnerModes().containsKey(helper.absolutePos(steady)),
                                "Removed steady owner must not remain saved");
                helper.assertFalse(savedData.getOwnerModes().containsKey(helper.absolutePos(changing)),
                                "Removed changing owner must not remain saved");
                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:restoration")
        public void restoringSavedOwnersRebuildsSharedTickets(GameTestHelper helper) {
                ServerLevel level = helper.getLevel();
                BlockPos first = helper.absolutePos(new BlockPos(2, 2, 2));
                BlockPos second = helper.absolutePos(new BlockPos(2, 4, 2));
                BlockPos wide = helper.absolutePos(new BlockPos(2, 6, 2));
                ChunkPos chunk = ChunkPos.containing(first);
                AnkerstoneSavedData savedData = AnkerstoneSavedData.get(level);
                Map<BlockPos, AnkerstoneMode> expected = Map.of(
                                first, AnkerstoneMode.SINGLE,
                                second, AnkerstoneMode.SINGLE,
                                wide, AnkerstoneMode.WIDE);

                helper.assertTrue(savedData.getOwnerModes().isEmpty(),
                                "Restoration test requires an empty saved owner table");
                assertTicketLevels(helper, chunk, List.of());

                try {
                        expected.forEach(savedData::addOwner);
                        savedData.setDirty(false);

                        AnkerstoneChunkTickets.restore(level);
                        assertTicketLevels(helper, chunk, List.of(30, 31));
                        AnkerstoneChunkTickets.restore(level);
                        assertTicketLevels(helper, chunk, List.of(30, 31));

                        helper.assertTrue(savedData.getOwnerModes().equals(expected),
                                        "Restoration must preserve saved owners");
                        helper.assertTrue(!savedData.isDirty(),
                                        "Restoration must not mark unchanged saved data dirty");

                        AnkerstoneChunkTickets.remove(level, first, AnkerstoneMode.SINGLE);
                        assertTicketLevels(helper, chunk, List.of(30, 31));
                        AnkerstoneChunkTickets.remove(level, second, AnkerstoneMode.SINGLE);
                        assertTicketLevels(helper, chunk, List.of(30));
                        AnkerstoneChunkTickets.remove(level, wide, AnkerstoneMode.WIDE);
                        assertTicketLevels(helper, chunk, List.of());
                        helper.assertTrue(savedData.getOwnerModes().isEmpty(),
                                        "Removing all restored owners must clear their saved entries");
                } finally {
                        expected.forEach((pos, mode) -> AnkerstoneChunkTickets.remove(level, pos, mode));
                        level.getChunkSource().removeTicketWithRadius(
                                        AnkerstoneChunkTickets.ANKERSTONE_TICKET, chunk, 2);
                        level.getChunkSource().removeTicketWithRadius(
                                        AnkerstoneChunkTickets.ANKERSTONE_TICKET, chunk, 3);
                }
                helper.succeed();
        }

        @GameTest(maxTicks = 40)
        public void observerDetectsActivation(GameTestHelper helper) {
                assertObserverTransition(helper, 0, 1, AnkerstoneMode.SINGLE);
        }

        @GameTest(maxTicks = 40)
        public void observerDetectsUpgradeToWide(GameTestHelper helper) {
                assertObserverTransition(helper, 1, 15, AnkerstoneMode.WIDE);
        }

        @GameTest(maxTicks = 40)
        public void observerDetectsDeactivation(GameTestHelper helper) {
                assertObserverTransition(helper, 15, 0, AnkerstoneMode.OFF);
        }

        @GameTest(maxTicks = 40)
        public void comparatorDetectsActivation(GameTestHelper helper) {
                assertComparatorTransition(helper, 0, 1, 7);
        }

        @GameTest(maxTicks = 40)
        public void comparatorDetectsUpgradeToWide(GameTestHelper helper) {
                assertComparatorTransition(helper, 1, 15, 15);
        }

        @GameTest(maxTicks = 40)
        public void comparatorDetectsDowngradeToSingle(GameTestHelper helper) {
                assertComparatorTransition(helper, 15, 1, 7);
        }

        @GameTest(maxTicks = 40)
        public void comparatorDetectsDeactivation(GameTestHelper helper) {
                assertComparatorTransition(helper, 1, 0, 0);
        }

        private void assertObserverTransition(GameTestHelper helper,
                        int initialPower, int targetPower,
                        AnkerstoneMode targetMode) {
                BlockPos anchor = new BlockPos(3, 2, 2);
                BlockPos observer = anchor.west();
                BlockPos lamp = observer.west();
                Runnable cleanup = () -> {
                        helper.setBlock(observer, Blocks.AIR);
                        helper.setBlock(lamp, Blocks.AIR);
                        helper.setBlock(anchor, Blocks.AIR);
                        helper.setBlock(anchor.below(), Blocks.AIR);
                };
                try {
                        setTestPower(helper, anchor, initialPower);
                        helper.setBlock(anchor, ModBlocks.ANKERSTONE);
                        helper.setBlock(observer, Blocks.OBSERVER.defaultBlockState()
                                        .setValue(ObserverBlock.FACING, Direction.EAST));
                        helper.setBlock(lamp, Blocks.REDSTONE_LAMP);

                        helper.runAfterDelay(10, () -> {
                                try {
                                        helper.assertBlockProperty(observer,
                                                        ObserverBlock.POWERED, false);
                                        helper.assertBlockProperty(lamp,
                                                        RedstoneLampBlock.LIT, false);
                                        setPowerAndAssert(helper, anchor,
                                                        targetPower, targetMode);

                                        helper.runAfterDelay(3, () -> {
                                                try {
                                                        helper.assertBlockProperty(observer,
                                                                        ObserverBlock.POWERED, true);
                                                        helper.assertBlockProperty(lamp,
                                                                        RedstoneLampBlock.LIT, true);
                                                } finally {
                                                        cleanup.run();
                                                }
                                                helper.succeed();
                                        });
                                } catch (RuntimeException failure) {
                                        cleanup.run();
                                        throw failure;
                                }
                        });
                } catch (RuntimeException failure) {
                        cleanup.run();
                        throw failure;
                }
        }

        private void setPowerAndAssert(GameTestHelper helper, BlockPos localPos,
                        int power, AnkerstoneMode expectedMode) {
                setTestPower(helper, localPos, power);
                helper.assertBlockProperty(localPos, AnkerstoneBlock.MODE, expectedMode);

                AnkerstoneMode actualOwner = AnkerstoneSavedData.get(helper.getLevel())
                                .getOwnerModes().get(helper.absolutePos(localPos));
                AnkerstoneMode expectedOwner = expectedMode == AnkerstoneMode.OFF
                                ? null
                                : expectedMode;

                helper.assertTrue(actualOwner == expectedOwner,
                                "Power " + power + " expected owner " + expectedOwner
                                                + ", got " + actualOwner);
                BlockState state = helper.getBlockState(localPos);
                int expectedOutput = switch (expectedMode) {
                        case OFF -> 0;
                        case SINGLE -> 7;
                        case WIDE -> 15;
                };
                helper.assertTrue(state.hasAnalogOutputSignal(),
                                "Ankerstone must advertise a comparator readout");
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                        int actual = state.getAnalogOutputSignal(helper.getLevel(),
                                        helper.absolutePos(localPos), direction);
                        helper.assertTrue(actual == expectedOutput,
                                        "Power " + power + ", direction " + direction
                                                        + " expected readout " + expectedOutput + ", got " + actual);
                }
        }

        @GameTest(environment = "ankerstone-test:restoration")
        public void changingInputPositionDoesNotPreventTicketRemoval(
                        GameTestHelper helper) {
                ServerLevel level = helper.getLevel();
                BlockPos originalPosition = helper.absolutePos(
                                new BlockPos(2, 2, 2)).immutable();
                BlockPos.MutableBlockPos cursor = originalPosition.mutable();
                ChunkPos chunk = ChunkPos.containing(originalPosition);
                AnkerstoneSavedData savedData = AnkerstoneSavedData.get(level);

                assertTicketCount(helper, chunk, 0);
                try {
                        AnkerstoneChunkTickets.add(
                                        level, cursor, AnkerstoneMode.WIDE);
                        assertTicketCount(helper, chunk, 1);

                        cursor.set(originalPosition.getX() + 32,
                                        originalPosition.getY(),
                                        originalPosition.getZ());

                        AnkerstoneChunkTickets.remove(
                                        level, originalPosition, AnkerstoneMode.WIDE);

                        helper.assertFalse(
                                        savedData.getOwnerModes().containsKey(originalPosition),
                                        "Removal must clear the saved owner");
                        assertTicketCount(helper, chunk, 0);
                } finally {
                        // Restore the key so cleanup also works before the fix.
                        cursor.set(originalPosition);
                        AnkerstoneChunkTickets.remove(
                                        level, originalPosition, AnkerstoneMode.WIDE);
                }
                helper.succeed();
        }

        private void setTestPower(GameTestHelper helper,
                        BlockPos anchor, int power) {
                helper.setBlock(anchor.below(),
                                Blocks.DAYLIGHT_DETECTOR.defaultBlockState()
                                                .setValue(DaylightDetectorBlock.POWER, power));
                // Keep the chosen signal instead of recalculating it from sunlight.
                helper.getLevel().removeBlockEntity(
                                helper.absolutePos(anchor.below()));
        }

        private void assertTicketCount(GameTestHelper helper, ChunkPos chunk, int expected) {
                TicketStorage storage = helper.getLevel().getChunkSource()
                                .getDataStorage().computeIfAbsent(TicketStorage.TYPE);
                long actual = storage.getTickets(chunk.pack()).stream()
                                .filter(ticket -> ticket.getType() == AnkerstoneChunkTickets.ANKERSTONE_TICKET)
                                .count();

                helper.assertTrue(actual == expected,
                                "Expected " + expected + " Ankerstone tickets, got " + actual);
        }

        private void assertTicketLevels(GameTestHelper helper, ChunkPos chunk,
                        List<Integer> expected) {
                TicketStorage storage = helper.getLevel().getChunkSource()
                                .getDataStorage().computeIfAbsent(TicketStorage.TYPE);
                List<Integer> actual = storage.getTickets(chunk.pack()).stream()
                                .filter(ticket -> ticket.getType() == AnkerstoneChunkTickets.ANKERSTONE_TICKET)
                                .map(ticket -> ticket.getTicketLevel())
                                .sorted()
                                .toList();

                helper.assertTrue(actual.equals(expected),
                                "Expected ticket levels " + expected + ", got " + actual);
        }

        private void assertComparatorTransition(GameTestHelper helper,
                        int initialPower, int targetPower, int expectedOutput) {
                BlockPos anchor = new BlockPos(3, 2, 2);
                BlockPos comparator = anchor.west();
                BlockPos output = comparator.west();
                int initialOutput = initialPower == 0 ? 0 : initialPower == 15 ? 15 : 7;
                List<BlockPos> fixture = List.of(comparator, output, anchor,
                                comparator.below(), output.below(), anchor.below());
                Runnable cleanup = () -> fixture.forEach(
                                pos -> helper.setBlock(pos, Blocks.AIR));
                try {
                        helper.setBlock(comparator.below(), Blocks.STONE);
                        helper.setBlock(output.below(), Blocks.STONE);
                        setTestPower(helper, anchor, initialPower);
                        helper.setBlock(anchor, ModBlocks.ANKERSTONE);
                        helper.setBlock(comparator, Blocks.COMPARATOR.defaultBlockState()
                                        .setValue(ComparatorBlock.FACING, Direction.EAST));
                        helper.setBlock(output, Blocks.REDSTONE_WIRE);

                        helper.runAfterDelay(3, () -> {
                                try {
                                        helper.assertBlockProperty(output,
                                                        RedStoneWireBlock.POWER, initialOutput);
                                        setTestPower(helper, anchor, targetPower);
                                        helper.runAfterDelay(3, () -> {
                                                try {
                                                        helper.assertBlockProperty(output,
                                                                        RedStoneWireBlock.POWER,
                                                                        expectedOutput);
                                                } finally {
                                                        cleanup.run();
                                                }
                                                helper.succeed();
                                        });
                                } catch (RuntimeException failure) {
                                        cleanup.run();
                                        throw failure;
                                }
                        });
                } catch (RuntimeException failure) {
                        cleanup.run();
                        throw failure;
                }
        }
}
