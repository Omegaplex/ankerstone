package io.github.omegaplex.ankerstone.mixin;

import java.util.Queue;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PersistentEntitySectionManager.class)
public interface EntityManagerAccessor {
    @Accessor("chunkVisibility")
    Long2ObjectMap<Visibility> ankerstone$getChunkVisibility();

    @Accessor("chunkLoadStatuses")
    Long2ObjectMap<?> ankerstone$getChunkLoadStatuses();

    @Accessor("chunksToUnload")
    LongSet ankerstone$getChunksToUnload();

    @Accessor("loadingInbox")
    Queue<?> ankerstone$getLoadingInbox();
}
