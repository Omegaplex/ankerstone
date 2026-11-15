package io.github.omegaplex.ankerstone;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public final class Footsteps {
    private static final SoundType STILLSTONE = tunedDeepslate(0.25F, 0.60F);
    private static final SoundType REINFORCED = tunedDeepslate(0.50F, 0.80F);
    private static final SoundType ANKERSTONE_BASE = new SoundType(
            SoundType.STONE.getVolume(),
            SoundType.STONE.getPitch(),
            SoundType.STONE.getBreakSound(),
            SoundType.POLISHED_DEEPSLATE.getStepSound(),
            SoundType.STONE.getPlaceSound(),
            SoundType.STONE.getHitSound(),
            SoundType.STONE.getFallSound());

    public static boolean shouldLayerIron(BlockState state, SoundEvent step) {
        return state.is(ModBlocks.ANKERSTONE)
                && step == ANKERSTONE_BASE.getStepSound()
                && ClientSettings.preferences().ankerstoneSounds();
    }

    private Footsteps() {
    }

    public static SoundType forClientStep(BlockState state, SoundType original) {
        if (state.is(ModBlocks.ANKERSTONE) && original == SoundType.STONE) {
            return ANKERSTONE_BASE;
        }
        return select(state, original, ClientSettings.preferences().stillstoneSounds());
    }

    static SoundType select(BlockState state, SoundType original, boolean enabled) {
        if (!enabled || original != SoundType.DEEPSLATE) {
            return original;
        }

        if (state.is(ModBlocks.STILLSTONE)
                || state.is(ModBlocks.STILLSTONE_SLAB)
                || state.is(ModBlocks.STILLSTONE_STAIRS)
                || state.is(ModBlocks.STILLSTONE_WALL)) {
            return STILLSTONE;
        }

        if (state.is(ModBlocks.REINFORCED_STILLSTONE)
                || state.is(ModBlocks.REINFORCED_STILLSTONE_SLAB)
                || state.is(ModBlocks.REINFORCED_STILLSTONE_STAIRS)
                || state.is(ModBlocks.REINFORCED_STILLSTONE_WALL)) {
            return REINFORCED;
        }

        return original;
    }

    private static SoundType tunedDeepslate(float volume, float pitch) {
        return tunedStep(SoundType.DEEPSLATE, SoundType.DEEPSLATE, volume, pitch);
    }

    private static SoundType tunedStep(
            SoundType original, SoundType stepSource, float volume, float pitch) {
        return new SoundType(
                original.getVolume() * volume,
                original.getPitch() * pitch,
                original.getBreakSound(), stepSource.getStepSound(),
                original.getPlaceSound(), original.getHitSound(),
                original.getFallSound());
    }
}
