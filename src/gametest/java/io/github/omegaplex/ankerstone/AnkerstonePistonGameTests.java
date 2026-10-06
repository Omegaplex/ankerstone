package io.github.omegaplex.ankerstone;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
}