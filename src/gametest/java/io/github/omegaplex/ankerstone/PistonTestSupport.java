package io.github.omegaplex.ankerstone;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

final class PistonTestSupport {
    private static final TicketType NEGATIVE_TEST_TICKET = new TicketType(
            240, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION);

    private PistonTestSupport() {
    }

    static void assertAnkerstoneTicketLevel(GameTestHelper helper,
            BlockPos localPos, int expectedLevel) {
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(localPos));
        TicketStorage storage = helper.getLevel().getChunkSource()
                .getDataStorage().computeIfAbsent(TicketStorage.TYPE);
        List<Integer> levels = storage.getTickets(chunk.pack()).stream()
                .filter(ticket -> ticket.getType() == ChunkTickets.ANKERSTONE_TICKET)
                .map(ticket -> ticket.getTicketLevel())
                .sorted()
                .toList();

        helper.assertTrue(levels.equals(List.of(expectedLevel)),
                "Expected Ankerstone ticket level " + expectedLevel
                        + " at " + chunk + ", got " + levels);
    }

    static void assertNoAnkerstoneTickets(GameTestHelper helper, BlockPos localPos) {
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(localPos));
        TicketStorage storage = helper.getLevel().getChunkSource()
                .getDataStorage().computeIfAbsent(TicketStorage.TYPE);
        long count = storage.getTickets(chunk.pack()).stream()
                .filter(ticket -> ticket.getType() == ChunkTickets.ANKERSTONE_TICKET)
                .count();

        helper.assertTrue(count == 0,
                "Expected no Ankerstone tickets at " + chunk + ", got " + count);
    }

    static void assertNoActiveOwnersOrTickets(GameTestHelper helper,
            BlockPos first, BlockPos second) {
        OwnerData savedData = OwnerData.get(helper.getLevel());
        for (BlockPos pos : List.of(first, second)) {
            helper.assertFalse(savedData.getOwnerModes().containsKey(
                    helper.absolutePos(pos)),
                    "Off movement must not register an owner at " + pos);
            assertNoAnkerstoneTickets(helper, pos);
        }
    }

    static BlockPos pistonBeforeChunkBoundary(GameTestHelper helper, Direction direction) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        boolean alongX = direction == Direction.EAST;
        int coordinate = alongX ? origin.getX() : origin.getZ();
        int boundary = (Math.floorDiv(coordinate + 4, 16) + 1) * 16;
        int offset = boundary - coordinate - 2;
        BlockPos piston = alongX
                ? new BlockPos(offset, 3, 3)
                : new BlockPos(3, 3, offset);
        ChunkPos start = ChunkPos.containing(
                helper.absolutePos(piston.relative(direction)));
        ChunkPos destination = ChunkPos.containing(
                helper.absolutePos(piston.relative(direction, 2)));

        helper.assertFalse(start.equals(destination),
                "The fixture must cross an actual chunk boundary");
        return piston;
    }

    static void loadNegativeBoundaryChunks(GameTestHelper helper,
            BlockPos piston, Direction direction) {
        for (BlockPos pos : List.of(piston.relative(direction),
                piston.relative(direction, 2))) {
            ChunkPos chunk = ChunkPos.containing(helper.absolutePos(pos));
            helper.getLevel().getChunkSource().addTicketWithRadius(
                    NEGATIVE_TEST_TICKET, chunk, 2);
            helper.getLevel().getChunk(chunk.x(), chunk.z());
        }
    }

    static void assertPistonChunksReady(GameTestHelper helper,
            BlockPos worldPiston, Direction direction) {
        ServerLevel level = helper.getLevel();
        for (BlockPos position : List.of(worldPiston.relative(direction),
                worldPiston.relative(direction, 2))) {
            ChunkPos chunk = ChunkPos.containing(position);
            helper.assertTrue(level.isPositionEntityTicking(position),
                    "Fixture chunk must be entity-ticking: " + chunk);
            // Moving piston block entities also wait for the chunk's entity data.
            helper.assertTrue(level.areEntitiesLoaded(chunk.pack()),
                    "Fixture entity data must be loaded: " + chunk);
        }
    }

    private static void releaseNegativeTestTickets(GameTestHelper helper,
            BlockPos piston, Direction direction) {
        for (BlockPos pos : List.of(piston.relative(direction),
                piston.relative(direction, 2))) {
            ChunkPos chunk = ChunkPos.containing(helper.absolutePos(pos));
            helper.getLevel().getChunkSource().removeTicketWithRadius(
                    NEGATIVE_TEST_TICKET, chunk, 2);
        }
    }

    static void clearPistonFixture(GameTestHelper helper, BlockPos piston) {
        helper.setBlock(piston.east(2), Blocks.AIR);
        helper.setBlock(piston, Blocks.AIR);
        helper.setBlock(piston.east(), Blocks.AIR);
        helper.setBlock(piston.west(), Blocks.AIR);

        for (int distance = 1; distance <= 3; distance++) {
            BlockPos pos = piston.east().south(distance);
            helper.setBlock(pos, Blocks.AIR);
            helper.setBlock(pos.below(), Blocks.AIR);
        }
        for (int distance = 1; distance <= 3; distance++) {
            BlockPos pos = piston.east(2 + distance);
            helper.setBlock(pos, Blocks.AIR);
            helper.setBlock(pos.below(), Blocks.AIR);
        }
        releaseNegativeTestTickets(helper, piston, Direction.EAST);
    }

    static void clearOffPistonFixture(GameTestHelper helper,
            BlockPos piston, Direction direction) {
        helper.setBlock(piston.relative(direction, 2), Blocks.AIR);
        helper.setBlock(piston, Blocks.AIR);
        helper.setBlock(piston.relative(direction), Blocks.AIR);
        helper.setBlock(piston.relative(direction.getOpposite()), Blocks.AIR);
        releaseNegativeTestTickets(helper, piston, direction);
    }

    static void assertOffAnkerstoneCanBePushed(
            GameTestHelper helper, Direction direction) {
        assertOffAnkerstoneCanBePushed(helper, direction, new BlockPos(3, 3, 3));
    }

    static void assertOffAnkerstoneCanBePushed(
            GameTestHelper helper, Direction direction, BlockPos piston) {
        BlockPos start = piston.relative(direction);
        BlockPos destination = start.relative(direction);
        assertNoAnkerstoneTickets(helper, start);
        assertNoAnkerstoneTickets(helper, destination);
        BlockPos power = piston.relative(direction.getOpposite());
        GlobalPos oldPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(start));
        GlobalPos newPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(destination));
        UUID placer = UUID.randomUUID();
        AttributionData data = AttributionData.get(helper.getLevel());
        try {
            helper.setBlock(piston, Blocks.PISTON.defaultBlockState()
                    .setValue(PistonBaseBlock.FACING, direction));
            helper.setBlock(start, ModBlocks.ANKERSTONE);
            data.setPlacer(oldPosition, placer);
            helper.assertBlockProperty(start, AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
            helper.assertBlockPresent(Blocks.AIR, destination);
            helper.setBlock(power, Blocks.REDSTONE_BLOCK);

            long movementDeadline = helper.getTick() + 20;
            boolean[] finished = { false };

            helper.onEachTick(() -> {
                if (finished[0]) {
                    return;
                }

                boolean movementFinished = helper.getBlockState(start).is(Blocks.PISTON_HEAD)
                        && helper.getBlockState(destination).is(ModBlocks.ANKERSTONE);

                if (!movementFinished && helper.getTick() < movementDeadline) {
                    return;
                }

                finished[0] = true;

                try {
                    helper.assertBlockPresent(Blocks.PISTON, piston);
                    helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, true);
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, start);
                    helper.assertBlockPresent(ModBlocks.ANKERSTONE, destination);
                    helper.assertBlockProperty(destination,
                            AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                    OwnerData savedData = OwnerData.get(helper.getLevel());
                    helper.assertFalse(savedData.getOwnerModes().containsKey(
                            helper.absolutePos(start)),
                            "Off push must not leave an owner at the old position");
                    helper.assertFalse(savedData.getOwnerModes().containsKey(
                            helper.absolutePos(destination)),
                            "Off push must not register an owner at the destination");
                    helper.assertTrue(placer.equals(data.getPlacer(newPosition)),
                            "Off push must preserve the placer at the destination");
                    helper.assertTrue(data.getPlacer(oldPosition) == null,
                            "Off push must clear the old placer record");
                    assertNoAnkerstoneTickets(helper, start);
                    assertNoAnkerstoneTickets(helper, destination);
                } finally {
                    clearOffPistonFixture(helper, piston, direction);
                    data.removePlacer(oldPosition);
                    data.removePlacer(newPosition);
                }
                helper.succeed();
            });
        } catch (RuntimeException failure) {
            clearOffPistonFixture(helper, piston, direction);
            data.removePlacer(oldPosition);
            data.removePlacer(newPosition);
            throw failure;
        }
    }

    static void assertOffAnkerstoneCanBePulled(
            GameTestHelper helper, Direction direction) {
        assertOffAnkerstoneCanBePulled(helper, direction, new BlockPos(3, 3, 3));
    }

    static void assertOffAnkerstoneCanBePulled(
            GameTestHelper helper, Direction direction, BlockPos piston) {
        BlockPos start = piston.relative(direction);
        BlockPos destination = start.relative(direction);
        BlockPos power = piston.relative(direction.getOpposite());
        GlobalPos oldPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(start));
        GlobalPos newPosition = GlobalPos.of(
                helper.getLevel().dimension(), helper.absolutePos(destination));
        UUID placer = UUID.randomUUID();
        AttributionData data = AttributionData.get(helper.getLevel());
        try {
            helper.setBlock(piston, Blocks.STICKY_PISTON.defaultBlockState()
                    .setValue(PistonBaseBlock.FACING, direction));
            helper.setBlock(start, ModBlocks.ANKERSTONE);
            data.setPlacer(oldPosition, placer);
            helper.assertBlockProperty(start, AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
            assertNoActiveOwnersOrTickets(helper, start, destination);
            helper.setBlock(power, Blocks.REDSTONE_BLOCK);

            long extensionDeadline = helper.getTick() + 15;
            long retractionDeadline = helper.getTick() + 30;
            boolean[] retracting = { false };
            boolean[] finished = { false };

            helper.onEachTick(() -> {
                if (finished[0]) {
                    return;
                }

                if (!retracting[0]) {
                    boolean extensionFinished = helper.getBlockState(start).is(Blocks.PISTON_HEAD)
                            && helper.getBlockState(destination).is(ModBlocks.ANKERSTONE);

                    if (!extensionFinished && helper.getTick() < extensionDeadline) {
                        return;
                    }

                    try {
                        helper.assertBlockPresent(Blocks.STICKY_PISTON, piston);
                        helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, true);
                        helper.assertBlockPresent(Blocks.PISTON_HEAD, start);
                        helper.assertBlockPresent(ModBlocks.ANKERSTONE, destination);
                        helper.assertBlockProperty(destination,
                                AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                        helper.assertTrue(placer.equals(data.getPlacer(newPosition)),
                                "Sticky extension must preserve the placer");
                        helper.assertTrue(data.getPlacer(oldPosition) == null,
                                "Sticky extension must clear the old placer record");
                        assertNoActiveOwnersOrTickets(helper, start, destination);
                        helper.setBlock(power, Blocks.AIR);
                        retracting[0] = true;
                    } catch (RuntimeException failure) {
                        finished[0] = true;
                        clearOffPistonFixture(helper, piston, direction);
                        data.removePlacer(oldPosition);
                        data.removePlacer(newPosition);
                        throw failure;
                    }

                    return;
                }

                boolean retractionFinished = helper.getBlockState(start).is(ModBlocks.ANKERSTONE)
                        && helper.getBlockState(destination).is(Blocks.AIR);

                if (!retractionFinished && helper.getTick() < retractionDeadline) {
                    return;
                }
                finished[0] = true;
                try {
                    helper.assertBlockPresent(Blocks.STICKY_PISTON, piston);
                    helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, false);
                    helper.assertBlockPresent(ModBlocks.ANKERSTONE, start);
                    helper.assertBlockProperty(start,
                            AnkerstoneBlock.MODE, AnkerstoneMode.OFF);
                    helper.assertBlockPresent(Blocks.AIR, destination);
                    helper.assertTrue(placer.equals(data.getPlacer(oldPosition)),
                            "Sticky retraction must return the original placer");
                    helper.assertTrue(data.getPlacer(newPosition) == null,
                            "Sticky retraction must clear the outer placer record");
                    assertNoActiveOwnersOrTickets(helper, start, destination);
                } finally {
                    clearOffPistonFixture(helper, piston, direction);
                    data.removePlacer(oldPosition);
                    data.removePlacer(newPosition);
                }
                helper.succeed();
            });
        } catch (RuntimeException failure) {
            clearOffPistonFixture(helper, piston, direction);
            data.removePlacer(oldPosition);
            data.removePlacer(newPosition);
            throw failure;
        }
    }

}
