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
                AnkerstoneSavedData data = new AnkerstoneSavedData();

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
                AnkerstoneSavedData original = new AnkerstoneSavedData();
                expected.forEach(original::addOwner);

                AnkerstoneSavedData restored = roundTrip(original);
                helper.assertTrue(restored.getOwnerModes().equals(expected),
                                "Owner positions and modes must survive encoding and decoding");
                helper.succeed();
        }

        @GameTest
        public void removedOwnerStaysAbsentAfterRoundTrip(GameTestHelper helper) {
                BlockPos ownerPos = new BlockPos(-17, 64, -1);
                AnkerstoneSavedData original = new AnkerstoneSavedData();
                original.addOwner(ownerPos, AnkerstoneMode.WIDE);
                original.removeOwner(ownerPos);

                AnkerstoneSavedData restored = roundTrip(original);
                helper.assertTrue(restored.getOwnerModes().isEmpty(),
                                "A removed owner must not reappear after encoding and decoding");
                helper.succeed();
        }

        private AnkerstoneSavedData roundTrip(AnkerstoneSavedData original) {
                Tag encoded = AnkerstoneSavedData.CODEC
                                .encodeStart(NbtOps.INSTANCE, original).getOrThrow();
                return AnkerstoneSavedData.CODEC
                                .parse(NbtOps.INSTANCE, encoded).getOrThrow();
        }
}