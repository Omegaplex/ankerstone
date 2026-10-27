package io.github.omegaplex.ankerstone;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

/** Disk-persistence checks; delivery uses a server-side stand-in, not a network reconnect. */
final class RestartCreditChecks {
    private static final List<Placement> PLACEMENTS = List.of(
            new Placement("Plain", Level.OVERWORLD, new BlockPos(10008, 200, 10013), false, false),
            new Placement("Moved", Level.OVERWORLD, new BlockPos(10524, 200, 10008), true, false),
            new Placement("Nether", Level.NETHER, new BlockPos(10008, 200, 10013), false, false),
            new Placement("Removed", Level.END, new BlockPos(10008, 200, 10013), false, true),
            new Placement("Deferred", Level.OVERWORLD, new BlockPos(10520, 200, 10013), false, true));

    void prepare(MinecraftServer server, Properties baseline, String run) {
        for (Placement placement : PLACEMENTS) {
            UUID id = UUID.nameUUIDFromBytes((run + ":" + placement.name).getBytes(StandardCharsets.UTF_8));
            baseline.setProperty(placement.key(), id.toString());
            ServerLevel level = server.getLevel(placement.dimension);
            level.setBlock(placement.start.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(placement.start, ModBlocks.ANKERSTONE.defaultBlockState(), Block.UPDATE_ALL);
            ServerPlayer placer = player(level, id);
            try {
                ModBlocks.ANKERSTONE.setPlacedBy(level, placement.start, level.getBlockState(placement.start),
                        placer, new ItemStack(ModBlocks.ANKERSTONE));
            } finally {
                placer.getAdvancements().clearTriggers();
            }
            require(id.equals(AttributionData.get(level).getPlacer(placement.global(placement.start))),
                    placement.name + " placement hook did not record its placer");
            if (placement.queued) {
                activate(level, placement.start, id);
                remove(level, placement.start);
            } else if (placement.moved) {
                level.setBlock(placement.start.west(), Blocks.PISTON.defaultBlockState()
                        .setValue(PistonBaseBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
                level.setBlock(placement.start.west(2), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    boolean movementFinished(MinecraftServer server, Properties baseline) {
        Placement moved = PLACEMENTS.get(1);
        ServerLevel level = server.getLevel(moved.dimension);
        if (!level.getBlockState(moved.position()).is(ModBlocks.ANKERSTONE)) return false;
        require(level.getBlockState(moved.start).is(Blocks.PISTON_HEAD),
                "The piston head must occupy the original Ankerstone position");
        require(level.getBlockState(moved.position()).getValue(AnkerstoneBlock.MODE) == AnkerstoneMode.OFF,
                "Piston-moved placement must remain Off before restart");
        checkSaved(server, baseline, false);
        return true;
    }

    void checkSaved(MinecraftServer server, Properties baseline, boolean consumed) {
        // These queries read attribution only, so they cannot load the remote fixture chunks.
        AttributionData data = AttributionData.get(server.overworld());
        for (Placement placement : PLACEMENTS) {
            UUID id = placement.id(baseline);
            UUID expected = !consumed && !placement.queued ? id : null;
            require(java.util.Objects.equals(expected, data.getPlacer(placement.global(placement.position()))),
                    placement.name + " placer record did not survive correctly across restart");
            if (placement.moved) {
                require(data.getPlacer(placement.global(placement.start)) == null,
                        "Old piston position regained attribution");
            }
            boolean pending = consumed ? placement.name.equals("Deferred") : placement.queued;
            require(data.hasPendingCredit(id) == pending,
                    placement.name + " pending credit did not survive correctly across restart");
        }
    }

    void activateAndDeliver(MinecraftServer server, Properties baseline) {
        for (Placement placement : PLACEMENTS) {
            ServerLevel level = server.getLevel(placement.dimension);
            UUID id = placement.id(baseline);
            if (!placement.queued) {
                require(level.getBlockState(placement.position()).is(ModBlocks.ANKERSTONE),
                        placement.name + " Off placement was not saved");
                require(level.getBlockState(placement.position()).getValue(AnkerstoneBlock.MODE) == AnkerstoneMode.OFF,
                        placement.name + " activated before the restart check");
                activate(level, placement.position(), id);
                remove(level, placement.position());
                if (placement.moved) {
                    level.setBlock(placement.start, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(placement.start.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(placement.start.west(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            if (!placement.name.equals("Deferred")) {
                ServerLevel returnLevel = placement.dimension == Level.OVERWORLD
                        ? server.getLevel(Level.NETHER) : server.overworld();
                deliver(returnLevel, id, true);
            }
        }
        checkSaved(server, baseline, true);
    }

    void checkNoRepeatAndDeliverDeferred(MinecraftServer server, Properties baseline) {
        checkSaved(server, baseline, true);
        // An unrelated player must not consume anyone else's queued credit.
        deliver(server.overworld(), UUID.randomUUID(), false);
        checkSaved(server, baseline, true);
        for (Placement placement : PLACEMENTS) {
            UUID id = placement.id(baseline);
            deliver(server.getLevel(Level.END), id, placement.name.equals("Deferred"));
            require(!AttributionData.get(server.overworld()).hasPendingCredit(id),
                    placement.name + " delivered credit must be consumed");
        }
    }

    private void activate(ServerLevel level, BlockPos position, UUID id) {
        require(level.getServer().getPlayerList().getPlayer(id) == null, "The placer must be offline");
        level.setBlock(position.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        require(level.getBlockState(position).getValue(AnkerstoneBlock.MODE) == AnkerstoneMode.WIDE,
                "Ordinary redstone power must activate the placement");
        AttributionData data = AttributionData.get(level);
        require(data.hasPendingCredit(id), "Activation must queue credit for the original offline placer");
        require(data.getPlacer(GlobalPos.of(level.dimension(), position)) == null,
                "Activation must consume the placement record");
    }

    private void remove(ServerLevel level, BlockPos position) {
        level.setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(position.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
    }

    private void deliver(ServerLevel level, UUID id, boolean expected) {
        ServerPlayer player = player(level, id);
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(level.getServer(), connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        try {
            var advancement = level.getServer().getAdvancements()
                    .get(Identifier.fromNamespaceAndPath("ankerstone", "stay_a_while"));
            require(advancement != null, "Activation advancement is missing");
            require(!player.getAdvancements().getOrStartProgress(advancement).isDone(),
                    "Returning player's activation advancement must start incomplete");
            ModAdvancements.onPlayerJoin(player);
            require(player.getAdvancements().getOrStartProgress(advancement).isDone() == expected,
                    "Saved credit delivery mismatch for " + id);
            require(!AttributionData.get(level).hasPendingCredit(id), "Delivered credit was not consumed");
            // Revoke before saving so a duplicate delivery cannot hide behind a completed advancement.
            player.getAdvancements().revoke(advancement, "activated");
            ModAdvancements.onPlayerJoin(player);
            require(!player.getAdvancements().getOrStartProgress(advancement).isDone(),
                    "Credit must not be delivered twice");
            player.getAdvancements().save();
        } finally {
            player.getAdvancements().clearTriggers();
            channel.finishAndReleaseAll();
        }
    }

    private ServerPlayer player(ServerLevel level, UUID id) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(id, "restart-credit"), false);
        return new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private record Placement(String name, ResourceKey<Level> dimension, BlockPos start, boolean moved, boolean queued) {
        BlockPos position() { return moved ? start.east() : start; }
        GlobalPos global(BlockPos pos) { return GlobalPos.of(dimension, pos); }
        String key() { return "credit." + name; }
        UUID id(Properties baseline) { return UUID.fromString(baseline.getProperty(key())); }
    }
}
