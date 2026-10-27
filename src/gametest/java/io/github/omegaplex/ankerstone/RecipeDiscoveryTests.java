package io.github.omegaplex.ankerstone;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public class RecipeDiscoveryTests {
    private static final List<String> RECIPES = List.of(
            "stillstone", "reinforced_stillstone", "ankerstone",
            "stillstone_slab", "stillstone_stairs", "stillstone_wall",
            "stillstone_slab_from_stonecutting", "stillstone_stairs_from_stonecutting",
            "stillstone_wall_from_stonecutting",
            "reinforced_stillstone_slab", "reinforced_stillstone_stairs", "reinforced_stillstone_wall",
            "reinforced_stillstone_slab_from_stonecutting", "reinforced_stillstone_stairs_from_stonecutting",
            "reinforced_stillstone_wall_from_stonecutting");

    @GameTest
    public void survivalDiscoveryFollowsMaterials(GameTestHelper helper) {
        withSurvivalPlayer(helper, player -> {
            assertKnown(helper, player, List.of());
            obtain(player, Items.LAPIS_LAZULI);
            assertKnown(helper, player, List.of("stillstone"));
            obtain(player, ModBlocks.STILLSTONE.asItem());
            assertKnown(helper, player, RECIPES.stream()
                    .filter(id -> id.startsWith("stillstone") || id.equals("reinforced_stillstone")).toList());
            obtain(player, ModBlocks.REINFORCED_STILLSTONE.asItem());
            assertKnown(helper, player, RECIPES);
        });
        helper.succeed();
    }

    @GameTest
    public void otherIngredientsDoNotDiscoverRecipes(GameTestHelper helper) {
        withSurvivalPlayer(helper, player -> {
            for (Item item : List.of(Items.STONE, Items.IRON_NUGGET, Items.IRON_INGOT, Items.ENDER_PEARL)) {
                obtain(player, item);
                assertKnown(helper, player, List.of());
            }
        });
        helper.succeed();
    }

    @GameTest
    public void variantsDoNotDiscoverBaseBlockRecipes(GameTestHelper helper) {
        withSurvivalPlayer(helper, player -> {
            for (Item item : List.of(ModBlocks.STILLSTONE_SLAB.asItem(), ModBlocks.STILLSTONE_STAIRS.asItem(),
                    ModBlocks.STILLSTONE_WALL.asItem(), ModBlocks.REINFORCED_STILLSTONE_SLAB.asItem(),
                    ModBlocks.REINFORCED_STILLSTONE_STAIRS.asItem(), ModBlocks.REINFORCED_STILLSTONE_WALL.asItem())) {
                obtain(player, item);
                assertKnown(helper, player, List.of());
            }
        });
        helper.succeed();
    }

    private void obtain(ServerPlayer player, Item item) {
        player.getInventory().add(new ItemStack(item));
        player.inventoryMenu.broadcastChanges();
    }

    private void assertKnown(GameTestHelper helper, ServerPlayer player, List<String> expected) {
        for (String name : RECIPES) {
            var key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("ankerstone", name));
            helper.assertTrue(player.getRecipeBook().contains(key) == expected.contains(name),
                    name + ": expected discovered=" + expected.contains(name));
        }
    }

    private void withSurvivalPlayer(GameTestHelper helper, java.util.function.Consumer<ServerPlayer> check) {
        var server = helper.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "recipe-check"), false);
        // The vanilla GameTest mock overrides gameMode() to always return Creative.
        var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        try {
            server.getPlayerList().placeNewPlayer(connection, player, cookie);
            player.setGameMode(GameType.SURVIVAL);
            helper.assertTrue(player.gameMode() == GameType.SURVIVAL, "Recipe discovery must run in Survival");
            check.accept(player);
        } finally {
            if (server.getPlayerList().getPlayer(player.getUUID()) != null) server.getPlayerList().remove(player);
            channel.finishAndReleaseAll();
        }
    }
}
