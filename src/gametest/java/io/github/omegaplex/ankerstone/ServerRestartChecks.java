package io.github.omegaplex.ankerstone;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.github.omegaplex.ankerstone.mixin.ServerLevelAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Test-only initializer; inert during ordinary GameTests and absent from the release JAR. */
public class ServerRestartChecks implements ModInitializer {
    private static final int SETTLE_TICKS = 400;
    private static final int OBSERVE_TICKS = 1200;
    private final String phase = System.getProperty("ankerstone.restart.phase");
    private final String run = System.getProperty("ankerstone.restart.run");
    private final String process = UUID.randomUUID().toString();
    private final Properties baseline = new Properties();
    private final List<Fixture> fixtures = new ArrayList<>();
    private final RestartCreditChecks credits = new RestartCreditChecks();
    private Path directory;
    private int ticks;
    private int step;
    private int stepStarted;
    private long expires;
    private boolean finished;

    @Override
    public void onInitialize() {
        if (phase == null) return;
        require(List.of("prepare", "restore", "released").contains(phase), "Unknown restart phase");
        require(run != null && !run.isBlank(), "Restart task must supply a run identity");
        ServerLifecycleEvents.SERVER_STARTED.register(this::start);
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void start(MinecraftServer server) {
        directory = server.getServerDirectory().toAbsolutePath();
        expires = System.nanoTime() + TimeUnit.SECONDS.toNanos(120);
        try {
            require(server instanceof DedicatedServer, "Restart checks require a real dedicated server");
            require(((DedicatedServer) server).pauseWhenEmptySeconds() == 0, "Empty-server pausing must be disabled");
            require(server.getPlayerCount() == 0, "No player may load the fixtures");
            server.tickRateManager().setTickRate(200);
            fixtures.add(new Fixture(server.overworld(), "Single", 10008, AnkerstoneMode.SINGLE, false));
            fixtures.add(new Fixture(server.overworld(), "Wide", 10520, AnkerstoneMode.WIDE, false));
            fixtures.add(new Fixture(server.getLevel(Level.NETHER), "Nether", 10008, AnkerstoneMode.WIDE, false));
            fixtures.add(new Fixture(server.getLevel(Level.END), "EndSingle", 10008, AnkerstoneMode.SINGLE, false));
            fixtures.add(new Fixture(server.getLevel(Level.END), "EndWide", 10520, AnkerstoneMode.WIDE, false));
            fixtures.add(new Fixture(server.overworld(), "Off", 11032, AnkerstoneMode.WIDE, true));
            fixtures.add(new Fixture(server.overworld(), "Removed", 11544, AnkerstoneMode.WIDE, true));
            if (phase.equals("prepare")) {
                server.getGameRules().set(GameRules.COMMAND_BLOCKS_WORK, true, server);
                server.getGameRules().set(GameRules.COMMAND_BLOCK_OUTPUT, false, server);
                server.getScoreboard().addObjective("restart_ticks", ObjectiveCriteria.DUMMY,
                        Component.literal("Restart checks"), ObjectiveCriteria.RenderType.INTEGER, false, null);
                for (Fixture fixture : fixtures) fixture.create(server);
                baseline.setProperty("run", run);
                baseline.setProperty("prepareProcess", process);
                credits.prepare(server, baseline, run);
            } else {
                try (var input = Files.newInputStream(directory.resolve("baseline.properties"))) {
                    baseline.load(input);
                }
                require(run.equals(baseline.getProperty("run")), "Baseline belongs to another test world");
                require(!process.equals(baseline.getProperty("prepareProcess")), "Must use a fresh server process");
                require(!process.equals(baseline.getProperty("restoreProcess")), "Must restart after releasing owners");
                credits.checkSaved(server, baseline, phase.equals("released"));
                for (Fixture fixture : fixtures) fixture.readIds(baseline);
                for (Fixture fixture : fixtures) {
                    boolean active = phase.equals("restore") && !fixture.negative;
                    int expected = fixture.mode == AnkerstoneMode.WIDE ? 30 : 31;
                    TicketStorage storage = fixture.level.getChunkSource().getDataStorage()
                            .computeIfAbsent(TicketStorage.TYPE);
                    var levels = storage.getTickets(ChunkPos.containing(fixture.anchor).pack()).stream()
                            .filter(t -> t.getType() == ChunkTickets.ANKERSTONE_TICKET)
                            .map(t -> t.getTicketLevel()).toList();
                    require(levels.equals(active ? List.of(expected) : List.of()),
                            fixture.id + " startup ticket levels: " + levels);
                }
            }
        } catch (Exception failure) {
            finish(server, failure);
        }
    }

    private void tick(MinecraftServer server) {
        if (finished || directory == null) return;
        try {
            ticks++;
            require(System.nanoTime() < expires && ticks < 12000,
                    "Restart phase timed out at step " + step + ": " + diagnostics(server));
            require(server.getPlayerCount() == 0, "A player joined the restart-test server");
            if (phase.equals("prepare")) prepare(server);
            else if (phase.equals("restore")) restore(server);
            else released(server);
        } catch (Exception failure) {
            finish(server, failure);
        }
    }

    private void prepare(MinecraftServer server) throws IOException {
        if (step == 0) {
            if (!fixtures.stream().allMatch(f -> f.initiallyTicked(server))) return;
            if (!credits.movementFinished(server, baseline)) return;
            for (Fixture fixture : fixtures) {
                if (fixture.negative) fixture.release(fixture.id.equals("Removed"));
            }
            step = 1;
        } else if (step == 1) {
            if (!fixtures.stream().filter(f -> f.negative).allMatch(Fixture::unloaded)) return;
            snapshot(server, true);
            stepStarted = ticks;
            step = 2;
        } else {
            assertNegativeFixtures(server);
            if (ticks - stepStarted < SETTLE_TICKS + OBSERVE_TICKS) return;
            require(activeProgressed(server, OBSERVE_TICKS), "Active fixtures must tick throughout preparation");
            snapshot(server, true);
            writeBaseline();
            finish(server, null);
        }
    }

    private void restore(MinecraftServer server) throws IOException {
        if (step == 0) {
            assertNegativeFixtures(server);
            if (ticks < SETTLE_TICKS + OBSERVE_TICKS || !activeProgressed(server, OBSERVE_TICKS)) return;
            credits.activateAndDeliver(server, baseline);
            for (Fixture fixture : fixtures) {
                if (!fixture.negative) {
                    require(OwnerData.get(fixture.level).getOwnerModes().get(fixture.anchor) == fixture.mode,
                            fixture.id + " saved owner was not restored");
                    fixture.release(false);
                }
            }
            step = 1;
        } else {
            if (!fixtures.stream().allMatch(Fixture::unloaded)) return;
            for (Fixture fixture : fixtures) {
                require(!OwnerData.get(fixture.level).getOwnerModes().containsKey(fixture.anchor),
                        fixture.id + " must not retain a released owner");
            }
            snapshot(server, false);
            baseline.setProperty("restoreProcess", process);
            writeBaseline();
            finish(server, null);
        }
    }

    private void released(MinecraftServer server) {
        for (Fixture fixture : fixtures) {
            require(fixture.score(server) == value(fixture.id + ".score"),
                    fixture.id + " restarted ticking after its owner was released");
            require(!OwnerData.get(fixture.level).getOwnerModes().containsKey(fixture.anchor),
                    fixture.id + " released owner returned after restart");
            require(fixture.unloaded(), fixture.id + " loaded without an owner after restart");
        }
        if (ticks >= SETTLE_TICKS + OBSERVE_TICKS) {
            credits.checkNoRepeatAndDeliverDeferred(server, baseline);
            finish(server, null);
        }
    }

    private void assertNegativeFixtures(MinecraftServer server) {
        for (Fixture fixture : fixtures) {
            if (!fixture.negative) continue;
            require(fixture.unloaded(), fixture.id + " unexpectedly loaded");
            require(!OwnerData.get(fixture.level).getOwnerModes().containsKey(fixture.anchor),
                    fixture.id + " must not have a saved active owner");
            require(fixture.score(server) == value(fixture.id + ".score"),
                    fixture.id + " counter advanced while inactive");
        }
    }

    private boolean activeProgressed(MinecraftServer server, int minimum) {
        for (Fixture fixture : fixtures) {
            if (fixture.negative) continue;
            if (fixture.score(server) - value(fixture.id + ".score") < minimum) return false;
            for (int i = 0; i < fixture.ids.size(); i++) {
                var entity = fixture.level.getEntity(fixture.ids.get(i));
                if (!(entity instanceof ItemEntity item)
                        || !fixture.level.isPositionEntityTicking(item.blockPosition())
                        || item.getAge() - value(fixture.id + ".age." + i) < minimum) return false;
            }
        }
        return true;
    }

    private void snapshot(MinecraftServer server, boolean includeAges) {
        for (Fixture fixture : fixtures) {
            baseline.setProperty(fixture.id + ".score", Integer.toString(fixture.score(server)));
            for (int i = 0; i < fixture.ids.size(); i++) {
                baseline.setProperty(fixture.id + ".uuid." + i, fixture.ids.get(i).toString());
                if (includeAges && !fixture.negative) {
                    var entity = fixture.level.getEntity(fixture.ids.get(i));
                    require(entity instanceof ItemEntity, fixture.id + " item probe is missing");
                    baseline.setProperty(fixture.id + ".age." + i, Integer.toString(((ItemEntity) entity).getAge()));
                }
            }
        }
    }

    private int value(String key) {
        return Integer.parseInt(baseline.getProperty(key));
    }

    private void writeBaseline() throws IOException {
        try (var output = Files.newOutputStream(directory.resolve("baseline.properties"))) {
            baseline.store(output, "Counters, entity ages, and identities carried across real server restarts");
        }
    }

    private String diagnostics(MinecraftServer server) {
        return fixtures.stream().map(f -> f.id + ": score=" + f.score(server) + ", unloaded=" + f.unloaded())
                .toList().toString();
    }

    private void finish(MinecraftServer server, Exception failure) {
        if (finished) return;
        finished = true;
        Properties result = new Properties();
        result.setProperty("run", run);
        result.setProperty("phase", phase);
        result.setProperty("process", process);
        result.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
        result.setProperty("ticks", Integer.toString(ticks));
        result.setProperty("result", failure == null ? "pass" : "fail");
        if (failure != null) {
            result.setProperty("error", failure.toString());
            Ankerstone.LOGGER.error("Restart check failed: " + phase, failure);
        }
        try (var output = Files.newOutputStream(directory.resolve(phase + ".properties"))) {
            result.store(output, "Restart phase result; the runner also requires a clean process exit");
        } catch (IOException error) {
            Ankerstone.LOGGER.error("Cannot write restart result", error);
        }
        server.halt(false);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class Fixture {
        final ServerLevel level;
        final String id;
        final BlockPos anchor;
        final AnkerstoneMode mode;
        final boolean negative;
        final List<UUID> ids = new ArrayList<>();

        Fixture(ServerLevel level, String id, int x, AnkerstoneMode mode, boolean negative) {
            this.level = level;
            this.id = id;
            anchor = new BlockPos(x, 200, 10008);
            this.mode = mode;
            this.negative = negative;
        }

        void create(MinecraftServer server) {
            ChunkPos center = ChunkPos.containing(anchor);
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    level.getChunk(center.x() + dx, center.z() + dz);
                }
            }
            power(mode == AnkerstoneMode.WIDE ? 15 : 1);
            level.setBlock(anchor, ModBlocks.ANKERSTONE.defaultBlockState(), Block.UPDATE_ALL);
            BlockPos counter = anchor.offset(3, 0, 3);
            level.setBlock(counter, Blocks.REPEATING_COMMAND_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            var command = (CommandBlockEntity) level.getBlockEntity(counter);
            command.getCommandBlock().setCommand("scoreboard players add " + id + " restart_ticks 1");
            command.setAutomatic(true);
            server.getScoreboard().getOrCreatePlayerScore(ScoreHolder.forNameOnly(id),
                    server.getScoreboard().getObjective("restart_ticks")).set(0);
            List<BlockPos> positions = new ArrayList<>(List.of(anchor.above(2)));
            if (mode == AnkerstoneMode.WIDE) {
                for (int dx : new int[] {-16, 16}) {
                    for (int dz : new int[] {-16, 16}) positions.add(anchor.offset(dx, 2, dz));
                }
            }
            var entities = ((ServerLevelAccessor) level).ankerstone$getEntityManager();
            long started = System.nanoTime();
            server.managedBlock(() -> {
                entities.processPendingLoads();
                return positions.stream().allMatch(p -> level.areEntitiesLoaded(ChunkPos.containing(p).pack()))
                        || System.nanoTime() - started >= TimeUnit.SECONDS.toNanos(10);
            });
            for (BlockPos position : positions) {
                require(level.areEntitiesLoaded(ChunkPos.containing(position).pack()), id + " probe data not ready");
                ItemEntity item = new ItemEntity(level, position.getX() + 0.5, position.getY(),
                        position.getZ() + 0.5, new ItemStack(Items.DIAMOND), 0, 0, 0);
                item.setNoGravity(true);
                item.setPermanentlyInvulnerable(true);
                item.setNeverPickUp();
                require(level.addFreshEntity(item), id + " probe could not be added");
                ids.add(item.getUUID());
            }
        }

        void readIds(Properties baseline) {
            int count = mode == AnkerstoneMode.SINGLE ? 1 : 5;
            for (int i = 0; i < count; i++) ids.add(UUID.fromString(baseline.getProperty(id + ".uuid." + i)));
        }

        boolean initiallyTicked(MinecraftServer server) {
            if (score(server) < 10) return false;
            return ids.stream().allMatch(uuid -> level.getEntity(uuid) instanceof ItemEntity item && item.getAge() >= 340);
        }

        void release(boolean remove) {
            if (remove) level.setBlock(anchor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            else power(0);
        }

        void power(int strength) {
            for (int distance = 1; distance <= 3; distance++) {
                level.setBlock(anchor.west(distance), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            level.setBlock(anchor.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            if (strength == 15) {
                level.setBlock(anchor.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            } else if (strength > 0) {
                for (int distance = 1; distance <= 2; distance++) {
                    level.setBlock(anchor.west(distance).below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                }
                level.setBlock(anchor.west(3), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(anchor.west(2), Blocks.REDSTONE_WIRE.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(anchor.west(), Blocks.REDSTONE_WIRE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        int score(MinecraftServer server) {
            var objective = server.getScoreboard().getObjective("restart_ticks");
            require(objective != null, "Saved counter objective is missing");
            var score = server.getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly(id), objective);
            require(score != null, id + " saved counter is missing");
            return score.value();
        }

        boolean unloaded() {
            ChunkPos center = ChunkPos.containing(anchor);
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int x = center.x() + dx;
                    int z = center.z() + dz;
                    if (level.getChunkSource().getChunkNow(x, z) != null
                            || level.areEntitiesLoaded(new ChunkPos(x, z).pack())) return false;
                }
            }
            return true;
        }
    }
}
