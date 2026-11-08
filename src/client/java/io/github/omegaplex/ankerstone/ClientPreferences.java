package io.github.omegaplex.ankerstone;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Properties;

record ClientPreferences(int singleParticles, int wideParticles) {
    static final int MAX_SINGLE = 4;
    static final int MAX_WIDE = 8;
    static final ClientPreferences DEFAULTS = new ClientPreferences(1, 4);

    ClientPreferences {
        singleParticles = Math.clamp(singleParticles, 0, MAX_SINGLE);
        wideParticles = Math.clamp(wideParticles, 0, MAX_WIDE);
    }

    static ClientPreferences load(Path file) throws IOException {
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (NoSuchFileException missing) {
            return DEFAULTS;
        }

        boolean wasEnabled = !"false".equalsIgnoreCase(
                properties.getProperty("showParticles", "true").trim());
        return new ClientPreferences(
                readCount(properties, "singleParticles", wasEnabled ? 1 : 0),
                readCount(properties, "wideParticles", wasEnabled ? 4 : 0));
    }

    private static int readCount(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(
                    key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }

    void save(Path file) throws IOException {
        var properties = new Properties();
        properties.setProperty("singleParticles", Integer.toString(singleParticles));
        properties.setProperty("wideParticles", Integer.toString(wideParticles));

        Files.createDirectories(file.toAbsolutePath().getParent());
        try (var writer = Files.newBufferedWriter(file)) {
            properties.store(writer, "Ankerstone client preferences");
        }
    }
}
