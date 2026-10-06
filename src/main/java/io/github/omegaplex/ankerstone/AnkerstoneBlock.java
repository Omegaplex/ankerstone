package io.github.omegaplex.ankerstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
        // Active placements need ownership even when their mode does not change.
        if (!oldState.is(this)) {
            AnkerstoneMode mode = state.getValue(MODE);

            if (mode != AnkerstoneMode.OFF) {
                setChunkLoading(level, pos, mode, true);
            }
        }

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
            if (oldMode != AnkerstoneMode.OFF) {
                setChunkLoading(level, pos, oldMode, false);
            }

            if (mode != AnkerstoneMode.OFF) {
                setChunkLoading(level, pos, mode, true);
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

    private static void setChunkLoading(Level level, BlockPos pos, AnkerstoneMode mode, boolean loaded) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (loaded) {
            AnkerstoneChunkTickets.add(serverLevel, pos, mode);
        } else {
            AnkerstoneChunkTickets.remove(serverLevel, pos, mode);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
            boolean movedByPiston) {
        AnkerstoneMode mode = state.getValue(MODE);

        if (mode != AnkerstoneMode.OFF) {
            setChunkLoading(level, pos, mode, false);
        }

        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    public static int lightLevel(BlockState state) {
        AnkerstoneMode mode = state.getValue(MODE);

        if (mode == AnkerstoneMode.WIDE) {
            return 7;
        }

        if (mode == AnkerstoneMode.SINGLE) {
            return 3;
        }

        return 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        AnkerstoneMode mode = state.getValue(MODE);

        if (mode == AnkerstoneMode.OFF) {
            return;
        }

        int particleCount = mode == AnkerstoneMode.WIDE ? 4 : 1;

        for (int i = 0; i < particleCount; i++) {
            int side = random.nextInt(6);

            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            double xSpeed = 0.0;
            double ySpeed = 0.0;
            double zSpeed = 0.0;

            if (side == 0) {
                x = pos.getX() - 0.05;
                xSpeed = 0.02;
            } else if (side == 1) {
                x = pos.getX() + 1.05;
                xSpeed = -0.02;
            } else if (side == 2) {
                y = pos.getY() - 0.05;
                ySpeed = 0.02;
            } else if (side == 3) {
                y = pos.getY() + 1.05;
                ySpeed = -0.02;
            } else if (side == 4) {
                z = pos.getZ() - 0.05;
                zSpeed = 0.02;
            } else {
                z = pos.getZ() + 1.05;
                zSpeed = -0.02;
            }

            level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}
