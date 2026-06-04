package io.github.omegaplex.ankerstone;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {
    public static final Block ANKERSTONE = register(
            "ankerstone",
            AnkerstoneBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .lightLevel(AnkerstoneBlock::lightLevel));

    public static final Block STILLSTONE = register(
            "stillstone",
            Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE));

    public static final Block STILLSTONE_SLAB = register(
            "stillstone_slab",
            SlabBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE));

    public static final Block STILLSTONE_STAIRS = register(
            "stillstone_stairs",
            settings -> new StairBlock(STILLSTONE.defaultBlockState(), settings),
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE));

    public static final Block STILLSTONE_WALL = register(
            "stillstone_wall",
            WallBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE));

    public static final Block REINFORCED_STILLSTONE = register(
            "reinforced_stillstone",
            Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE));

    public static final Block REINFORCED_STILLSTONE_SLAB = register(
            "reinforced_stillstone_slab",
            SlabBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE));

    public static final Block REINFORCED_STILLSTONE_STAIRS = register(
            "reinforced_stillstone_stairs",
            settings -> new StairBlock(REINFORCED_STILLSTONE.defaultBlockState(), settings),
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE));

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory,
            BlockBehaviour.Properties settings) {
        ResourceKey<Block> blockKey = keyOfBlock(name);
        Block block = blockFactory.apply(settings.setId(blockKey));

        ResourceKey<Item> itemKey = keyOfItem(name);
        BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    private static ResourceKey<Block> keyOfBlock(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Ankerstone.MOD_ID, name));
    }

    private static ResourceKey<Item> keyOfItem(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Ankerstone.MOD_ID, name));
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS)
                .register(entries -> entries.accept(ANKERSTONE));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
                .register(entries -> {
                    entries.accept(STILLSTONE);
                    entries.accept(STILLSTONE_SLAB);
                    entries.accept(STILLSTONE_STAIRS);
                    entries.accept(STILLSTONE_WALL);
                    entries.accept(REINFORCED_STILLSTONE);
                    entries.accept(REINFORCED_STILLSTONE_SLAB);
                    entries.accept(REINFORCED_STILLSTONE_STAIRS);
                });
    }
}
