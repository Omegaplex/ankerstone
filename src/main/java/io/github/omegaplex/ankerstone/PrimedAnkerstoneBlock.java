package io.github.omegaplex.ankerstone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;

public final class PrimedAnkerstoneBlock extends BaseEntityBlock {
    public PrimedAnkerstoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(AnkerstoneBlock.MODE, AnkerstoneMode.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AnkerstoneBlock.MODE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrimedAnkerstoneBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel)) {
            return null;
        }

        return createTickerHelper(
                type, ModBlockEntities.PRIMED_ANKERSTONE,
                (tickLevel, pos, tickState, primed) -> PrimedAnkerstoneBlockEntity.serverTick(
                        (ServerLevel) tickLevel,
                        pos, tickState, primed));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
