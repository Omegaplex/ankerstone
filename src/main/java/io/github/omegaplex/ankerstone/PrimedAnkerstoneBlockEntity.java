package io.github.omegaplex.ankerstone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;

public final class PrimedAnkerstoneBlockEntity extends BlockEntity {
    public static final int COUNTDOWN_TICKS = 140;
    public static final int EXPANSION_TICKS = 4;
    public static final int HOLD_TICKS = 4;
    public static final int COLLAPSE_TICKS = 6;
    public static final int FINALE_TICKS = EXPANSION_TICKS + HOLD_TICKS + COLLAPSE_TICKS + 1;

    private int remainingTicks = COUNTDOWN_TICKS;
    private int finaleTicks;

    public int finaleTicks() {
        return finaleTicks;
    }

    public boolean readyToDetonate() {
        return remainingTicks == 0 && finaleTicks == FINALE_TICKS;
    }

    public PrimedAnkerstoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRIMED_ANKERSTONE, pos, state);
    }

    public int remainingTicks() {
        return remainingTicks;
    }

    private long visualSyncGameTime;

    public static void serverTick(
            ServerLevel level, BlockPos pos, BlockState state,
            PrimedAnkerstoneBlockEntity primed) {
        if (primed.remainingTicks > 0) {
            if (primed.remainingTicks == COUNTDOWN_TICKS) {
                level.playSound(null, pos, ModSounds.BREACH_WARNING,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            primed.remainingTicks--;
            if (primed.remainingTicks == 0) {
                primed.emitFinale(level, pos, false);
            }
            primed.setChanged();
            if (primed.remainingTicks % 5 == 0) {
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        } else if (primed.finaleTicks < FINALE_TICKS) {
            primed.finaleTicks++;
            if (primed.finaleTicks == EXPANSION_TICKS + HOLD_TICKS) {
                primed.emitFinale(level, pos, true);
            }
            primed.setChanged();
            if (primed.finaleTicks == EXPANSION_TICKS + HOLD_TICKS || primed.readyToDetonate()) {
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }

        // Detached or replaced entities must never detonate a newer block.
        if (primed.readyToDetonate() && level.getBlockEntity(pos) == primed
                && level.getBlockState(pos).is(ModBlocks.PRIMED_ANKERSTONE)) {
            if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) {
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5,
                        pos.getZ() + 0.5, 8.0F, Level.ExplosionInteraction.BLOCK);
            }
        }
    }

    private void emitFinale(ServerLevel level, BlockPos pos, boolean inward) {
        int count = 256;
        // Both phases use the same shell positions, including across a reload.
        double phase = (pos.asLong() & 1023) * (Math.PI * 2 / 1024);
        // Evenly cover a sphere without rejection sampling or per-frame work.
        for (int i = 0; i < count; i++) {
            double y = 1 - 2 * (i + 0.5) / count;
            double ring = Math.sqrt(1 - y * y);
            double angle = phase + i * Math.PI * (3 - Math.sqrt(5));
            double radius = 2.25;
            level.sendParticles(inward ? ModParticles.BREACH_INWARD
                            : ModParticles.BREACH_OUTWARD,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    0, Math.cos(angle) * ring * radius, y * radius,
                    Math.sin(angle) * ring * radius, 1);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("RemainingTicks", remainingTicks);
        output.putInt("FinaleTicks", finaleTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        remainingTicks = Math.clamp(
                input.getIntOr("RemainingTicks", COUNTDOWN_TICKS),
                0, COUNTDOWN_TICKS);
        finaleTicks = remainingTicks == 0
                ? Math.clamp(input.getIntOr("FinaleTicks", 0), 0, FINALE_TICKS)
                : 0;
        if (level != null) {
            visualSyncGameTime = level.getGameTime();
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        visualSyncGameTime = level.getGameTime();
    }

    public float visualElapsedTicks(float partialTick) {
        if (level == null) {
            return COUNTDOWN_TICKS - remainingTicks;
        }

        double sinceSync = Math.max(
                0, level.getGameTime() - visualSyncGameTime + partialTick);
        return (float) Math.clamp(
                COUNTDOWN_TICKS - remainingTicks + sinceSync,
                0, COUNTDOWN_TICKS);
    }

    public float visualFinaleTicks(float partialTick) {
        if (remainingTicks > 0 || level == null) {
            return finaleTicks;
        }
        double sinceSync = Math.max(
                0, level.getGameTime() - visualSyncGameTime + partialTick);
        return (float) Math.clamp(finaleTicks + sinceSync, 0, FINALE_TICKS);
    }
}
