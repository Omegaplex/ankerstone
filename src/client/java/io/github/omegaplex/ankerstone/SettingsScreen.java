package io.github.omegaplex.ankerstone;

import java.io.IOException;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

final class SettingsScreen extends Screen {
        private final Screen parent;
        private ParticleCountSlider singleSlider;
        private ParticleCountSlider wideSlider;
        private CycleButton<Boolean> stillstoneSounds;
        private CycleButton<Boolean> ankerstoneSounds;

        private static final Identifier HEADER_ICON = Identifier.fromNamespaceAndPath(
                        Ankerstone.MOD_ID, "textures/gui/ankerstone_pulse.png");
        private final long animationStart = System.nanoTime();

        SettingsScreen(Screen parent) {
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
                boolean stillstoneEnabled = stillstoneSounds == null
                                ? saved.stillstoneSounds()
                                : stillstoneSounds.getValue();
                boolean ankerstoneEnabled = ankerstoneSounds == null
                                ? saved.ankerstoneSounds()
                                : ankerstoneSounds.getValue();

                stillstoneSounds = CycleButton.onOffBuilder(stillstoneEnabled)
                                .create(0, 0, 200, 20, Component.literal("Stillstone Sounds"));
                ankerstoneSounds = CycleButton.onOffBuilder(ankerstoneEnabled)
                                .create(0, 0, 200, 20, Component.literal("Ankerstone Sounds"));
                var controls = LinearLayout.vertical().spacing(8);
                controls.addChild(singleSlider);
                controls.addChild(wideSlider);
                controls.addChild(stillstoneSounds);
                controls.addChild(ankerstoneSounds);

                var layout = new HeaderAndFooterLayout(this);
                layout.addToContents(controls);
                layout.addToFooter(Button.builder(
                                Component.translatable("gui.done"), button -> saveAndClose())
                                .width(200)
                                .build());
                layout.arrangeElements();
                layout.visitWidgets(this::addRenderableWidget);
                addRenderableOnly((graphics, mouseX, mouseY, partialTick) -> {
                        int iconSize = 16;
                        int gap = 6;
                        int titleWidth = font.width(title);
                        int left = (width - iconSize - gap - titleWidth) / 2;
                        int frame = (int) (((System.nanoTime() - animationStart)
                                        / 200_000_000L) % 24);

                        graphics.blit(RenderPipelines.GUI_TEXTURED, HEADER_ICON,
                                        left, 20 + (font.lineHeight - iconSize) / 2,
                                        0.0F, frame * 16.0F,
                                        iconSize, iconSize,
                                        16, 16,
                                        16, 384);
                        graphics.text(font, title,
                                        left + iconSize + gap, 20, 0xFFFFFFFF);
                });
        }

        private void saveAndClose() {
                try {
                        ClientSettings.save(new ClientPreferences(
                                        singleSlider.count(), wideSlider.count(),
                                        stillstoneSounds.getValue(), ankerstoneSounds.getValue()));
                        onClose();
                } catch (IOException failure) {
                        Ankerstone.LOGGER.warn("Could not save Ankerstone client preferences", failure);
                        minecraft.gui.setScreen(new AlertScreen(
                                        () -> minecraft.gui.setScreen(this),
                                        Component.literal("Could not save settings"),
                                        Component.literal("The particle settings were not changed.")));
                }
        }

        @Override
        public void onClose() {
                minecraft.gui.setScreen(parent);
        }
}
