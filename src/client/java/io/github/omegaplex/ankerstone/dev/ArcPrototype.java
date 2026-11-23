package io.github.omegaplex.ankerstone.dev;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import io.github.omegaplex.ankerstone.AnkerstoneBlock;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Development controls only; never changes blocks, redstone, or chunk tickets. */
public final class ArcPrototype {
    private static final int MAX_POSITIONS = 8;
    private static final Map<BlockPos, ArcInstance> INSTANCES = new LinkedHashMap<>();
    private static ClientLevel selectedLevel;

    private ArcPrototype() { }

    public static void initialize() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return;
        }

        ArcRenderer.initialize();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            var root = literal("ankerstonearcs")
                    .requires(FabricClientCommandSource::attended)
                    .then(literal("start").executes(context -> start(context.getSource())))
                    .then(literal("stop").executes(context -> {
                        clear();
                        context.getSource().sendFeedback(Component.literal("Arc prototype positions cleared."));
                        return 1;
                    }))
                    .then(literal("status").executes(context -> {
                        var source = context.getSource();
                        resetIfLevelChanged(source.getLevel());
                        source.sendFeedback(Component.literal("Arc prototype: " + INSTANCES.size()
                                + " selected positions. Region 0 = top gem, 1 = recesses, 2 = recesses and exterior frame."));
                        for (var option : ArcOption.values()) {
                            source.sendFeedback(Component.literal(option.commandName() + " = " + option.value()));
                        }
                        return 1;
                    }))
                    .then(literal("reset").executes(context -> {
                        for (var option : ArcOption.values()) {
                            option.reset();
                        }
                        context.getSource().sendFeedback(Component.literal("Arc tuning reset to defaults."));
                        return 1;
                    }));

            for (var option : ArcOption.values()) {
                root.then(literal(option.commandName())
                        .then(argument("value", DoubleArgumentType.doubleArg(option.minimum(), option.maximum()))
                                .executes(context -> {
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    if (!option.set(value)) {
                                        context.getSource().sendError(Component.literal("Use a whole number."));
                                        return 0;
                                    }
                                    context.getSource().sendFeedback(Component.literal(
                                            option.commandName() + " = " + value));
                                    return 1;
                                })));
            }
            dispatcher.register(root);
        });
    }

    private static int start(FabricClientCommandSource source) {
        resetIfLevelChanged(source.getLevel());
        HitResult hit = source.getPlayer().pick(12.0, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                || !(source.getLevel().getBlockState(blockHit.getBlockPos()).getBlock() instanceof AnkerstoneBlock)) {
            source.sendError(Component.literal("Look at an Ankerstone within 12 blocks."));
            return 0;
        }
        BlockPos position = blockHit.getBlockPos().immutable();
        if (INSTANCES.containsKey(position)) {
            source.sendFeedback(Component.literal("That Ankerstone is already selected."));
            return 1;
        }
        if (INSTANCES.size() >= MAX_POSITIONS) {
            source.sendError(Component.literal("Eight positions maximum; use /ankerstonearcs stop to clear."));
            return 0;
        }
        INSTANCES.put(position, new ArcInstance(position.asLong(), System.nanoTime()));
        source.sendFeedback(Component.literal("Selected " + position.toShortString()
                + ". Inspect the selected gem or recess region."));
        return 1;
    }

    private static void resetIfLevelChanged(ClientLevel level) {
        if (selectedLevel != level) {
            clear();
            selectedLevel = level;
        }
    }

    static Map<BlockPos, ArcInstance> instances(ClientLevel level) {
        resetIfLevelChanged(level);
        return INSTANCES;
    }

    private static void clear() {
        INSTANCES.clear();
        selectedLevel = null;
    }
}
