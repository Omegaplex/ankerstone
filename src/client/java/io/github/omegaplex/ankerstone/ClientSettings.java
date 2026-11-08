package io.github.omegaplex.ankerstone;

import java.io.IOException;
import java.nio.file.Path;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ClientSettings implements ClientModInitializer {
    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("ankerstone-client.properties");

    private static ClientPreferences preferences = ClientPreferences.DEFAULTS;

    @Override
    public void onInitializeClient() {
        try {
            preferences = ClientPreferences.load(FILE);
        } catch (IOException | IllegalArgumentException failure) {
            Ankerstone.LOGGER.warn(
                    "Could not load Ankerstone client preferences; using defaults", failure);
        }
        applyParticles();
    }

    static ClientPreferences preferences() {
        return preferences;
    }

    static void save(ClientPreferences updated) throws IOException {
        updated.save(FILE);
        preferences = updated;
        applyParticles();
    }

    private static void applyParticles() {
        AnkerstoneBlock.setParticleCounts(
                preferences.singleParticles(), preferences.wideParticles());

    }
}
