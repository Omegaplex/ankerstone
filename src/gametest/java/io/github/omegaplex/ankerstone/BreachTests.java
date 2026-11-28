package io.github.omegaplex.ankerstone;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BreachTests {
    private static final int DURATION = PrimedAnkerstoneBlockEntity.COUNTDOWN_TICKS;

    @GameTest
    public void breachSoundsBelongToAnkerstoneToggle(GameTestHelper helper) {
        helper.assertTrue(ModSounds.isAnkerstoneSound(ModSounds.BREACH_WARNING_ID)
                        && ModSounds.isAnkerstoneSound(ModSounds.ACTIVATE_ID),
                "The Ankerstone toggle must include breach and activation cues");
        helper.assertTrue(!ModSounds.isAnkerstoneSound(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(
                                "minecraft", "entity.generic.explode")),
                "The Ankerstone toggle must not suppress vanilla explosion audio");
        helper.succeed();
    }

    @GameTest
    public void detonationWaitsForFinaleConsumesBlockAndDoesNotRepeat(GameTestHelper helper) {
        var level = helper.getLevel();
        // Keep the real explosion well away from the shared test structures.
        var pos = helper.absolutePos(new BlockPos(2, 2, 2)).offset(512, 128, 512);
        var witness = pos.east();
        var state = ModBlocks.PRIMED_ANKERSTONE.defaultBlockState();
        level.getChunkAt(pos);
        try {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            level.setBlock(witness, Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
            var primed = (PrimedAnkerstoneBlockEntity) level.getBlockEntity(pos);
            for (int tick = 0; tick < DURATION + PrimedAnkerstoneBlockEntity.FINALE_TICKS - 1; tick++) {
                PrimedAnkerstoneBlockEntity.serverTick(level, pos, state, primed);
                helper.assertTrue(level.getBlockEntity(pos) == primed
                                && level.getBlockState(witness).is(Blocks.GLASS),
                        "The block and witness must survive until the final pause ends");
            }
            PrimedAnkerstoneBlockEntity.serverTick(level, pos, state, primed);
            helper.assertTrue(level.getBlockState(pos).isAir()
                            && level.getBlockEntity(pos) == null,
                    "Detonation must consume the primed block and its entity");
            helper.assertTrue(level.getBlockState(witness).isAir(),
                    "The vanilla explosion must affect nearby terrain");
            helper.assertTrue(level.getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(20),
                    item -> item.getItem().is(ModBlocks.ANKERSTONE.asItem())).isEmpty(),
                    "The consumed Ankerstone must not drop its item");

            level.setBlock(witness, Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            PrimedAnkerstoneBlockEntity.serverTick(level, pos, state, primed);
            helper.assertTrue(level.getBlockState(witness).is(Blocks.GLASS)
                            && level.getBlockState(pos).is(Blocks.STONE),
                    "A stale ticker call must not detonate again or remove a replacement");
        } finally {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(witness, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        helper.succeed();
    }

    @GameTest
    public void finaleProgressSurvivesSaveAndStopsAtCompletion(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var state = ModBlocks.PRIMED_ANKERSTONE.defaultBlockState();
        try {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            var primed = (PrimedAnkerstoneBlockEntity) level.getBlockEntity(pos);
            for (int tick = 0; tick < DURATION; tick++) {
                PrimedAnkerstoneBlockEntity.serverTick(level, pos, state, primed);
            }
            helper.assertTrue(primed.finaleTicks() == 0 && !primed.readyToDetonate(),
                    "Countdown completion must start, not skip, the finale");
            for (int tick = 0; tick < 2; tick++) {
                PrimedAnkerstoneBlockEntity.serverTick(level, pos, state, primed);
            }
            var saved = primed.saveWithFullMetadata(level.registryAccess());
            var restored = (PrimedAnkerstoneBlockEntity) BlockEntity.loadStatic(
                    pos, state, saved, level.registryAccess());
            helper.assertTrue(restored != null && restored.finaleTicks() == 2,
                    "Loading during expansion must preserve finale progress");
            restored.setLevel(level);
            for (int tick = 3; tick <= PrimedAnkerstoneBlockEntity.FINALE_TICKS; tick++) {
                PrimedAnkerstoneBlockEntity.serverTick(level, pos, state, restored);
                helper.assertTrue(restored.finaleTicks() == tick,
                        "Finale must advance exactly once per server ticker call");
                helper.assertTrue(restored.readyToDetonate()
                                == (tick == PrimedAnkerstoneBlockEntity.FINALE_TICKS),
                        "Detonation readiness must wait until after collapse and its pause");
                if (tick == PrimedAnkerstoneBlockEntity.EXPANSION_TICKS + 2
                        || tick == PrimedAnkerstoneBlockEntity.EXPANSION_TICKS
                                + PrimedAnkerstoneBlockEntity.HOLD_TICKS + 3) {
                    var collapseSave = restored.saveWithFullMetadata(level.registryAccess());
                    restored = (PrimedAnkerstoneBlockEntity) BlockEntity.loadStatic(
                            pos, state, collapseSave, level.registryAccess());
                    helper.assertTrue(restored != null && restored.finaleTicks() == tick,
                            "Loading during hold or collapse must not restart the finale");
                    restored.setLevel(level);
                }
            }
            PrimedAnkerstoneBlockEntity.serverTick(level, pos, state, restored);
            var finishedSave = restored.saveWithFullMetadata(level.registryAccess());
            var finished = (PrimedAnkerstoneBlockEntity) BlockEntity.loadStatic(
                    pos, state, finishedSave, level.registryAccess());
            helper.assertTrue(finished != null && finished.readyToDetonate()
                            && finished.finaleTicks() == PrimedAnkerstoneBlockEntity.FINALE_TICKS,
                    "A completed finale must stay completed after extra ticks and loading");
            helper.assertBlockPresent(ModBlocks.PRIMED_ANKERSTONE, new BlockPos(2, 2, 2));
        } finally {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        helper.succeed();
    }

    @GameTest
    public void primedCountdownTakesConfiguredNumberOfTickerCalls(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var state = ModBlocks.PRIMED_ANKERSTONE.defaultBlockState();

        try {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            var entity = level.getBlockEntity(pos);
            helper.assertTrue(entity instanceof PrimedAnkerstoneBlockEntity,
                    "Primed placement must create its block entity");
            var primed = (PrimedAnkerstoneBlockEntity) entity;
            var block = (PrimedAnkerstoneBlock) state.getBlock();
            var ticker = block.getTicker(
                    level, state, ModBlockEntities.PRIMED_ANKERSTONE);
            helper.assertTrue(ticker != null,
                    "Primed block must provide a server ticker");
            helper.assertTrue(primed.remainingTicks() == DURATION,
                    "Countdown must start at " + DURATION + " ticks");

            for (int tick = 1; tick <= DURATION; tick++) {
                ticker.tick(level, pos, state, primed);
                helper.assertTrue(primed.remainingTicks() == DURATION - tick,
                        "Countdown must decrement once per ticker call");
            }

            ticker.tick(level, pos, state, primed);
            helper.assertTrue(primed.remainingTicks() == 0,
                    "Completed countdown must not become negative");
        } finally {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 200)
    public void primedCountdownTicksAutomatically(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos, ModBlocks.PRIMED_ANKERSTONE.defaultBlockState(),
                Block.UPDATE_ALL);

        var entity = level.getBlockEntity(pos);
        helper.assertTrue(entity instanceof PrimedAnkerstoneBlockEntity,
                "Primed placement must create its block entity");
        var primed = (PrimedAnkerstoneBlockEntity) entity;
        long[] firstDecrementTime = { -1 };

        helper.onEachTick(() -> {
            int remaining = primed.remainingTicks();

            if (firstDecrementTime[0] == -1) {
                if (remaining == DURATION) {
                    return;
                }
                helper.assertTrue(remaining == DURATION - 1,
                        "The first observed decrement must leave 99 ticks");
                firstDecrementTime[0] = level.getGameTime();
            }

            long elapsed = level.getGameTime() - firstDecrementTime[0];
            int expected = (int) Math.max(0, DURATION - 1 - elapsed);
            helper.assertTrue(remaining == expected,
                    "Automatic countdown must decrement once per server tick");

            if (remaining == 0) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL);
                helper.succeed();
            }
        });
    }

    @GameTest
    public void primedCountdownSurvivesSaveAndLoad(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var state = ModBlocks.PRIMED_ANKERSTONE.defaultBlockState();

        try {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            var original = (PrimedAnkerstoneBlockEntity) level.getBlockEntity(pos);
            var block = (PrimedAnkerstoneBlock) state.getBlock();
            var ticker = block.getTicker(
                    level, state, ModBlockEntities.PRIMED_ANKERSTONE);
            helper.assertTrue(ticker != null,
                    "Primed block must provide a server ticker");

            for (int tick = 0; tick < 37; tick++) {
                ticker.tick(level, pos, state, original);
            }

            var saved = original.saveWithFullMetadata(level.registryAccess());
            var loaded = BlockEntity.loadStatic(
                    pos, state, saved, level.registryAccess());
            helper.assertTrue(loaded instanceof PrimedAnkerstoneBlockEntity,
                    "Saved data must recreate the registered primed entity");
            var restored = (PrimedAnkerstoneBlockEntity) loaded;
            restored.setLevel(level);

            helper.assertTrue(restored.remainingTicks() == DURATION - 37,
                    "Loading must preserve the remaining " + (DURATION - 37) + " ticks");
            ticker.tick(level, pos, state, restored);
            helper.assertTrue(restored.remainingTicks() == DURATION - 38,
                    "Restored countdown must continue, not restart");
            helper.assertTrue(original.remainingTicks() == DURATION - 37,
                    "The restored entity must have independent countdown state");

            for (int tick = 0; tick < DURATION - 38; tick++) {
                ticker.tick(level, pos, state, restored);
            }

            var finishedSave = restored.saveWithFullMetadata(
                    level.registryAccess());
            var finished = BlockEntity.loadStatic(
                    pos, state, finishedSave, level.registryAccess());
            helper.assertTrue(finished instanceof PrimedAnkerstoneBlockEntity
                    && ((PrimedAnkerstoneBlockEntity) finished).remainingTicks() == 0,
                    "A completed countdown must remain completed after loading");
        } finally {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        helper.succeed();
    }
}
