package io.github.omegaplex.ankerstone;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
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
}
