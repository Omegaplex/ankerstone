package io.github.omegaplex.ankerstone;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
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
}
