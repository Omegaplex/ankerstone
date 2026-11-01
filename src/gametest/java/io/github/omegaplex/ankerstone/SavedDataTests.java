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

        @GameTest
        public void activeLimitDefaultsToUnlimited(GameTestHelper helper) {
                LimitData data = new LimitData();

                helper.assertTrue(data.isUnlimited(),
                                "A new world must default to unlimited");
                helper.assertTrue(data.getActiveLimit() == LimitData.UNLIMITED,
                                "Unlimited must use the explicit sentinel value");
                helper.assertFalse(data.isDirty(),
                                "Reading the default setting must not request saving");
                helper.succeed();
        }

        @GameTest
        public void activeLimitAcceptsZeroPositiveAndUnlimited(GameTestHelper helper) {
                LimitData data = new LimitData();

                data.setActiveLimit(0);
                helper.assertTrue(data.getActiveLimit() == 0,
                                "Zero must remain a valid limit");
                helper.assertFalse(data.isUnlimited(),
                                "Zero must not mean unlimited");

                data.setActiveLimit(12);
                helper.assertTrue(data.getActiveLimit() == 12,
                                "A positive limit must retain its value");
                helper.assertFalse(data.isUnlimited(),
                                "A positive limit must not mean unlimited");

                data.setActiveLimit(LimitData.UNLIMITED);
                helper.assertTrue(data.isUnlimited(),
                                "A finite limit must be changeable back to unlimited");
                helper.succeed();
        }

        @GameTest
        public void activeLimitRejectsInvalidNegativeValues(GameTestHelper helper) {
                LimitData data = new LimitData();
                data.setActiveLimit(12);
                data.setDirty(false);

                for (int invalid : new int[] { -2, Integer.MIN_VALUE }) {
                        boolean rejected = false;
                        try {
                                data.setActiveLimit(invalid);
                        } catch (IllegalArgumentException expected) {
                                rejected = true;
                        }

                        helper.assertTrue(rejected,
                                        "Invalid negative limit must be rejected: " + invalid);
                        helper.assertTrue(data.getActiveLimit() == 12,
                                        "Rejected input must preserve the previous limit");
                        helper.assertFalse(data.isDirty(),
                                        "Rejected input must not request saving");
                }

                helper.succeed();
        }

        @GameTest
        public void activeLimitSaveFlagTracksChanges(GameTestHelper helper) {
                LimitData data = new LimitData();

                data.setActiveLimit(LimitData.UNLIMITED);
                helper.assertFalse(data.isDirty(),
                                "Setting the unchanged default must not request saving");

                for (int limit : new int[] { 0, 12, LimitData.UNLIMITED }) {
                        data.setActiveLimit(limit);
                        helper.assertTrue(data.isDirty(),
                                        "Changing the limit must request saving");

                        data.setDirty(false);
                        data.setActiveLimit(limit);
                        helper.assertFalse(data.isDirty(),
                                        "Setting the same limit must not request saving");
                }

                helper.succeed();
        }

        @GameTest
        public void activeLimitSurvivesRoundTrip(GameTestHelper helper) {
                for (int limit : new int[] { LimitData.UNLIMITED, 0, 12, Integer.MAX_VALUE }) {
                        LimitData original = new LimitData();
                        original.setActiveLimit(limit);

                        LimitData restored = roundTrip(original);

                        helper.assertTrue(restored.getActiveLimit() == limit,
                                        "Limit must survive encoding and decoding: " + limit);
                        helper.assertTrue(
                                        restored.isUnlimited() == (limit == LimitData.UNLIMITED),
                                        "Unlimited status must survive encoding and decoding");
                        helper.assertFalse(restored.isDirty(),
                                        "Decoding saved data must not request another save");
                }

                helper.succeed();
        }

        @GameTest
        public void activeLimitCodecRejectsInvalidValues(GameTestHelper helper) {
                for (int invalid : new int[] { -2, Integer.MIN_VALUE }) {
                        var encoded = new net.minecraft.nbt.CompoundTag();
                        encoded.putInt("active_limit", invalid);

                        helper.assertTrue(
                                        LimitData.CODEC.parse(NbtOps.INSTANCE, encoded).error().isPresent(),
                                        "Invalid saved limit must fail decoding: " + invalid);
                }

                helper.succeed();
        }

        private LimitData roundTrip(LimitData original) {
                Tag encoded = LimitData.CODEC
                                .encodeStart(NbtOps.INSTANCE, original).getOrThrow();
                return LimitData.CODEC
                                .parse(NbtOps.INSTANCE, encoded).getOrThrow();
        }

        private OwnerData roundTrip(OwnerData original) {
                Tag encoded = OwnerData.CODEC
                                .encodeStart(NbtOps.INSTANCE, original).getOrThrow();
                return OwnerData.CODEC
                                .parse(NbtOps.INSTANCE, encoded).getOrThrow();
        }
}