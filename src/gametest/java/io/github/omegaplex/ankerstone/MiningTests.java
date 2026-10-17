package io.github.omegaplex.ankerstone;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.BlockState;

public class MiningTests {
    @GameTest
    public void toolsRespectMiningTiers(GameTestHelper helper) {
        for (Block block : List.of(
                ModBlocks.STILLSTONE, ModBlocks.STILLSTONE_SLAB,
                ModBlocks.STILLSTONE_STAIRS, ModBlocks.STILLSTONE_WALL)) {
            assertTools(helper, block, false);
        }
        for (Block block : List.of(
                ModBlocks.ANKERSTONE, ModBlocks.REINFORCED_STILLSTONE,
                ModBlocks.REINFORCED_STILLSTONE_SLAB,
                ModBlocks.REINFORCED_STILLSTONE_STAIRS,
                ModBlocks.REINFORCED_STILLSTONE_WALL)) {
            assertTools(helper, block, true);
        }
        helper.succeed();
    }

    @GameTest
    public void blocksDropTheirOwnItems(GameTestHelper helper) {
        for (Block block : List.of(
                ModBlocks.STILLSTONE, ModBlocks.STILLSTONE_STAIRS,
                ModBlocks.STILLSTONE_WALL, ModBlocks.REINFORCED_STILLSTONE,
                ModBlocks.REINFORCED_STILLSTONE_STAIRS,
                ModBlocks.REINFORCED_STILLSTONE_WALL)) {
            assertDrops(helper, block.defaultBlockState(), 1);
        }
        for (AnkerstoneMode mode : AnkerstoneMode.values()) {
            BlockState state = ModBlocks.ANKERSTONE.defaultBlockState()
                    .setValue(AnkerstoneBlock.MODE, mode);
            assertDrops(helper, state, 1);
        }
        helper.succeed();
    }

    @GameTest
    public void slabsDropOneOrTwoItems(GameTestHelper helper) {
        for (Block slab : List.of(
                ModBlocks.STILLSTONE_SLAB, ModBlocks.REINFORCED_STILLSTONE_SLAB)) {
            for (SlabType type : SlabType.values()) {
                BlockState state = slab.defaultBlockState().setValue(SlabBlock.TYPE, type);
                assertDrops(helper, state, type == SlabType.DOUBLE ? 2 : 1);
            }
        }
        helper.succeed();
    }

    private void assertDrops(GameTestHelper helper, BlockState state, int expectedCount) {
        List<ItemStack> drops = Block.getDrops(state, helper.getLevel(),
                helper.absolutePos(new BlockPos(2, 2, 2)), null, null,
                new ItemStack(Items.NETHERITE_PICKAXE));
        helper.assertTrue(drops.size() == 1,
                "Expected one stack for " + state + ", got " + drops);
        ItemStack drop = drops.getFirst();
        helper.assertTrue(drop.getItem() == state.getBlock().asItem(),
                "Wrong dropped item for " + state + ": " + drop);
        helper.assertTrue(drop.getCount() == expectedCount,
                state + ": expected " + expectedCount + " items, got " + drop.getCount());
    }

    private void assertTools(GameTestHelper helper, Block block, boolean ironRequired) {
        BlockState state = block.defaultBlockState();
        helper.assertTrue(state.requiresCorrectToolForDrops(),
                block + " must require a correct tool");
        for (Item tool : List.of(Items.WOODEN_PICKAXE, Items.COPPER_PICKAXE,
                Items.STONE_PICKAXE, Items.GOLDEN_PICKAXE)) {
            assertTool(helper, state, tool, !ironRequired);
        }
        for (Item tool : List.of(Items.IRON_PICKAXE,
                Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE)) {
            assertTool(helper, state, tool, true);
        }
        assertTool(helper, state, Items.IRON_AXE, false);
        helper.assertTrue(!ItemStack.EMPTY.isCorrectToolForDrops(state),
                "Empty hands must not qualify for " + block);
    }

    private void assertTool(GameTestHelper helper, BlockState state,
            Item tool, boolean expected) {
        boolean actual = new ItemStack(tool).isCorrectToolForDrops(state);
        helper.assertTrue(actual == expected,
                tool + " on " + state + ": expected " + expected + ", got " + actual);
    }
}