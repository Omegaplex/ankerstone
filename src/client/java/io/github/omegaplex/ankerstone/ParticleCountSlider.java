package io.github.omegaplex.ankerstone;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

final class ParticleCountSlider extends AbstractSliderButton {
    private final String label;
    private final int max;

    ParticleCountSlider(String label, int count, int max) {
        super(0, 0, 200, 20, Component.empty(), (double) count / max);
        this.label = label;
        this.max = max;
        updateMessage();
    }

    int count() {
        return (int) Math.round(value * max);
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(label + ": " + count()));
    }

    @Override
    protected void applyValue() {
        // The screen saves both slider values on Done.
    }
}
