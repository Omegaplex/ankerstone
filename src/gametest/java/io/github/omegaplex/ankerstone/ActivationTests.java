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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ActivationTests {

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
                        placeBlockAsPlayer(helper, player, localPos, ModBlocks.ANKERSTONE);

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
                        placeBlockAsPlayer(helper, player, localPos, ModBlocks.ANKERSTONE);
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
        @SuppressWarnings("removal")
        public void nonPlayerActivationGrantsNoCredit(GameTestHelper helper) {
                BlockPos pos = new BlockPos(2, 2, 2);
                GlobalPos position = GlobalPos.of(
                                helper.getLevel().dimension(), helper.absolutePos(pos));
                AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
                AdvancementHolder advancement = helper.getLevel().getServer()
                                .getAdvancements().get(Identifier.fromNamespaceAndPath(
                                                "ankerstone", "stay_a_while"));
                helper.assertTrue(advancement != null, "Missing activation advancement");
                ServerPlayer activator = helper.makeMockServerPlayerInLevel();

                try {
                        helper.setBlock(pos.below(2), Blocks.STONE);
                        helper.setBlock(pos.below(), Blocks.STONE);
                        helper.setBlock(pos, ModBlocks.ANKERSTONE);
                        helper.assertTrue(data.getPlacer(position) == null,
                                        "Non-player placement must not invent attribution");

                        helper.setBlock(pos.below(), Blocks.AIR);
                        placeBlockAsPlayer(helper, activator, pos.below(), Blocks.REDSTONE_BLOCK);

                        helper.assertBlockProperty(pos, AnkerstoneBlock.MODE, AnkerstoneMode.WIDE);
                        helper.assertFalse(activator.getAdvancements()
                                        .getOrStartProgress(advancement).isDone(),
                                        "Powering an unattributed block must not grant activation credit");
                        helper.assertTrue(data.getPlacer(position) == null,
                                        "Activation must not invent a placer");
                        helper.assertFalse(data.hasPendingCredit(activator.getUUID()),
                                        "An unattributed block must not queue credit for its activator");
                } finally {
                        helper.setBlock(pos, Blocks.AIR);
                        helper.setBlock(pos.below(), Blocks.AIR);
                        helper.setBlock(pos.below(2), Blocks.AIR);
                        data.removePlacer(position);
                        data.removePendingCredit(activator.getUUID());
                        helper.getLevel().getServer().getPlayerList().remove(activator);
                }
                helper.succeed();
        }

        @GameTest
        public void singleActivationGrantsPlacerAdvancement(GameTestHelper helper) {
                assertActivationGrantsPlacer(helper, 1, AnkerstoneMode.SINGLE);
        }

        @GameTest
        public void wideActivationGrantsPlacerAdvancement(GameTestHelper helper) {
                assertActivationGrantsPlacer(helper, 15, AnkerstoneMode.WIDE);
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
                        helper.setBlock(localPos, Blocks.AIR);

                        helper.assertBlockPresent(Blocks.AIR, localPos);
                        helper.assertTrue(data.hasPendingCredit(placer),
                                        "Removing the activated block must preserve earned credit");
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
        public void singlePoweredPlacementGrantsAdvancement(GameTestHelper helper) {
                assertPoweredPlacementGrantsAdvancement(helper, 1, AnkerstoneMode.SINGLE);
        }

        @GameTest
        public void widePoweredPlacementGrantsAdvancement(GameTestHelper helper) {
                assertPoweredPlacementGrantsAdvancement(helper, 15, AnkerstoneMode.WIDE);
        }

        @GameTest
        @SuppressWarnings("removal")
        public void anotherPlayerPoweringCreditsPlacer(GameTestHelper helper) {
                BlockPos pos = new BlockPos(2, 3, 2);
                GlobalPos position = GlobalPos.of(
                                helper.getLevel().dimension(), helper.absolutePos(pos));
                AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
                AdvancementHolder advancement = helper.getLevel().getServer()
                                .getAdvancements().get(Identifier.fromNamespaceAndPath(
                                                "ankerstone", "stay_a_while"));
                helper.assertTrue(advancement != null, "Missing activation advancement");
                ServerPlayer placer = helper.makeMockServerPlayerInLevel();
                ServerPlayer activator = helper.makeMockServerPlayerInLevel();

                try {
                        helper.setBlock(pos.below(2), Blocks.STONE);
                        helper.setBlock(pos.below(), Blocks.STONE);
                        placeBlockAsPlayer(helper, placer, pos, ModBlocks.ANKERSTONE);
                        helper.assertBlockProperty(pos, AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                        helper.assertFalse(placer.getAdvancements()
                                        .getOrStartProgress(advancement).isDone(),
                                        "Off placement must not grant activation");

                        helper.setBlock(pos.below(), Blocks.AIR);
                        placeBlockAsPlayer(helper, activator, pos.below(), Blocks.REDSTONE_BLOCK);

                        helper.assertBlockProperty(pos, AnkerstoneBlock.MODE, AnkerstoneMode.WIDE);
                        helper.assertTrue(placer.getAdvancements()
                                        .getOrStartProgress(advancement).isDone(),
                                        "Original placer must receive activation credit");
                        helper.assertFalse(activator.getAdvancements()
                                        .getOrStartProgress(advancement).isDone(),
                                        "Supplying power must not earn someone else's credit");
                } finally {
                        helper.setBlock(pos, Blocks.AIR);
                        helper.setBlock(pos.below(), Blocks.AIR);
                        helper.setBlock(pos.below(2), Blocks.AIR);
                        data.removePlacer(position);
                        helper.getLevel().getServer().getPlayerList().remove(placer);
                        helper.getLevel().getServer().getPlayerList().remove(activator);
                }
                helper.succeed();
        }

        @GameTest
        @SuppressWarnings("removal")
        public void replacementCreditsNewPlacer(GameTestHelper helper) {
                BlockPos pos = new BlockPos(2, 2, 2);
                GlobalPos position = GlobalPos.of(
                                helper.getLevel().dimension(), helper.absolutePos(pos));
                AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
                AdvancementHolder advancement = helper.getLevel().getServer()
                                .getAdvancements().get(Identifier.fromNamespaceAndPath(
                                                "ankerstone", "stay_a_while"));
                helper.assertTrue(advancement != null, "Missing activation advancement");
                ServerPlayer first = helper.makeMockServerPlayerInLevel();
                ServerPlayer second = helper.makeMockServerPlayerInLevel();

                try {
                        helper.setBlock(pos.below(), Blocks.STONE);
                        placeBlockAsPlayer(helper, first, pos, ModBlocks.ANKERSTONE);
                        helper.assertTrue(first.getUUID().equals(data.getPlacer(position)),
                                        "First placement must record the first player");

                        helper.setBlock(pos, Blocks.AIR);
                        placeBlockAsPlayer(helper, second, pos, ModBlocks.ANKERSTONE);
                        helper.assertTrue(second.getUUID().equals(data.getPlacer(position)),
                                        "Replacement must record the new placer");

                        helper.setBlock(pos.below(), Blocks.REDSTONE_BLOCK);

                        helper.assertBlockProperty(pos, AnkerstoneBlock.MODE, AnkerstoneMode.WIDE);
                        helper.assertTrue(second.getAdvancements()
                                        .getOrStartProgress(advancement).isDone(),
                                        "New placer must receive activation credit");
                        helper.assertFalse(first.getAdvancements()
                                        .getOrStartProgress(advancement).isDone(),
                                        "Previous placer must not receive replacement credit");
                } finally {
                        helper.setBlock(pos, Blocks.AIR);
                        helper.setBlock(pos.below(), Blocks.AIR);
                        data.removePlacer(position);
                        helper.getLevel().getServer().getPlayerList().remove(first);
                        helper.getLevel().getServer().getPlayerList().remove(second);
                }
                helper.succeed();
        }

        @GameTest
        @SuppressWarnings("removal")
        public void joiningInNetherDeliversPendingCredit(GameTestHelper helper) {
                ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
                helper.assertTrue(nether != null, "Missing Nether dimension");
                AnkerstoneAttributionData data = AnkerstoneAttributionData.get(helper.getLevel());
                AdvancementHolder advancement = helper.getLevel().getServer()
                                .getAdvancements().get(Identifier.fromNamespaceAndPath(
                                                "ankerstone", "stay_a_while"));
                helper.assertTrue(advancement != null, "Missing activation advancement");
                ServerPlayer player = helper.makeMockServerPlayerInLevel();

                try {
                        data.queueCredit(player.getUUID());
                        player.setServerLevel(nether);
                        helper.assertFalse(player.getAdvancements()
                                        .getOrStartProgress(advancement).isDone(),
                                        "Activation advancement must start incomplete");

                        ServerPlayConnectionEvents.JOIN.invoker().onPlayReady(
                                        player.connection,
                                        ServerPlayNetworking.getSender(player),
                                        helper.getLevel().getServer());

                        helper.assertTrue(player.getAdvancements()
                                        .getOrStartProgress(advancement).isDone(),
                                        "Joining in the Nether must deliver pending credit");
                        helper.assertFalse(data.hasPendingCredit(player.getUUID()),
                                        "Nether delivery must clear the shared pending queue");
                } finally {
                        player.setServerLevel(helper.getLevel());
                        data.removePendingCredit(player.getUUID());
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        @SuppressWarnings("removal")
        private void assertActivationGrantsPlacer(GameTestHelper helper,
                        int power, AnkerstoneMode expectedMode) {
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
                        placeBlockAsPlayer(helper, player, localPos, ModBlocks.ANKERSTONE);

                        helper.assertBlockProperty(localPos,
                                        AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                        helper.assertTrue(player.getUUID().equals(data.getPlacer(position)),
                                        "Placement must record the original placer");
                        helper.assertFalse(player.getAdvancements()
                                        .getOrStartProgress(advancement).isDone(),
                                        "Unpowered placement must not grant activation");

                        helper.setBlock(localPos.below(), Blocks.DAYLIGHT_DETECTOR.defaultBlockState()
                                        .setValue(DaylightDetectorBlock.POWER, power));

                        helper.assertBlockProperty(localPos,
                                        AnkerstoneBlock.MODE, expectedMode);
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

        @SuppressWarnings("removal")
        private void assertPoweredPlacementGrantsAdvancement(GameTestHelper helper,
                        int power, AnkerstoneMode expectedMode) {
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
                        helper.assertTrue(player.isCreative(),
                                        "Powered placement fixture must be Creative");
                        helper.assertFalse(player.getAdvancements()
                                        .getOrStartProgress(advancement).isDone(),
                                        "Activation advancement must start incomplete");

                        helper.setBlock(localPos.below(), Blocks.DAYLIGHT_DETECTOR.defaultBlockState()
                                        .setValue(DaylightDetectorBlock.POWER, power));
                        placeBlockAsPlayer(helper, player, localPos, ModBlocks.ANKERSTONE);

                        helper.assertBlockProperty(localPos,
                                        AnkerstoneBlock.MODE, expectedMode);
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

        private void placeBlockAsPlayer(GameTestHelper helper, ServerPlayer player,
                        BlockPos localPos, Block block) {
                BlockPos absolutePos = helper.absolutePos(localPos);
                ItemStack stack = new ItemStack(block.asItem());
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                BlockHitResult hit = new BlockHitResult(
                                Vec3.atBottomCenterOf(absolutePos),
                                Direction.UP, absolutePos.below(), false);
                stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        }

}
