package io.github.omegaplex.ankerstone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

        AnkerstoneMode oldMode = state.getValue(MODE);

        if (oldMode != mode) {
            if (mode == AnkerstoneMode.SINGLE) {
                setSingleChunkLoaded(level, pos, true);
            }

            if (oldMode == AnkerstoneMode.SINGLE) {
                setSingleChunkLoaded(level, pos, false);
            }

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

    private static void setSingleChunkLoaded(Level level, BlockPos pos, boolean loaded) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int chunkX = Math.floorDiv(pos.getX(), 16);
        int chunkZ = Math.floorDiv(pos.getZ(), 16);

        serverLevel.setChunkForced(chunkX, chunkZ, loaded);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
            boolean movedByPiston) {
        if (state.getValue(MODE) == AnkerstoneMode.SINGLE) {
            setSingleChunkLoaded(level, pos, false);
        }

        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
