package io.github.omegaplex.ankerstone;

import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.PortalParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Vanilla portal appearance with a bounded, center-relative flight path. */
public final class BreachParticle extends PortalParticle {
    private final double centerX, centerY, centerZ;
    private final double offsetX, offsetY, offsetZ;
    private final boolean inward;

    private BreachParticle(ClientLevel level, double x, double y, double z,
            double dx, double dy, double dz, TextureAtlasSprite sprite,
            boolean inward) {
        super(level, x, y, z, dx, dy, dz, sprite);
        centerX = x;
        centerY = y;
        centerZ = z;
        offsetX = dx;
        offsetY = dy;
        offsetZ = dz;
        this.inward = inward;
        lifetime = inward ? PrimedAnkerstoneBlockEntity.COLLAPSE_TICKS
                : PrimedAnkerstoneBlockEntity.EXPANSION_TICKS
                        + PrimedAnkerstoneBlockEntity.HOLD_TICKS;
        quadSize *= 2;
        hasPhysics = false;
        if (inward) {
            setPos(x + dx, y + dy, z + dz);
        }
        xo = this.x;
        yo = this.y;
        zo = this.z;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age > lifetime || (!inward && age == lifetime)) {
            remove();
            return;
        }
        double progress = inward ? age / (double) lifetime
                : Math.min(1, age / (double) PrimedAnkerstoneBlockEntity.EXPANSION_TICKS);
        double radius = inward ? 1 - progress * progress : Math.sqrt(progress);
        setPos(centerX + offsetX * radius,
                centerY + offsetY * radius, centerZ + offsetZ * radius);
    }

    @Override
    public float getQuadSize(float partialTick) {
        // Keep the expanding shell bright and full-sized through the hold.
        if (!inward) {
            return quadSize;
        }
        float progress = Math.clamp((age + partialTick) / lifetime, 0, 1);
        return quadSize * (1 - 0.5F * progress * progress);
    }

    @Override
    public int getLightCoords(float partialTick) {
        return 0xF000F0;
    }

    public static void initialize() {
        ParticleProviderRegistry.getInstance().register(ModParticles.BREACH_OUTWARD,
                sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                        new BreachParticle(level, x, y, z, dx, dy, dz,
                                sprites.get(random), false));
        ParticleProviderRegistry.getInstance().register(ModParticles.BREACH_INWARD,
                sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                        new BreachParticle(level, x, y, z, dx, dy, dz,
                                sprites.get(random), true));
    }
}
