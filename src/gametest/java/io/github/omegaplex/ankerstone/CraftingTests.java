package io.github.omegaplex.ankerstone;

import java.util.Arrays;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CraftingTests {
        @GameTest
        public void stillstoneRecipeMatches(GameTestHelper helper) {
                CraftingInput input = grid(
                                Items.COBBLED_DEEPSLATE, Items.LAPIS_LAZULI, Items.COBBLED_DEEPSLATE,
                                Items.LAPIS_LAZULI, Items.COBBLED_DEEPSLATE, Items.LAPIS_LAZULI,
                                Items.COBBLED_DEEPSLATE, Items.LAPIS_LAZULI, Items.COBBLED_DEEPSLATE);
                assertRecipe(helper, "stillstone", input, ModBlocks.STILLSTONE.asItem(), 5);
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
        public void stillstoneVariantsMatch(GameTestHelper helper) {
                assertVariantRecipes(helper, "stillstone",
                                ModBlocks.STILLSTONE.asItem(), ModBlocks.STILLSTONE_SLAB.asItem(),
                                ModBlocks.STILLSTONE_STAIRS.asItem(), ModBlocks.STILLSTONE_WALL.asItem());
                helper.succeed();
        }

        @GameTest
        public void reinforcedStillstoneVariantsMatch(GameTestHelper helper) {
                assertVariantRecipes(helper, "reinforced_stillstone",
                                ModBlocks.REINFORCED_STILLSTONE.asItem(),
                                ModBlocks.REINFORCED_STILLSTONE_SLAB.asItem(),
                                ModBlocks.REINFORCED_STILLSTONE_STAIRS.asItem(),
                                ModBlocks.REINFORCED_STILLSTONE_WALL.asItem());
                helper.succeed();
        }

        @GameTest
        public void variantsRejectMixedFamilies(GameTestHelper helper) {
                Item stillstone = ModBlocks.STILLSTONE.asItem();
                Item reinforced = ModBlocks.REINFORCED_STILLSTONE.asItem();
                Item empty = Items.AIR;

                for (Item base : new Item[] { stillstone, reinforced }) {
                        Item other = base == stillstone ? reinforced : stillstone;
                        String family = base == stillstone ? "Stillstone" : "Reinforced Stillstone";

                        assertNoRecipe(helper, family + " slabs must reject mixed blocks", grid(
                                        base, other, base,
                                        empty, empty, empty,
                                        empty, empty, empty));
                        assertNoRecipe(helper, family + " stairs must reject mixed blocks", grid(
                                        base, empty, empty,
                                        other, base, empty,
                                        base, base, base));
                        assertNoRecipe(helper, family + " walls must reject mixed blocks", grid(
                                        base, base, base,
                                        other, base, base,
                                        empty, empty, empty));
                }
                helper.succeed();
        }

        @GameTest
        public void variantsRejectWrongLayouts(GameTestHelper helper) {
                Item stillstone = ModBlocks.STILLSTONE.asItem();
                Item reinforced = ModBlocks.REINFORCED_STILLSTONE.asItem();
                Item empty = Items.AIR;

                for (Item base : new Item[] { stillstone, reinforced }) {
                        String family = base == stillstone ? "Stillstone" : "Reinforced Stillstone";

                        assertNoRecipe(helper, family + " slabs require a horizontal row", grid(
                                        base, empty, empty,
                                        base, empty, empty,
                                        base, empty, empty));

                        assertNoRecipe(helper, family + " requires a valid six-block pattern", grid(
                                        base, base, empty,
                                        base, empty, base,
                                        empty, base, base));

                        assertNoRecipe(helper, family + " slabs must reject extra ingredients", grid(
                                        base, base, base,
                                        empty, base, empty,
                                        empty, empty, empty));
                }
                helper.succeed();
        }

        @GameTest
        public void stonecuttingRecipesMatch(GameTestHelper helper) {
                Item stone = ModBlocks.STILLSTONE.asItem();
                Item reinforced = ModBlocks.REINFORCED_STILLSTONE.asItem();

                assertStonecuttingRecipe(helper, "stillstone_slab_from_stonecutting", stone,
                                ModBlocks.STILLSTONE_SLAB.asItem(), 2);
                assertStonecuttingRecipe(helper, "stillstone_stairs_from_stonecutting", stone,
                                ModBlocks.STILLSTONE_STAIRS.asItem(), 1);
                assertStonecuttingRecipe(helper, "stillstone_wall_from_stonecutting", stone,
                                ModBlocks.STILLSTONE_WALL.asItem(), 1);
                assertStonecuttingRecipe(helper, "reinforced_stillstone_slab_from_stonecutting", reinforced,
                                ModBlocks.REINFORCED_STILLSTONE_SLAB.asItem(), 2);
                assertStonecuttingRecipe(helper, "reinforced_stillstone_stairs_from_stonecutting", reinforced,
                                ModBlocks.REINFORCED_STILLSTONE_STAIRS.asItem(), 1);
                assertStonecuttingRecipe(helper, "reinforced_stillstone_wall_from_stonecutting", reinforced,
                                ModBlocks.REINFORCED_STILLSTONE_WALL.asItem(), 1);
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
                                .orElseThrow(() -> helper.assertionException("No recipe matched " + name));
                Identifier expectedId = Identifier.fromNamespaceAndPath("ankerstone", name);
                helper.assertTrue(recipe.id().identifier().equals(expectedId),
                                "Wrong recipe for " + name + ": " + recipe.id());

                ItemStack result = recipe.value().assemble(input);
                helper.assertTrue(result.getItem() == expectedItem,
                                "Wrong output for " + name + ": " + result);
                helper.assertTrue(result.getCount() == expectedCount,
                                name + ": expected " + expectedCount + " items, got " + result.getCount());
        }

        private void assertNoRecipe(GameTestHelper helper, String message,
                        CraftingInput input) {
                helper.assertTrue(helper.getLevel().recipeAccess()
                                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).isEmpty(),
                                message);
        }

        private void assertVariantRecipes(GameTestHelper helper, String family,
                        Item base, Item slab, Item stairs, Item wall) {
                Item empty = Items.AIR;

                assertRecipe(helper, family + "_slab", grid(
                                base, base, base,
                                empty, empty, empty,
                                empty, empty, empty), slab, 6);

                assertRecipe(helper, family + "_stairs", grid(
                                base, empty, empty,
                                base, base, empty,
                                base, base, base), stairs, 4);

                assertRecipe(helper, family + "_wall", grid(
                                base, base, base,
                                base, base, base,
                                empty, empty, empty), wall, 6);
                assertRecipe(helper, family + "_slab", grid(
                                empty, empty, empty,
                                base, base, base,
                                empty, empty, empty), slab, 6);

                assertRecipe(helper, family + "_slab", grid(
                                empty, empty, empty,
                                empty, empty, empty,
                                base, base, base), slab, 6);

                assertRecipe(helper, family + "_wall", grid(
                                empty, empty, empty,
                                base, base, base,
                                base, base, base), wall, 6);

                assertRecipe(helper, family + "_stairs", grid(
                                empty, empty, base,
                                empty, base, base,
                                base, base, base), stairs, 4);
        }

        private void assertStonecuttingRecipe(GameTestHelper helper, String name,
                        Item ingredient, Item expectedItem, int expectedCount) {
                ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE,
                                Identifier.fromNamespaceAndPath("ankerstone", name));
                RecipeHolder<?> holder = helper.getLevel().recipeAccess().byKey(key)
                                .orElseThrow(() -> helper.assertionException("Missing recipe: " + name));

                if (!(holder.value() instanceof StonecutterRecipe recipe)) {
                        throw helper.assertionException("Expected a stonecutting recipe: " + name);
                }
                SingleRecipeInput input = new SingleRecipeInput(new ItemStack(ingredient));
                helper.assertTrue(recipe.matches(input, helper.getLevel()),
                                name + ": expected ingredient did not match");

                Item otherFamily = ingredient == ModBlocks.STILLSTONE.asItem()
                                ? ModBlocks.REINFORCED_STILLSTONE.asItem()
                                : ModBlocks.STILLSTONE.asItem();
                SingleRecipeInput wrongInput = new SingleRecipeInput(new ItemStack(otherFamily));
                helper.assertFalse(recipe.matches(wrongInput, helper.getLevel()),
                                name + ": must reject the other stone family");

                ItemStack result = recipe.assemble(input);
                helper.assertTrue(result.getItem() == expectedItem,
                                name + ": wrong output item: " + result);
                helper.assertTrue(result.getCount() == expectedCount,
                                name + ": expected " + expectedCount + " items, got " + result.getCount());
        }

}
