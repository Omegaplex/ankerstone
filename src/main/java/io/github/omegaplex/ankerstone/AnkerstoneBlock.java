package io.github.omegaplex.ankerstone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;

public class AnkerstoneBlock extends Block {
    public static final EnumProperty<AnkerstoneMode> MODE = EnumProperty.create("mode", AnkerstoneMode.class);

    public AnkerstoneBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(MODE, AnkerstoneMode.OFF));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        updateMode(level, pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            Orientation orientation, boolean movedByPiston) {
        updateMode(level, pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    private static void updateMode(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        int signalStrength = level.getBestNeighborSignal(pos);
        AnkerstoneMode mode = modeFromSignal(signalStrength);

        if (state.getValue(MODE) != mode) {
            level.setBlock(pos, state.setValue(MODE, mode), Block.UPDATE_CLIENTS);
        }
    }

    private static AnkerstoneMode modeFromSignal(int signalStrength) {
        if (signalStrength == 0) {
            return AnkerstoneMode.OFF;
        }

        if (signalStrength == 15) {
            return AnkerstoneMode.WIDE;
        }

        return AnkerstoneMode.SINGLE;
    }
}
