package io.github.omegaplex.ankerstone;

import java.util.List;
import java.util.UUID;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class AttributionDataTests {
        @GameTest
        public void dimensionsKeepSeparatePlacers(GameTestHelper helper) {
                BlockPos coordinates = new BlockPos(10, 64, 10);
                GlobalPos overworld = GlobalPos.of(Level.OVERWORLD, coordinates);
                GlobalPos nether = GlobalPos.of(Level.NETHER, coordinates);
                UUID firstPlayer = UUID.randomUUID();
                UUID secondPlayer = UUID.randomUUID();
                AnkerstoneAttributionData data = new AnkerstoneAttributionData();

                data.setPlacer(overworld, firstPlayer);
                data.setPlacer(nether, secondPlayer);

                helper.assertTrue(firstPlayer.equals(data.getPlacer(overworld)),
                                "Overworld placement must retain its own placer");
                helper.assertTrue(secondPlayer.equals(data.getPlacer(nether)),
                                "Nether placement must retain its own placer");
                helper.succeed();
        }

        @GameTest
        public void removedPlacementHasNoPlacer(GameTestHelper helper) {
                GlobalPos position = GlobalPos.of(
                                Level.OVERWORLD, new BlockPos(10, 64, 10));
                AnkerstoneAttributionData data = new AnkerstoneAttributionData();
                data.setPlacer(position, UUID.randomUUID());

                data.removePlacer(position);

                helper.assertTrue(data.getPlacer(position) == null,
                                "Removed placement must not retain a placer");
                helper.succeed();
        }

        @GameTest
        public void changingInputPositionDoesNotMoveStoredPlacement(GameTestHelper helper) {
                BlockPos.MutableBlockPos coordinates = new BlockPos.MutableBlockPos(10, 64, 10);
                GlobalPos original = GlobalPos.of(
                                Level.OVERWORLD, new BlockPos(10, 64, 10));
                UUID placer = UUID.randomUUID();
                AnkerstoneAttributionData data = new AnkerstoneAttributionData();

                data.setPlacer(GlobalPos.of(Level.OVERWORLD, coordinates), placer);
                coordinates.set(20, 64, 20);

                helper.assertTrue(placer.equals(data.getPlacer(original)),
                                "Changing the input position must not move the stored placement");
                helper.assertTrue(data.getPlacer(
                                GlobalPos.of(Level.OVERWORLD, coordinates)) == null,
                                "The changed input position must not acquire a placer");
                helper.succeed();
        }

        @GameTest
        public void dimensionsShareAttributionData(GameTestHelper helper) {
                ServerLevel overworld = helper.getLevel().getServer().overworld();
                ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
                helper.assertTrue(nether != null, "Nether must be available");

                AnkerstoneAttributionData data = AnkerstoneAttributionData.get(overworld);

                helper.assertTrue(data == AnkerstoneAttributionData.get(nether),
                                "Dimensions must share the same attribution data");
                helper.assertTrue(data == overworld.getDataStorage()
                                .get(AnkerstoneAttributionData.TYPE),
                                "Attribution data must be managed by world storage");
                helper.succeed();
        }

        @GameTest
        public void placerSaveFlagTracksChanges(GameTestHelper helper) {
                GlobalPos position = GlobalPos.of(
                                Level.OVERWORLD, new BlockPos(10, 64, 10));
                UUID player = UUID.randomUUID();
                AnkerstoneAttributionData data = new AnkerstoneAttributionData();

                data.setPlacer(position, player);
                helper.assertTrue(data.isDirty(), "Adding a placer must require saving");

                data.setDirty(false);
                data.setPlacer(position, player);
                helper.assertFalse(data.isDirty(), "An unchanged placer must not require saving");

                data.setPlacer(position, UUID.randomUUID());
                helper.assertTrue(data.isDirty(), "Replacing a placer must require saving");

                data.setDirty(false);
                data.removePlacer(position);
                helper.assertTrue(data.isDirty(), "Removing a placer must require saving");

                data.setDirty(false);
                data.removePlacer(position);
                helper.assertFalse(data.isDirty(), "Removing an absent placer must change nothing");
                helper.succeed();
        }

        @GameTest
        public void pendingCreditSaveFlagTracksChanges(GameTestHelper helper) {
                UUID player = UUID.randomUUID();
                AnkerstoneAttributionData data = new AnkerstoneAttributionData();

                data.queueCredit(player);
                helper.assertTrue(data.isDirty(), "Adding pending credit must require saving");

                data.setDirty(false);
                data.queueCredit(player);
                helper.assertFalse(data.isDirty(), "Duplicate credit must not require saving");

                data.removePendingCredit(player);
                helper.assertTrue(data.isDirty(), "Clearing pending credit must require saving");

                data.setDirty(false);
                data.removePendingCredit(player);
                helper.assertFalse(data.isDirty(), "Removing absent credit must change nothing");
                helper.succeed();
        }

        @GameTest
        public void pendingCreditSurvivesRoundTrip(GameTestHelper helper) {
                UUID player = UUID.randomUUID();
                UUID otherPlayer = UUID.randomUUID();
                AnkerstoneAttributionData original = new AnkerstoneAttributionData();
                original.queueCredit(player);

                AnkerstoneAttributionData restored = roundTrip(original);

                helper.assertTrue(restored.hasPendingCredit(player),
                                "Pending credit must survive serialization");
                helper.assertFalse(restored.hasPendingCredit(otherPlayer),
                                "Pending credit must belong only to the credited player");
                helper.succeed();
        }

        @GameTest
        public void clearedCreditDoesNotReturnAfterRoundTrip(GameTestHelper helper) {
                UUID player = UUID.randomUUID();
                AnkerstoneAttributionData original = new AnkerstoneAttributionData();
                original.queueCredit(player);
                AnkerstoneAttributionData restored = roundTrip(original);

                restored.removePendingCredit(player);
                AnkerstoneAttributionData savedAgain = roundTrip(restored);

                helper.assertFalse(savedAgain.hasPendingCredit(player),
                                "Cleared credit must not return after serialization");
                helper.succeed();
        }

        @GameTest
        public void removingPlacementKeepsEarnedCredit(GameTestHelper helper) {
                UUID player = UUID.randomUUID();
                GlobalPos position = GlobalPos.of(
                                Level.OVERWORLD, new BlockPos(10, 64, 10));
                AnkerstoneAttributionData data = new AnkerstoneAttributionData();
                data.setPlacer(position, player);
                data.queueCredit(player);

                data.removePlacer(position);

                helper.assertTrue(data.hasPendingCredit(player),
                                "Removing a placement must not erase earned credit");
                helper.succeed();
        }

        @GameTest
        public void placersSurviveRoundTrip(GameTestHelper helper) {
                GlobalPos overworld = GlobalPos.of(
                                Level.OVERWORLD, new BlockPos(10, 64, 10));
                GlobalPos nether = GlobalPos.of(
                                Level.NETHER, new BlockPos(10, 64, 10));
                UUID firstPlayer = UUID.randomUUID();
                UUID secondPlayer = UUID.randomUUID();
                AnkerstoneAttributionData original = new AnkerstoneAttributionData();
                original.setPlacer(overworld, firstPlayer);
                original.setPlacer(nether, secondPlayer);

                AnkerstoneAttributionData restored = roundTrip(original);

                helper.assertTrue(firstPlayer.equals(restored.getPlacer(overworld)),
                                "Overworld placer must survive serialization");
                helper.assertTrue(secondPlayer.equals(restored.getPlacer(nether)),
                                "Nether placer must survive serialization");
                helper.succeed();
        }

        @GameTest
        public void movedPlacerSurvivesRoundTrip(GameTestHelper helper) {
                BlockPos start = new BlockPos(10, 64, 10);
                GlobalPos oldPosition = GlobalPos.of(Level.OVERWORLD, start);
                GlobalPos newPosition = GlobalPos.of(Level.OVERWORLD, start.east());
                UUID placer = UUID.randomUUID();
                AnkerstoneAttributionData original = new AnkerstoneAttributionData();

                original.setPlacer(oldPosition, placer);
                original.setDirty(false);

                original.movePlacers(Level.OVERWORLD, List.of(start), Direction.EAST);

                helper.assertTrue(original.isDirty(),
                                "Moving a placer must require saving");

                AnkerstoneAttributionData restored = roundTrip(original);

                helper.assertTrue(placer.equals(restored.getPlacer(newPosition)),
                                "Moved placer must survive serialization at the destination");
                helper.assertTrue(restored.getPlacer(oldPosition) == null,
                                "Serialization must not restore attribution at the old position");
                helper.succeed();
        }

        private AnkerstoneAttributionData roundTrip(AnkerstoneAttributionData original) {
                Tag encoded = AnkerstoneAttributionData.CODEC
                                .encodeStart(NbtOps.INSTANCE, original).getOrThrow();
                return AnkerstoneAttributionData.CODEC
                                .parse(NbtOps.INSTANCE, encoded).getOrThrow();
        }
}
