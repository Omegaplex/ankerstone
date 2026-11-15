package io.github.omegaplex.ankerstone.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.omegaplex.ankerstone.Footsteps;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
abstract class FootstepMixin {
    @ModifyExpressionValue(method = { "playStepSound", "playCombinationStepSounds",
            "playMuffledStepSound" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"), require = 3)
    private SoundType ankerstone$tuneStep(
            SoundType original, @Local(argsOnly = true, ordinal = 0) BlockState state) {
        return Footsteps.forClientStep(state, original);
    }

    @WrapOperation(method = { "playStepSound", "playCombinationStepSounds",
            "playMuffledStepSound" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"), require = 3)
    private void ankerstone$layerIron(
            Entity entity, SoundEvent step, float volume, float pitch,
            Operation<Void> original,
            @Local(argsOnly = true, ordinal = 0) BlockState state) {
        original.call(entity, step, volume, pitch);

        if (!Footsteps.shouldLayerIron(state, step)) {
            original.call(entity, step, volume, pitch);
            return;
        }

        original.call(entity, step, volume * 0.65F, pitch * 0.80F);
        original.call(entity, SoundType.IRON.getStepSound(),
                volume * 0.35F, pitch * 0.80F);
    }
}