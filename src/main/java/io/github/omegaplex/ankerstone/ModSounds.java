package io.github.omegaplex.ankerstone;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final Identifier ACTIVATE_ID = Identifier.fromNamespaceAndPath(Ankerstone.MOD_ID,
            "block.ankerstone.activate");

    public static final Identifier ACTIVATE_WIDE_ID = id("block.ankerstone.activate_wide");
    public static final Identifier WIDE_TO_SINGLE_ID = id("block.ankerstone.wide_to_single");
    public static final Identifier DEACTIVATE_ID = id("block.ankerstone.deactivate");

    static final SoundEvent ACTIVATE = register(ACTIVATE_ID);
    static final SoundEvent ACTIVATE_WIDE = register(ACTIVATE_WIDE_ID);
    static final SoundEvent WIDE_TO_SINGLE = register(WIDE_TO_SINGLE_ID);
    static final SoundEvent DEACTIVATE = register(DEACTIVATE_ID);

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Ankerstone.MOD_ID, path);
    }

    private static SoundEvent register(Identifier id) {
        return Registry.register(
                BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    static SoundEvent forTransition(AnkerstoneMode from, AnkerstoneMode to) {
        if (from == to) {
            return null;
        }

        return switch (to) {
            case OFF -> DEACTIVATE;
            case SINGLE -> from == AnkerstoneMode.WIDE ? WIDE_TO_SINGLE : ACTIVATE;
            case WIDE -> ACTIVATE_WIDE;
        };
    }

    public static boolean isTransitionSound(Identifier id) {
        return ACTIVATE_ID.equals(id)
                || ACTIVATE_WIDE_ID.equals(id)
                || WIDE_TO_SINGLE_ID.equals(id)
                || DEACTIVATE_ID.equals(id);
    }

    static void initialize() {
        // Calling this method triggers registration during mod startup.
    }
}