package io.github.omegaplex.ankerstone.mixin;

import io.github.omegaplex.ankerstone.AttributionData;
import io.github.omegaplex.ankerstone.AnkerstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
public abstract class MovingPistonMixin {
    @Inject(method = "preRemoveSideEffects", at = @At("HEAD"))
    private void ankerstone$clearRemovedPlacer(
            BlockPos pos, BlockState state, CallbackInfo ci) {
        PistonMovingBlockEntity piston = (PistonMovingBlockEntity) (Object) this;

        if (piston.getLevel() instanceof ServerLevel level
                && piston.getMovedState().getBlock() instanceof AnkerstoneBlock) {
            AttributionData.get(level).removePlacer(
                    GlobalPos.of(level.dimension(), pos));
        }
    }
}
