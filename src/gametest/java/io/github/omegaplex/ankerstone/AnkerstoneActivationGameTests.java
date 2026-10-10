package io.github.omegaplex.ankerstone;

import java.util.UUID;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class AnkerstoneActivationGameTests {
    @GameTest
    @SuppressWarnings("removal")
    public void unpoweredPlacementRecordsPlacer(GameTestHelper helper) {
        BlockPos localPos = new BlockPos(2, 2, 2);
        BlockPos absolutePos = helper.absolutePos(localPos);
        GlobalPos position = GlobalPos.of(
                helper.getLevel().dimension(), absolutePos);
        AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        try {
            helper.setBlock(localPos.below(), Blocks.STONE);
            ItemStack stack = new ItemStack(ModBlocks.ANKERSTONE.asItem());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atBottomCenterOf(absolutePos),
                    Direction.UP, absolutePos.below(), false);
            stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));

            helper.assertBlockPresent(ModBlocks.ANKERSTONE, localPos);
            helper.assertBlockProperty(localPos,
                    AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
            helper.assertTrue(player.getUUID().equals(data.getPlacer(position)),
                    "Player placement must store the placer UUID");
        } finally {
            helper.setBlock(localPos, Blocks.AIR);
            helper.setBlock(localPos.below(), Blocks.AIR);
            data.removePlacer(position);
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void removingUnpoweredBlockClearsPlacer(GameTestHelper helper) {
        BlockPos localPos = new BlockPos(2, 2, 2);
        BlockPos absolutePos = helper.absolutePos(localPos);
        GlobalPos position = GlobalPos.of(
                helper.getLevel().dimension(), absolutePos);
        AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        try {
            helper.setBlock(localPos.below(), Blocks.STONE);
            ItemStack stack = new ItemStack(ModBlocks.ANKERSTONE.asItem());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atBottomCenterOf(absolutePos),
                    Direction.UP, absolutePos.below(), false);
            stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            helper.assertTrue(player.getUUID().equals(data.getPlacer(position)),
                    "Placement must record a placer before testing removal");

            helper.setBlock(localPos, Blocks.AIR);

            helper.assertTrue(data.getPlacer(position) == null,
                    "Removing Ankerstone must clear its placer record");
        } finally {
            helper.setBlock(localPos, Blocks.AIR);
            helper.setBlock(localPos.below(), Blocks.AIR);
            data.removePlacer(position);
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    @GameTest
    public void nonPlayerPlacementHasNoPlacer(GameTestHelper helper) {
        BlockPos localPos = new BlockPos(2, 2, 2);
        GlobalPos position = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(localPos));
        AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());

        try {
            helper.setBlock(localPos, ModBlocks.ANKERSTONE);
            helper.assertTrue(data.getPlacer(position) == null,
                    "Placement without a player must not invent attribution");
        } finally {
            helper.setBlock(localPos, Blocks.AIR);
            data.removePlacer(position);
        }
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void activationGrantsPlacerAdvancement(GameTestHelper helper) {
        BlockPos localPos = new BlockPos(2, 2, 2);
        BlockPos absolutePos = helper.absolutePos(localPos);
        GlobalPos position = GlobalPos.of(
                helper.getLevel().dimension(), absolutePos);
        AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
        AdvancementHolder advancement = helper.getLevel().getServer()
                .getAdvancements().get(Identifier.fromNamespaceAndPath(
                        "ankerstone", "stay_a_while"));
        helper.assertTrue(advancement != null, "Missing activation advancement");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        try {
            helper.setBlock(localPos.below(), Blocks.STONE);
            ItemStack stack = new ItemStack(ModBlocks.ANKERSTONE.asItem());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atBottomCenterOf(absolutePos),
                    Direction.UP, absolutePos.below(), false);
            stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));

            helper.assertBlockProperty(localPos,
                    AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
            helper.assertTrue(player.getUUID().equals(data.getPlacer(position)),
                    "Placement must record the original placer");
            helper.assertFalse(player.getAdvancements()
                    .getOrStartProgress(advancement).isDone(),
                    "Unpowered placement must not grant activation");

            helper.setBlock(localPos.below(), Blocks.REDSTONE_BLOCK);

            helper.assertBlockProperty(localPos,
                    AnkerstoneBlock.MODE, AnkerstoneMode.WIDE);
            helper.assertTrue(player.getAdvancements()
                    .getOrStartProgress(advancement).isDone(),
                    "Activation must grant the placer advancement");
        } finally {
            helper.setBlock(localPos, Blocks.AIR);
            helper.setBlock(localPos.below(), Blocks.AIR);
            data.removePlacer(position);
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    @GameTest
    public void offlineActivationQueuesCredit(GameTestHelper helper) {
        BlockPos localPos = new BlockPos(2, 2, 2);
        GlobalPos position = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(localPos));
        UUID placer = UUID.randomUUID();
        AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());

        try {
            helper.assertTrue(helper.getLevel().getServer()
                    .getPlayerList().getPlayer(placer) == null,
                    "Test placer must be offline");

            helper.setBlock(localPos.below(), Blocks.STONE);
            helper.setBlock(localPos, ModBlocks.ANKERSTONE);
            data.setPlacer(position, placer);
            helper.assertBlockProperty(localPos,
                    AnkerstoneBlock.MODE, AnkerstoneMode.OFF);

            helper.setBlock(localPos.below(), Blocks.REDSTONE_BLOCK);

            helper.assertBlockProperty(localPos,
                    AnkerstoneBlock.MODE, AnkerstoneMode.WIDE);
            helper.assertTrue(data.hasPendingCredit(placer),
                    "Offline activation must queue advancement credit");
            helper.assertTrue(data.getPlacer(position) == null,
                    "Queued credit no longer needs the placement record");
        } finally {
            helper.setBlock(localPos, Blocks.AIR);
            helper.setBlock(localPos.below(), Blocks.AIR);
            data.removePlacer(position);
            data.removePendingCredit(placer);
        }
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void joiningDeliversPendingCredit(GameTestHelper helper) {
        AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
        AdvancementHolder advancement = helper.getLevel().getServer()
                .getAdvancements().get(Identifier.fromNamespaceAndPath(
                        "ankerstone", "stay_a_while"));
        helper.assertTrue(advancement != null, "Missing activation advancement");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        try {
            data.queueCredit(player.getUUID());
            helper.assertFalse(player.getAdvancements()
                    .getOrStartProgress(advancement).isDone(),
                    "Activation advancement must start incomplete");

            ServerPlayConnectionEvents.JOIN.invoker().onPlayReady(
                    player.connection,
                    ServerPlayNetworking.getSender(player),
                    helper.getLevel().getServer());

            helper.assertTrue(player.getAdvancements()
                    .getOrStartProgress(advancement).isDone(),
                    "Joining must deliver pending activation credit");
            helper.assertFalse(data.hasPendingCredit(player.getUUID()),
                    "Delivered credit must leave the pending queue");
        } finally {
            data.removePendingCredit(player.getUUID());
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void poweredPlacementGrantsAdvancement(GameTestHelper helper) {
        BlockPos localPos = new BlockPos(2, 2, 2);
        BlockPos absolutePos = helper.absolutePos(localPos);
        GlobalPos position = GlobalPos.of(
                helper.getLevel().dimension(), absolutePos);
        AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
        AdvancementHolder advancement = helper.getLevel().getServer()
                .getAdvancements().get(Identifier.fromNamespaceAndPath(
                        "ankerstone", "stay_a_while"));
        helper.assertTrue(advancement != null, "Missing activation advancement");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        try {
            helper.assertFalse(player.getAdvancements()
                    .getOrStartProgress(advancement).isDone(),
                    "Activation advancement must start incomplete");

            helper.setBlock(localPos.below(), Blocks.REDSTONE_BLOCK);
            ItemStack stack = new ItemStack(ModBlocks.ANKERSTONE.asItem());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atBottomCenterOf(absolutePos),
                    Direction.UP, absolutePos.below(), false);
            stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));

            helper.assertBlockProperty(localPos,
                    AnkerstoneBlock.MODE, AnkerstoneMode.WIDE);
            helper.assertTrue(player.getAdvancements()
                    .getOrStartProgress(advancement).isDone(),
                    "Powered placement must grant activation credit");
            helper.assertTrue(data.getPlacer(position) == null,
                    "Credited placement no longer needs its placer record");
        } finally {
            helper.setBlock(localPos, Blocks.AIR);
            helper.setBlock(localPos.below(), Blocks.AIR);
            data.removePlacer(position);
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }
}
