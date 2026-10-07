package io.github.omegaplex.ankerstone;

import java.util.Map;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

public class AnkerstoneSavedDataGameTests {
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