package io.github.omegaplex.ankerstone.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.omegaplex.ankerstone.AnkerstoneBlock;
import io.github.omegaplex.ankerstone.AnkerstoneMode;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class PistonReactionMixin {
    @ModifyReturnValue(method = "getPistonPushReaction", at = @At("RETURN"))
    private PushReaction ankerstone$restrictMovement(PushReaction original) {
        // At runtime, this code runs on Minecraft's BlockState instance.
        BlockState state = (BlockState) (Object) this;

        if (state.getBlock() instanceof AnkerstoneBlock
                && state.getValue(AnkerstoneBlock.MODE) != AnkerstoneMode.OFF) {
            return PushReaction.IMMOVEABLE;
        }

        return original;
    }
}