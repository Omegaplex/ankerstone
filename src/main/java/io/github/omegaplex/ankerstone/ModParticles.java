package io.github.omegaplex.ankerstone;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModParticles {
    public static final SimpleParticleType BREACH_OUTWARD = register("breach_outward");
    public static final SimpleParticleType BREACH_INWARD = register("breach_inward");

    private ModParticles() { }

    private static SimpleParticleType register(String name) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                Identifier.fromNamespaceAndPath(Ankerstone.MOD_ID, name),
                FabricParticleTypes.simple());
    }

    public static void initialize() { }
}
