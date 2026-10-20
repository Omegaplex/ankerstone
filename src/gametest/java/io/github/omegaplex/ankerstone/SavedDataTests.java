package io.github.omegaplex.ankerstone;

import java.util.Map;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

public class SavedDataTests {
        @GameTest
        public void changingInputPositionDoesNotMoveStoredOwner(GameTestHelper helper) {
                BlockPos originalPosition = new BlockPos(-17, 64, -1);
                BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                                originalPosition.getX(),
                                originalPosition.getY(),
                                originalPosition.getZ());
                OwnerData data = new OwnerData();

                data.addOwner(cursor, AnkerstoneMode.WIDE);
                cursor.set(32, 70, 48);

                Map<BlockPos, AnkerstoneMode> expected = Map.of(
                                originalPosition, AnkerstoneMode.WIDE);
                helper.assertTrue(data.getOwnerModes().equals(expected),
                                "Changing the input position must not move the saved owner");
                helper.assertTrue(roundTrip(data).getOwnerModes().equals(expected),
                                "Encoding must preserve the original owner position");

                data.removeOwner(originalPosition);
                helper.assertTrue(data.getOwnerModes().isEmpty(),
                                "The owner must remain removable at its original position");
                helper.succeed();
        }

        @GameTest
        public void ownerDataSurvivesRoundTrip(GameTestHelper helper) {
                Map<BlockPos, AnkerstoneMode> expected = Map.of(
                                new BlockPos(-17, 64, -1), AnkerstoneMode.SINGLE,
                                new BlockPos(16, -20, 31), AnkerstoneMode.WIDE);
                OwnerData original = new OwnerData();
                expected.forEach(original::addOwner);

                OwnerData restored = roundTrip(original);
                helper.assertTrue(restored.getOwnerModes().equals(expected),
                                "Owner positions and modes must survive encoding and decoding");
                helper.succeed();
        }

        @GameTest
        public void removedOwnerStaysAbsentAfterRoundTrip(GameTestHelper helper) {
                BlockPos ownerPos = new BlockPos(-17, 64, -1);
                OwnerData original = new OwnerData();
                original.addOwner(ownerPos, AnkerstoneMode.WIDE);
                original.removeOwner(ownerPos);

                OwnerData restored = roundTrip(original);
                helper.assertTrue(restored.getOwnerModes().isEmpty(),
                                "A removed owner must not reappear after encoding and decoding");
                helper.succeed();
        }

        @GameTest
        public void ownerSaveFlagTracksChanges(GameTestHelper helper) {
                BlockPos position = new BlockPos(-17, 64, -1);
                OwnerData data = new OwnerData();

                data.setDirty(false);
                data.addOwner(position, AnkerstoneMode.SINGLE);
                helper.assertTrue(data.isDirty(),
                                "Adding an owner must require saving");

                data.setDirty(false);
                data.addOwner(position, AnkerstoneMode.SINGLE);
                helper.assertFalse(data.isDirty(),
                                "An unchanged owner must not require saving");

                data.addOwner(position, AnkerstoneMode.WIDE);
                helper.assertTrue(data.isDirty(),
                                "Changing an owner mode must require saving");

                data.setDirty(false);
                data.removeOwner(position);
                helper.assertTrue(data.isDirty(),
                                "Removing an owner must require saving");

                data.setDirty(false);
                data.removeOwner(position);
                helper.assertFalse(data.isDirty(),
                                "Removing an absent owner must change nothing");
                helper.succeed();
        }

        private OwnerData roundTrip(OwnerData original) {
                Tag encoded = OwnerData.CODEC
                                .encodeStart(NbtOps.INSTANCE, original).getOrThrow();
                return OwnerData.CODEC
                                .parse(NbtOps.INSTANCE, encoded).getOrThrow();
        }
}