package io.github.omegaplex.ankerstone;

import java.io.IOException;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class AnkerstoneSettingsScreen extends Screen {
    private final Screen parent;
    private ParticleCountSlider singleSlider;
    private ParticleCountSlider wideSlider;

    AnkerstoneSettingsScreen(Screen parent) {
        super(Component.literal("Ankerstone Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var saved = ClientSettings.preferences();
        int single = singleSlider == null ? saved.singleParticles() : singleSlider.count();
        int wide = wideSlider == null ? saved.wideParticles() : wideSlider.count();

        singleSlider = new ParticleCountSlider(
                "Single particles", single, ClientPreferences.MAX_SINGLE);
        wideSlider = new ParticleCountSlider(
                "Wide particles", wide, ClientPreferences.MAX_WIDE);
        singleSlider.setTooltip(Tooltip.create(Component.literal(
                "Default: " + ClientPreferences.DEFAULTS.singleParticles())));
        wideSlider.setTooltip(Tooltip.create(Component.literal(
                "Default: " + ClientPreferences.DEFAULTS.wideParticles())));
        var controls = LinearLayout.vertical().spacing(8);
        controls.addChild(singleSlider);
        controls.addChild(wideSlider);

        var layout = new HeaderAndFooterLayout(this);
        layout.addTitleHeader(title, font);
        layout.addToContents(controls);
        layout.addToFooter(Button.builder(
                Component.translatable("gui.done"), button -> saveAndClose())
                .width(200)
                .build());
        layout.arrangeElements();
        layout.visitWidgets(this::addRenderableWidget);
    }

    private void saveAndClose() {
        try {
            ClientSettings.save(new ClientPreferences(singleSlider.count(), wideSlider.count()));
            onClose();
        } catch (IOException failure) {
            Ankerstone.LOGGER.warn("Could not save Ankerstone client preferences", failure);
            minecraft.setScreen(new AlertScreen(
                    () -> minecraft.setScreen(this),
                    Component.literal("Could not save settings"),
                    Component.literal("The particle settings were not changed.")));
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
