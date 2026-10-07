package io.github.omegaplex.ankerstone;

import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.state.BlockState;

public class AnkerstonePlacementGameTests {
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

    private void setPowerAndAssert(GameTestHelper helper, BlockPos localPos,
            int power, AnkerstoneMode expectedMode) {
        helper.setBlock(localPos.below(), Blocks.DAYLIGHT_DETECTOR.defaultBlockState()
                .setValue(DaylightDetectorBlock.POWER, power));
        helper.assertBlockProperty(localPos, AnkerstoneBlock.MODE, expectedMode);

        AnkerstoneMode actualOwner = AnkerstoneSavedData.get(helper.getLevel())
                .getOwnerModes().get(helper.absolutePos(localPos));
        AnkerstoneMode expectedOwner = expectedMode == AnkerstoneMode.OFF
                ? null
                : expectedMode;

        helper.assertTrue(actualOwner == expectedOwner,
                "Power " + power + " expected owner " + expectedOwner
                        + ", got " + actualOwner);
    }

    @GameTest
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

    private void assertTicketCount(GameTestHelper helper, ChunkPos chunk, int expected) {
        TicketStorage storage = helper.getLevel().getChunkSource()
                .getDataStorage().computeIfAbsent(TicketStorage.TYPE);
        long actual = storage.getTickets(chunk.pack()).stream()
                .filter(ticket -> ticket.getType() == AnkerstoneChunkTickets.ANKERSTONE_TICKET)
                .count();

        helper.assertTrue(actual == expected,
                "Expected " + expected + " Ankerstone tickets, got " + actual);
    }

    @GameTest
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

    @GameTest
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
}
