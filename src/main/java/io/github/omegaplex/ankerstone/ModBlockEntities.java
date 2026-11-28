package io.github.omegaplex.ankerstone;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<PrimedAnkerstoneBlockEntity> PRIMED_ANKERSTONE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(
                    Ankerstone.MOD_ID, "primed_ankerstone"),
            FabricBlockEntityTypeBuilder.create(
                    PrimedAnkerstoneBlockEntity::new,
                    ModBlocks.PRIMED_ANKERSTONE).build());

    private ModBlockEntities() {
    }

    public static void initialize() {
    }
}
