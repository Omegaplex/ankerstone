package io.github.omegaplex.ankerstone;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;

public class CommandTests {
    @GameTest
    public void levelFourCanQueryLimit(GameTestHelper helper)
            throws CommandSyntaxException {
        var server = helper.getLevel().getServer();
        LimitData limits = LimitData.get(server);
        int previousLimit = limits.getActiveLimit();
        long previousCount = ChunkTickets.getActiveCount(server);

        var source = server.createCommandSourceStack()
                .withPermission(LevelBasedPermissionSet.OWNER);

        int result = server.getCommands().getDispatcher()
                .execute("ankerstone limit", source);

        helper.assertTrue(result == 1,
                "Level-four query must report success");
        helper.assertTrue(limits.getActiveLimit() == previousLimit,
                "Query must not change the limit");
        helper.assertTrue(ChunkTickets.getActiveCount(server) == previousCount,
                "Query must not change active ownership");
        helper.succeed();
    }

    @GameTest
    public void lowerLevelsCannotQueryLimit(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        LimitData limits = LimitData.get(server);
        int previousLimit = limits.getActiveLimit();

        for (int level = 0; level < 4; level++) {
            var source = server.createCommandSourceStack()
                    .withPermission(LevelBasedPermissionSet.forLevel(
                            PermissionLevel.byId(level)));

            boolean denied = false;
            try {
                server.getCommands().getDispatcher()
                        .execute("ankerstone limit", source);
            } catch (CommandSyntaxException expected) {
                denied = true;
            }

            helper.assertTrue(denied,
                    "Operator level " + level + " must not access the limit command");
            helper.assertTrue(limits.getActiveLimit() == previousLimit,
                    "Denied query must not change the limit");
        }

        helper.succeed();
    }

    @GameTest(environment = "ankerstone-test:limit_commands")
    public void levelFourCanSetFiniteLimits(GameTestHelper helper)
            throws CommandSyntaxException {
        var server = helper.getLevel().getServer();
        LimitData limits = LimitData.get(server);
        int previousLimit = limits.getActiveLimit();
        long previousCount = ChunkTickets.getActiveCount(server);
        var source = server.createCommandSourceStack()
                .withPermission(LevelBasedPermissionSet.OWNER);

        try {
            limits.setActiveLimit(LimitData.UNLIMITED);

            for (int limit : new int[] { 7, 0, Integer.MAX_VALUE }) {
                int result = server.getCommands().getDispatcher()
                        .execute("ankerstone limit " + limit, source);

                helper.assertTrue(result == 1,
                        "Setting a finite limit must report success");
                helper.assertTrue(limits.getActiveLimit() == limit,
                        "Command must store the requested limit: " + limit);
                helper.assertFalse(limits.isUnlimited(),
                        "A finite command value must not mean unlimited");
                helper.assertTrue(
                        ChunkTickets.getActiveCount(server) == previousCount,
                        "Changing the limit must not change active ownership");
            }
        } finally {
            limits.setActiveLimit(previousLimit);
        }

        helper.succeed();
    }

    @GameTest(environment = "ankerstone-test:limit_commands")
    public void levelFourCanSetUnlimited(GameTestHelper helper)
            throws CommandSyntaxException {
        var server = helper.getLevel().getServer();
        LimitData limits = LimitData.get(server);
        int previousLimit = limits.getActiveLimit();
        long previousCount = ChunkTickets.getActiveCount(server);
        var source = server.createCommandSourceStack()
                .withPermission(LevelBasedPermissionSet.OWNER);

        try {
            limits.setActiveLimit(0);

            int result = server.getCommands().getDispatcher()
                    .execute("ankerstone limit unlimited", source);

            helper.assertTrue(result == 1,
                    "Setting unlimited must report success");
            helper.assertTrue(limits.isUnlimited(),
                    "Unlimited command must remove the finite ceiling");
            helper.assertTrue(
                    limits.getActiveLimit() == LimitData.UNLIMITED,
                    "Unlimited command must store the sentinel value");
            helper.assertTrue(
                    ChunkTickets.getActiveCount(server) == previousCount,
                    "Setting unlimited must not change active ownership");
        } finally {
            limits.setActiveLimit(previousLimit);
        }

        helper.succeed();
    }

    @GameTest(environment = "ankerstone-test:limit_commands")
    public void lowerLevelsCannotChangeLimit(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        LimitData limits = LimitData.get(server);
        int previousLimit = limits.getActiveLimit();
        long previousCount = ChunkTickets.getActiveCount(server);

        try {
            limits.setActiveLimit(7);

            for (int level = 0; level < 4; level++) {
                var source = server.createCommandSourceStack()
                        .withPermission(LevelBasedPermissionSet.forLevel(
                                PermissionLevel.byId(level)));

                for (String command : new String[] {
                        "ankerstone limit 0",
                        "ankerstone limit unlimited"
                }) {
                    boolean denied = false;
                    try {
                        server.getCommands().getDispatcher().execute(command, source);
                    } catch (CommandSyntaxException expected) {
                        denied = true;
                    }

                    helper.assertTrue(denied,
                            "Operator level " + level + " must be denied: " + command);
                    helper.assertTrue(limits.getActiveLimit() == 7,
                            "Denied command must preserve the limit");
                    helper.assertTrue(
                            ChunkTickets.getActiveCount(server) == previousCount,
                            "Denied command must preserve active ownership");
                }
            }
        } finally {
            limits.setActiveLimit(previousLimit);
        }

        helper.succeed();
    }

    @GameTest(environment = "ankerstone-test:limit_commands")
    public void invalidLimitCommandsPreserveSetting(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        LimitData limits = LimitData.get(server);
        int previousLimit = limits.getActiveLimit();
        long previousCount = ChunkTickets.getActiveCount(server);
        var source = server.createCommandSourceStack()
                .withPermission(LevelBasedPermissionSet.OWNER);

        try {
            limits.setActiveLimit(7);

            for (String argument : new String[] {
                    "-1", "-2", "2147483648", "1.5", "many", "7 extra"
            }) {
                boolean rejected = false;
                try {
                    server.getCommands().getDispatcher()
                            .execute("ankerstone limit " + argument, source);
                } catch (CommandSyntaxException expected) {
                    rejected = true;
                }

                helper.assertTrue(rejected,
                        "Invalid limit argument must be rejected: " + argument);
                helper.assertTrue(limits.getActiveLimit() == 7,
                        "Rejected command must preserve the setting");
                helper.assertTrue(
                        ChunkTickets.getActiveCount(server) == previousCount,
                        "Rejected command must preserve active ownership");
            }
        } finally {
            limits.setActiveLimit(previousLimit);
        }

        helper.succeed();
    }
}
