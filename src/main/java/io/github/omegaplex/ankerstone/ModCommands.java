package io.github.omegaplex.ankerstone;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import com.mojang.brigadier.arguments.IntegerArgumentType;

public class ModCommands {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> {
                    dispatcher.register(Commands.literal("ankerstone")
                            .requires(source -> source.permissions()
                                    .hasPermission(Permissions.COMMANDS_OWNER))
                            .then(Commands.literal("limit")
                                    .executes(context -> showLimit(context.getSource()))
                                    .then(Commands.literal("unlimited")
                                            .executes(context -> setLimit(
                                                    context.getSource(),
                                                    LimitData.UNLIMITED)))
                                    .then(Commands.argument("value",
                                            IntegerArgumentType.integer(0))
                                            .executes(context -> setLimit(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(
                                                            context, "value"))))));
                });
    }

    private static int showLimit(CommandSourceStack source) {
        var server = source.getServer();
        LimitData data = LimitData.get(server);
        String limit = data.isUnlimited()
                ? "unlimited"
                : Integer.toString(data.getActiveLimit());
        long active = ChunkTickets.getActiveCount(server);

        source.sendSuccess(() -> Component.literal(
                "Ankerstone limit: " + limit + ". Active: " + active + "."), false);
        return 1;
    }

    private static int setLimit(CommandSourceStack source, int limit) {
        LimitData.get(source.getServer()).setActiveLimit(limit);
        String text = limit == LimitData.UNLIMITED
                ? "unlimited"
                : Integer.toString(limit);

        source.sendSuccess(() -> Component.literal(
                "Ankerstone limit set to " + text + "."), true);
        return 1;
    }
}