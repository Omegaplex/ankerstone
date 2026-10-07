package io.github.omegaplex.ankerstone;

import java.util.Arrays;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

public class AnkerstoneCraftingGameTests {
    @GameTest
    public void recipesRejectWrongLayouts(GameTestHelper helper) {
        Item cobbled = Items.COBBLED_DEEPSLATE;
        Item lapis = Items.LAPIS_LAZULI;
        Item iron = Items.IRON_NUGGET;
        Item stillstone = ModBlocks.STILLSTONE.asItem();
        Item reinforced = ModBlocks.REINFORCED_STILLSTONE.asItem();
        Item dust = Items.REDSTONE;

        assertNoRecipe(helper, "Stillstone requires the checkerboard layout", grid(
                cobbled, lapis, cobbled,
                lapis, lapis, cobbled,
                cobbled, lapis, cobbled));
        assertNoRecipe(helper, "Reinforced Stillstone requires stone in the center", grid(
                iron, iron, iron,
                stillstone, iron, iron,
                iron, iron, iron));
        assertNoRecipe(helper, "Ankerstone requires the pearl in the center", grid(
                reinforced, Items.ENDER_PEARL, reinforced,
                dust, dust, dust,
                reinforced, dust, reinforced));
        helper.succeed();
    }

    @GameTest
    public void reinforcedStillstoneRecipeMatches(GameTestHelper helper) {
        Item iron = Items.IRON_NUGGET;
        Item stone = ModBlocks.STILLSTONE.asItem();
        CraftingInput input = grid(
                iron, iron, iron,
                iron, stone, iron,
                iron, iron, iron);
        assertRecipe(helper, "reinforced_stillstone", input,
                ModBlocks.REINFORCED_STILLSTONE.asItem(), 1);
        helper.succeed();
    }

    @GameTest
    public void ankerstoneRecipeMatches(GameTestHelper helper) {
        Item stone = ModBlocks.REINFORCED_STILLSTONE.asItem();
        Item dust = Items.REDSTONE;
        CraftingInput input = grid(
                stone, dust, stone,
                dust, Items.ENDER_PEARL, dust,
                stone, dust, stone);
        assertRecipe(helper, "ankerstone", input, ModBlocks.ANKERSTONE.asItem(), 1);
        helper.succeed();
    }

    @GameTest
    public void recipesRejectWrongIngredients(GameTestHelper helper) {
        Item raw = Items.DEEPSLATE;
        Item lapis = Items.LAPIS_LAZULI;
        Item ingot = Items.IRON_INGOT;
        Item stillstone = ModBlocks.STILLSTONE.asItem();
        Item reinforced = ModBlocks.REINFORCED_STILLSTONE.asItem();
        Item dust = Items.REDSTONE;

        assertNoRecipe(helper, "Raw deepslate must not substitute for cobbled", grid(
                raw, lapis, raw,
                lapis, raw, lapis,
                raw, lapis, raw));
        assertNoRecipe(helper, "Iron ingots must not substitute for nuggets", grid(
                ingot, ingot, ingot,
                ingot, stillstone, ingot,
                ingot, ingot, ingot));
        assertNoRecipe(helper, "An eye of ender must not substitute for a pearl", grid(
                reinforced, dust, reinforced,
                dust, Items.ENDER_EYE, dust,
                reinforced, dust, reinforced));
        helper.succeed();
    }

    private void assertNoRecipe(GameTestHelper helper, String message,
            CraftingInput input) {
        helper.assertTrue(helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).isEmpty(),
                message);
    }

    @GameTest
    public void stillstoneRecipeMatches(GameTestHelper helper) {
        CraftingInput input = grid(
                Items.COBBLED_DEEPSLATE, Items.LAPIS_LAZULI, Items.COBBLED_DEEPSLATE,
                Items.LAPIS_LAZULI, Items.COBBLED_DEEPSLATE, Items.LAPIS_LAZULI,
                Items.COBBLED_DEEPSLATE, Items.LAPIS_LAZULI, Items.COBBLED_DEEPSLATE);
        assertRecipe(helper, "stillstone", input, ModBlocks.STILLSTONE.asItem(), 5);
        helper.succeed();
    }

    private CraftingInput grid(Item... items) {
        return CraftingInput.of(3, 3,
                Arrays.stream(items).map(ItemStack::new).toList());
    }

    private void assertRecipe(GameTestHelper helper, String name,
            CraftingInput input, Item expectedItem, int expectedCount) {
        RecipeHolder<CraftingRecipe> recipe = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow(() -> new AssertionError("No recipe matched " + name));
        Identifier expectedId = Identifier.fromNamespaceAndPath("ankerstone", name);
        helper.assertTrue(recipe.id().identifier().equals(expectedId),
                "Wrong recipe for " + name + ": " + recipe.id());

        ItemStack result = recipe.value().assemble(input);
        helper.assertTrue(result.getItem() == expectedItem,
                "Wrong output for " + name + ": " + result);
        helper.assertTrue(result.getCount() == expectedCount,
                name + ": expected " + expectedCount + " items, got " + result.getCount());
    }
}
