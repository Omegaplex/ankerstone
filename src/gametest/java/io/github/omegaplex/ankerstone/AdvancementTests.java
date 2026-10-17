package io.github.omegaplex.ankerstone;

import net.minecraft.core.component.DataComponents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class AdvancementTests {
    @GameTest
    public void obtainingStillstoneGrantsAdvancement(GameTestHelper helper) {
        assertObtainingGrants(helper, "root", ModBlocks.STILLSTONE.asItem());
        helper.succeed();
    }

    @GameTest
    public void obtainingReinforcedStillstoneGrantsAdvancement(GameTestHelper helper) {
        assertObtainingGrants(helper, "iron_resolve",
                ModBlocks.REINFORCED_STILLSTONE.asItem());
        helper.succeed();
    }

    @GameTest
    public void obtainingAnkerstoneGrantsAdvancement(GameTestHelper helper) {
        assertObtainingGrants(helper, "pearl_of_an_idea",
                ModBlocks.ANKERSTONE.asItem());
        helper.succeed();
    }

    @GameTest
    public void stillstoneVariantsDoNotGrantAdvancement(GameTestHelper helper) {
        assertItemsDoNotGrant(helper, "root",
                ModBlocks.STILLSTONE_SLAB.asItem(),
                ModBlocks.STILLSTONE_STAIRS.asItem(),
                ModBlocks.STILLSTONE_WALL.asItem());
        helper.succeed();
    }

    @GameTest
    public void reinforcedVariantsDoNotGrantAdvancement(GameTestHelper helper) {
        assertItemsDoNotGrant(helper, "iron_resolve",
                ModBlocks.REINFORCED_STILLSTONE_SLAB.asItem(),
                ModBlocks.REINFORCED_STILLSTONE_STAIRS.asItem(),
                ModBlocks.REINFORCED_STILLSTONE_WALL.asItem());
        helper.succeed();
    }

    @GameTest
    public void obtainingAnkerstoneDoesNotGrantActivation(GameTestHelper helper) {
        assertItemsDoNotGrant(helper, "stay_a_while",
                ModBlocks.ANKERSTONE.asItem());
        helper.succeed();
    }

    @GameTest
    public void activationAdvancementUsesWideIcon(GameTestHelper helper) {
        Identifier id = Identifier.fromNamespaceAndPath("ankerstone", "stay_a_while");
        AdvancementHolder advancement = helper.getLevel().getServer()
                .getAdvancements().get(id);
        helper.assertTrue(advancement != null, "Missing advancement: " + id);

        ItemStack icon = advancement.value().display()
                .orElseThrow(() -> helper.assertionException(
                        "Activation advancement must have display information"))
                .getIcon().create();

        helper.assertTrue(icon.is(ModBlocks.ANKERSTONE.asItem()),
                "Activation icon must use the Ankerstone item");

        Identifier expectedModel = Identifier.fromNamespaceAndPath(
                "ankerstone", "advancement/ankerstone_wide");
        helper.assertTrue(expectedModel.equals(icon.get(DataComponents.ITEM_MODEL)),
                "Activation icon must select the Wide item model");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    private void assertObtainingGrants(GameTestHelper helper, String name, Item item) {
        MinecraftServer server = helper.getLevel().getServer();
        Identifier id = Identifier.fromNamespaceAndPath("ankerstone", name);
        AdvancementHolder advancement = server.getAdvancements().get(id);
        helper.assertTrue(advancement != null, "Missing advancement: " + id);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            helper.assertFalse(player.getAdvancements()
                    .getOrStartProgress(advancement).isDone(),
                    name + ": must start incomplete");

            player.getInventory().add(new ItemStack(item));
            player.inventoryMenu.broadcastChanges();

            helper.assertTrue(player.getAdvancements()
                    .getOrStartProgress(advancement).isDone(),
                    name + ": obtaining the item must grant the advancement");
        } finally {
            server.getPlayerList().remove(player);
        }
    }

    @SuppressWarnings("removal")
    private void assertItemsDoNotGrant(GameTestHelper helper,
            String name, Item... items) {
        MinecraftServer server = helper.getLevel().getServer();
        Identifier id = Identifier.fromNamespaceAndPath("ankerstone", name);
        AdvancementHolder advancement = server.getAdvancements().get(id);
        helper.assertTrue(advancement != null, "Missing advancement: " + id);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            for (Item item : items) {
                player.getInventory().add(new ItemStack(item));
                player.inventoryMenu.broadcastChanges();

                helper.assertFalse(player.getAdvancements()
                        .getOrStartProgress(advancement).isDone(),
                        name + ": must not be granted by " + item);
            }
        } finally {
            server.getPlayerList().remove(player);
        }
    }
}