package io.github.omegaplex.ankerstone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;

public class PlacementTests {
        @GameTest
        public void alreadyWidePlacementRegistersAndReleasesOwner(GameTestHelper helper) {
                BlockPos localPos = new BlockPos(2, 2, 2);
                BlockPos ownerPos = helper.absolutePos(localPos);
                OwnerData savedData = OwnerData.get(helper.getLevel());
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
        public void negativeOwnerUsesFloorDivision(GameTestHelper helper) {
                ServerLevel level = helper.getLevel();
                BlockPos owner = new BlockPos(-1, 64, -17);
                ChunkPos expected = new ChunkPos(-1, -2);
                ChunkPos truncated = new ChunkPos(0, -1);

                assertTicketLevels(helper, expected, List.of());
                assertTicketLevels(helper, truncated, List.of());

                try {
                        ChunkTickets.add(
                                        level, owner, AnkerstoneMode.WIDE);

                        assertTicketLevels(helper, expected, List.of(30));
                        assertTicketLevels(helper, truncated, List.of());
                } finally {
                        ChunkTickets.remove(
                                        level, owner, AnkerstoneMode.WIDE);
                        level.getChunkSource().removeTicketWithRadius(
                                        ChunkTickets.ANKERSTONE_TICKET,
                                        expected, 3);
                        level.getChunkSource().removeTicketWithRadius(
                                        ChunkTickets.ANKERSTONE_TICKET,
                                        truncated, 3);
                }

                assertTicketLevels(helper, expected, List.of());
                assertTicketLevels(helper, truncated, List.of());
                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:ticket_ownership")
        public void netherOwnerStaysInItsDimension(GameTestHelper helper) {
                ServerLevel overworld = helper.getLevel().getServer().overworld();
                ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
                helper.assertTrue(nether != null, "Nether must be available");
                BlockPos owner = helper.absolutePos(new BlockPos(2, 2, 2));
                ChunkPos chunk = ChunkPos.containing(owner);
                OwnerData overworldData = OwnerData.get(overworld);
                OwnerData netherData = OwnerData.get(nether);

                helper.assertFalse(
                                overworldData.getOwnerModes().containsKey(owner)
                                                || netherData.getOwnerModes().containsKey(owner),
                                "Fixture position must start without a saved owner");
                assertTicketLevels(helper, overworld, chunk, List.of());
                assertTicketLevels(helper, nether, chunk, List.of());
                try {
                        ChunkTickets.add(nether, owner, AnkerstoneMode.WIDE);
                        helper.assertTrue(
                                        netherData.getOwnerModes().get(owner) == AnkerstoneMode.WIDE,
                                        "Nether must store its owner");
                        helper.assertFalse(overworldData.getOwnerModes().containsKey(owner),
                                        "Nether owner must not appear in Overworld data");
                        assertTicketLevels(helper, nether, chunk, List.of(30));
                        assertTicketLevels(helper, overworld, chunk, List.of());

                        ChunkTickets.remove(nether, owner, AnkerstoneMode.WIDE);
                        helper.assertFalse(netherData.getOwnerModes().containsKey(owner),
                                        "Removing the Nether owner must clear its saved entry");
                        assertTicketLevels(helper, nether, chunk, List.of());
                        assertTicketLevels(helper, overworld, chunk, List.of());
                } finally {
                        ChunkTickets.remove(nether, owner, AnkerstoneMode.WIDE);
                        nether.getChunkSource().removeTicketWithRadius(
                                        ChunkTickets.ANKERSTONE_TICKET, chunk, 3);
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
                OwnerData savedData = OwnerData.get(helper.getLevel());

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
                OwnerData savedData = OwnerData.get(helper.getLevel());

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
        public void levelEventsRebuildTickets(GameTestHelper helper) {
                ServerLevel level = helper.getLevel();
                BlockPos owner = helper.absolutePos(new BlockPos(2, 2, 2));
                ChunkPos chunk = ChunkPos.containing(owner);
                OwnerData savedData = OwnerData.get(level);

                helper.assertTrue(savedData.getOwnerModes().isEmpty(),
                                "Lifecycle test requires an empty saved owner table");
                assertTicketLevels(helper, chunk, List.of());

                try {
                        savedData.addOwner(owner, AnkerstoneMode.WIDE);

                        ServerLevelEvents.LOAD.invoker()
                                        .onLevelLoad(level.getServer(), level);
                        assertTicketLevels(helper, chunk, List.of(30));

                        // Remove the ticket without clearing the runtime owner.
                        level.getChunkSource().removeTicketWithRadius(
                                        ChunkTickets.ANKERSTONE_TICKET,
                                        chunk, 3);
                        assertTicketLevels(helper, chunk, List.of());

                        ServerLevelEvents.UNLOAD.invoker()
                                        .onLevelUnload(level.getServer(), level);
                        helper.assertTrue(
                                        savedData.getOwnerModes().get(owner) == AnkerstoneMode.WIDE,
                                        "Unloading must preserve the saved owner");

                        ServerLevelEvents.LOAD.invoker()
                                        .onLevelLoad(level.getServer(), level);
                        assertTicketLevels(helper, chunk, List.of(30));

                        ChunkTickets.remove(
                                        level, owner, AnkerstoneMode.WIDE);
                        assertTicketLevels(helper, chunk, List.of());
                } finally {
                        ChunkTickets.remove(
                                        level, owner, AnkerstoneMode.WIDE);
                        level.getChunkSource().removeTicketWithRadius(
                                        ChunkTickets.ANKERSTONE_TICKET,
                                        chunk, 2);
                        level.getChunkSource().removeTicketWithRadius(
                                        ChunkTickets.ANKERSTONE_TICKET,
                                        chunk, 3);
                }
                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:restoration")
        public void restoringSavedOwnersRebuildsSharedTickets(GameTestHelper helper) {
                ServerLevel level = helper.getLevel();
                BlockPos first = helper.absolutePos(new BlockPos(2, 2, 2));
                BlockPos second = helper.absolutePos(new BlockPos(2, 4, 2));
                BlockPos wide = helper.absolutePos(new BlockPos(2, 6, 2));
                ChunkPos chunk = ChunkPos.containing(first);
                OwnerData savedData = OwnerData.get(level);
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

                        ChunkTickets.restore(level);
                        assertTicketLevels(helper, chunk, List.of(30, 31));
                        ChunkTickets.restore(level);
                        assertTicketLevels(helper, chunk, List.of(30, 31));

                        helper.assertTrue(savedData.getOwnerModes().equals(expected),
                                        "Restoration must preserve saved owners");
                        helper.assertTrue(!savedData.isDirty(),
                                        "Restoration must not mark unchanged saved data dirty");

                        ChunkTickets.remove(level, first, AnkerstoneMode.SINGLE);
                        assertTicketLevels(helper, chunk, List.of(30, 31));
                        ChunkTickets.remove(level, second, AnkerstoneMode.SINGLE);
                        assertTicketLevels(helper, chunk, List.of(30));
                        ChunkTickets.remove(level, wide, AnkerstoneMode.WIDE);
                        assertTicketLevels(helper, chunk, List.of());
                        helper.assertTrue(savedData.getOwnerModes().isEmpty(),
                                        "Removing all restored owners must clear their saved entries");
                } finally {
                        expected.forEach((pos, mode) -> ChunkTickets.remove(level, pos, mode));
                        level.getChunkSource().removeTicketWithRadius(
                                        ChunkTickets.ANKERSTONE_TICKET, chunk, 2);
                        level.getChunkSource().removeTicketWithRadius(
                                        ChunkTickets.ANKERSTONE_TICKET, chunk, 3);
                }
                helper.succeed();
        }

        @GameTest
        public void stillstoneConductionMatchesFamily(GameTestHelper helper) {
                ServerLevel level = helper.getLevel();
                BlockPos position = helper.absolutePos(new BlockPos(2, 2, 2));

                helper.assertFalse(
                                ModBlocks.STILLSTONE.defaultBlockState()
                                                .isRedstoneConductor(level, position),
                                "Stillstone full blocks must not conduct redstone");
                helper.assertTrue(
                                ModBlocks.REINFORCED_STILLSTONE.defaultBlockState()
                                                .isRedstoneConductor(level, position),
                                "Reinforced Stillstone full blocks must conduct redstone");

                helper.succeed();
        }

        @GameTest(structure = "ankerstone-test:chunk_boundary", maxTicks = 40)
        public void fullBlocksTransmitExpectedPower(GameTestHelper helper) {
                List<BlockPos> repeaters = List.of(
                                new BlockPos(3, 2, 2),
                                new BlockPos(3, 2, 6),
                                new BlockPos(3, 2, 10));
                List<BlockState> materials = List.of(
                                Blocks.STONE.defaultBlockState(),
                                ModBlocks.STILLSTONE.defaultBlockState(),
                                ModBlocks.REINFORCED_STILLSTONE.defaultBlockState());
                List<BlockPos> fixture = new ArrayList<>();
                Runnable cleanup = () -> fixture.reversed().forEach(
                                pos -> helper.setBlock(pos, Blocks.AIR));
                try {
                        for (int i = 0; i < repeaters.size(); i++) {
                                BlockPos repeater = repeaters.get(i);
                                for (int offset = -1; offset <= 2; offset++) {
                                        BlockPos part = repeater.offset(offset, 0, 0);
                                        fixture.add(part.below());
                                        fixture.add(part);
                                        helper.setBlock(part.below(), Blocks.GLASS);
                                }
                                helper.setBlock(repeater, Blocks.REPEATER.defaultBlockState()
                                                .setValue(RepeaterBlock.FACING, Direction.WEST));
                                helper.setBlock(repeater.east(), materials.get(i));
                                helper.setBlock(repeater.east(2), Blocks.REDSTONE_WIRE);
                                helper.assertBlockProperty(repeater.east(2),
                                                RedstoneWireBlock.POWER, 0);
                        }
                        repeaters.forEach(pos -> helper.setBlock(pos.west(), Blocks.REDSTONE_BLOCK));
                        long deadline = helper.getTick() + 20;
                        boolean[] finished = { false };
                        helper.onEachTick(() -> {
                                if (finished[0]) {
                                        return;
                                }
                                try {
                                        boolean ready = repeaters.stream().allMatch(pos -> helper.getBlockState(pos)
                                                        .getValue(RepeaterBlock.POWERED));
                                        if (!ready && helper.getTick() < deadline) {
                                                return;
                                        }
                                        finished[0] = true;
                                        try {
                                                for (int i = 0; i < repeaters.size(); i++) {
                                                        BlockPos repeater = repeaters.get(i);
                                                        helper.assertBlockProperty(repeater,
                                                                        RepeaterBlock.POWERED, true);
                                                        helper.assertBlockProperty(repeater.east(2),
                                                                        RedstoneWireBlock.POWER,
                                                                        i == 1 ? 0 : 15);
                                                }
                                        } finally {
                                                cleanup.run();
                                        }
                                        helper.succeed();
                                } catch (RuntimeException failure) {
                                        finished[0] = true;
                                        cleanup.run();
                                        throw failure;
                                }
                        });
                } catch (RuntimeException failure) {
                        cleanup.run();
                        throw failure;
                }
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

                AnkerstoneMode actualOwner = OwnerData.get(helper.getLevel())
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
                OwnerData savedData = OwnerData.get(level);

                assertTicketCount(helper, chunk, 0);
                try {
                        ChunkTickets.add(
                                        level, cursor, AnkerstoneMode.WIDE);
                        assertTicketCount(helper, chunk, 1);

                        cursor.set(originalPosition.getX() + 32,
                                        originalPosition.getY(),
                                        originalPosition.getZ());

                        ChunkTickets.remove(
                                        level, originalPosition, AnkerstoneMode.WIDE);

                        helper.assertFalse(
                                        savedData.getOwnerModes().containsKey(originalPosition),
                                        "Removal must clear the saved owner");
                        assertTicketCount(helper, chunk, 0);
                } finally {
                        // Restore the key so cleanup also works before the fix.
                        cursor.set(originalPosition);
                        ChunkTickets.remove(
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
                                .filter(ticket -> ticket.getType() == ChunkTickets.ANKERSTONE_TICKET)
                                .count();

                helper.assertTrue(actual == expected,
                                "Expected " + expected + " Ankerstone tickets, got " + actual);
        }

        private void assertTicketLevels(GameTestHelper helper, ChunkPos chunk,
                        List<Integer> expected) {
                assertTicketLevels(helper, helper.getLevel(), chunk, expected);
        }

        private void assertTicketLevels(GameTestHelper helper, ServerLevel level,
                        ChunkPos chunk, List<Integer> expected) {
                TicketStorage storage = level.getChunkSource()
                                .getDataStorage().computeIfAbsent(TicketStorage.TYPE);
                List<Integer> actual = storage.getTickets(chunk.pack()).stream()
                                .filter(ticket -> ticket.getType() == ChunkTickets.ANKERSTONE_TICKET)
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
                                                        RedstoneWireBlock.POWER, initialOutput);
                                        setTestPower(helper, anchor, targetPower);
                                        helper.runAfterDelay(3, () -> {
                                                try {
                                                        helper.assertBlockProperty(output,
                                                                        RedstoneWireBlock.POWER,
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
