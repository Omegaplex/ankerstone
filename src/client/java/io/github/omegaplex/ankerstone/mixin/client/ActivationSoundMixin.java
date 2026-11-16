package io.github.omegaplex.ankerstone.mixin.client;

import io.github.omegaplex.ankerstone.ClientSettings;
import io.github.omegaplex.ankerstone.ModSounds;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
abstract class ActivationSoundMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void ankerstone$filterActivation(
            SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> callback) {
        if (ModSounds.isTransitionSound(sound.getIdentifier())
                && !ClientSettings.ankerstoneSoundsEnabled()) {
            callback.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
