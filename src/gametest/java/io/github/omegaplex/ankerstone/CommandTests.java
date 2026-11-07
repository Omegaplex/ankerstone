package io.github.omegaplex.ankerstone;

import java.util.List;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

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

        @GameTest
        public void ownerListSortsByDimensionThenCoordinates(GameTestHelper helper) {
                var end = new ChunkTickets.ActiveOwner(
                                Level.END, BlockPos.ZERO, AnkerstoneMode.SINGLE);
                var nether = new ChunkTickets.ActiveOwner(
                                Level.NETHER, BlockPos.ZERO, AnkerstoneMode.WIDE);
                var lowX = new ChunkTickets.ActiveOwner(
                                Level.OVERWORLD, new BlockPos(-10, 20, 30),
                                AnkerstoneMode.SINGLE);
                var lowY = new ChunkTickets.ActiveOwner(
                                Level.OVERWORLD, new BlockPos(2, -10, 30),
                                AnkerstoneMode.WIDE);
                var lowZ = new ChunkTickets.ActiveOwner(
                                Level.OVERWORLD, new BlockPos(2, 3, -10),
                                AnkerstoneMode.SINGLE);
                var highZ = new ChunkTickets.ActiveOwner(
                                Level.OVERWORLD, new BlockPos(2, 3, 2),
                                AnkerstoneMode.WIDE);

                var input = List.of(end, highZ, nether, lowZ, lowY, lowX);
                var sorted = ModCommands.sortOwners(input);

                helper.assertTrue(
                                sorted.equals(List.of(lowX, lowY, lowZ, highZ, nether, end)),
                                "Owners must sort Overworld, Nether, End, then by numeric X, Y, Z");
                helper.assertTrue(
                                input.equals(List.of(end, highZ, nether, lowZ, lowY, lowX)),
                                "Sorting must not rearrange the input list");
                helper.assertTrue(ModCommands.sortOwners(List.of()).isEmpty(),
                                "Sorting an empty list must return an empty list");

                helper.succeed();
        }

        @GameTest
        public void ownerListingPreservesEntriesAndExpires(GameTestHelper helper) {
                UUID id = UUID.randomUUID();
                long created = 1000L;
                long lifetime = Duration.ofMinutes(5).toNanos();

                var owner = new ChunkTickets.ActiveOwner(
                                Level.OVERWORLD, BlockPos.ZERO, AnkerstoneMode.SINGLE);
                var input = new ArrayList<>(List.of(owner));
                var listing = new ModCommands.OwnerListing(id, input, created);

                input.clear();

                helper.assertTrue(listing.id().equals(id),
                                "Listing must preserve its identifier");
                helper.assertTrue(listing.owners().equals(List.of(owner)),
                                "Changing the input must not change the listing");

                boolean rejected = false;
                try {
                        listing.owners().clear();
                } catch (UnsupportedOperationException expected) {
                        rejected = true;
                }
                helper.assertTrue(rejected,
                                "Listing entries must be unmodifiable");

                helper.assertFalse(listing.isExpired(created),
                                "A new listing must not be expired");
                helper.assertFalse(listing.isExpired(created + lifetime - 1),
                                "Listing must remain valid just before five minutes");
                helper.assertTrue(listing.isExpired(created + lifetime),
                                "Listing must expire at five minutes");
                helper.assertTrue(listing.isExpired(created + lifetime + 1),
                                "Listing must remain expired afterward");

                helper.succeed();
        }

        @GameTest
        public void ownerListingPaginatesWithoutLosingEntries(GameTestHelper helper) {
                for (int count : new int[] { 0, 1, 9, 10, 11, 20, 21 }) {
                        var owners = new ArrayList<ChunkTickets.ActiveOwner>();
                        for (int i = 0; i < count; i++) {
                                owners.add(new ChunkTickets.ActiveOwner(
                                                Level.OVERWORLD, new BlockPos(i, 64, 0),
                                                AnkerstoneMode.SINGLE));
                        }

                        var listing = new ModCommands.OwnerListing(
                                        UUID.randomUUID(), owners, 0L);
                        int expectedPages = Math.max(1, (count + 9) / 10);

                        helper.assertTrue(listing.pageCount() == expectedPages,
                                        "Incorrect page count for " + count + " owners");

                        var collected = new ArrayList<ChunkTickets.ActiveOwner>();
                        for (int page = 1; page <= expectedPages; page++) {
                                var entries = listing.pageEntries(page);
                                int expectedSize = Math.min(10, count - (page - 1) * 10);

                                helper.assertTrue(entries.size() == expectedSize,
                                                "Incorrect entry count on page " + page
                                                                + " for " + count + " owners");
                                collected.addAll(entries);
                        }

                        helper.assertTrue(collected.equals(owners),
                                        "Pages must preserve every entry in its original order");
                }

                helper.succeed();
        }

        @GameTest
        public void ownerListingRejectsInvalidPages(GameTestHelper helper) {
                var owners = new ArrayList<ChunkTickets.ActiveOwner>();
                for (int i = 0; i < 11; i++) {
                        owners.add(new ChunkTickets.ActiveOwner(
                                        Level.OVERWORLD, new BlockPos(i, 64, 0),
                                        AnkerstoneMode.SINGLE));
                }

                var listing = new ModCommands.OwnerListing(
                                UUID.randomUUID(), owners, 0L);
                var empty = new ModCommands.OwnerListing(
                                UUID.randomUUID(), List.of(), 0L);

                for (int page : new int[] {
                                Integer.MIN_VALUE, -1, 0, 3, Integer.MAX_VALUE }) {
                        boolean rejected = false;
                        try {
                                listing.pageEntries(page);
                        } catch (IllegalArgumentException expected) {
                                rejected = true;
                        }
                        helper.assertTrue(rejected,
                                        "Listing must reject invalid page " + page);
                }

                boolean rejected = false;
                try {
                        empty.pageEntries(2);
                } catch (IllegalArgumentException expected) {
                        rejected = true;
                }
                helper.assertTrue(rejected,
                                "An empty listing must reject page two");
                helper.assertTrue(empty.pageEntries(1).isEmpty(),
                                "An empty listing must allow its empty first page");
                helper.assertTrue(listing.owners().equals(owners),
                                "Rejected requests must not change the snapshot");

                helper.succeed();
        }

        @GameTest
        public void listingStoreKeepsPlayersSeparate(GameTestHelper helper) {
                var store = new ModCommands.ListingStore();
                UUID firstPlayer = UUID.randomUUID();
                UUID secondPlayer = UUID.randomUUID();
                long now = 1000L;

                var firstOwner = new ChunkTickets.ActiveOwner(
                                Level.OVERWORLD, BlockPos.ZERO, AnkerstoneMode.SINGLE);
                var secondOwner = new ChunkTickets.ActiveOwner(
                                Level.NETHER, new BlockPos(10, 64, 20), AnkerstoneMode.WIDE);

                helper.assertTrue(store.get(firstPlayer, now) == null,
                                "A player without a listing must have no selection");

                var first = store.create(firstPlayer, List.of(firstOwner), now);
                var second = store.create(secondPlayer, List.of(secondOwner), now);

                helper.assertTrue(store.get(firstPlayer, now).equals(first),
                                "Creating another player's listing must preserve the first");
                helper.assertTrue(store.get(secondPlayer, now).equals(second),
                                "Each player must retrieve their own listing");

                var replacement = store.create(
                                firstPlayer, List.of(secondOwner), now + 1);

                helper.assertFalse(replacement.id().equals(first.id()),
                                "Refreshing a listing must assign a new identifier");
                helper.assertTrue(store.get(firstPlayer, now + 1).equals(replacement),
                                "Refreshing must replace that player's previous listing");
                helper.assertTrue(store.get(secondPlayer, now + 1).equals(second),
                                "Refreshing must not replace another player's listing");

                store.remove(firstPlayer);

                helper.assertTrue(store.get(firstPlayer, now + 1) == null,
                                "Removing a player must discard their listing");
                helper.assertTrue(store.get(secondPlayer, now + 1).equals(second),
                                "Removing one player must preserve the other");

                store.clear();

                helper.assertTrue(store.get(secondPlayer, now + 1) == null,
                                "Clearing the store must discard remaining listings");
                helper.succeed();
        }

        @GameTest
        public void listingStoreExpiresWithoutRefreshingOnRead(GameTestHelper helper) {
                var store = new ModCommands.ListingStore();
                UUID firstPlayer = UUID.randomUUID();
                UUID secondPlayer = UUID.randomUUID();
                long created = 1000L;
                long lifetime = Duration.ofMinutes(5).toNanos();

                var owner = new ChunkTickets.ActiveOwner(
                                Level.OVERWORLD, BlockPos.ZERO, AnkerstoneMode.SINGLE);
                var first = store.create(firstPlayer, List.of(owner), created);

                helper.assertTrue(
                                first.equals(store.get(firstPlayer, created + lifetime - 1)),
                                "Browsing just before expiration must return the same listing");

                var second = store.create(
                                secondPlayer, List.of(owner), created + lifetime - 1);

                helper.assertTrue(store.get(firstPlayer, created + lifetime) == null,
                                "Browsing must not extend the first listing's lifetime");
                helper.assertTrue(
                                second.equals(store.get(secondPlayer, created + lifetime)),
                                "Expiring one listing must preserve another valid listing");

                helper.assertTrue(
                                store.get(secondPlayer, second.createdAtNanos() + lifetime) == null,
                                "The second listing must expire at its own deadline");
                helper.assertTrue(
                                store.get(firstPlayer, second.createdAtNanos() + lifetime) == null,
                                "An expired listing must remain absent");

                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:listing_commands")
        public void clearingServerListingsDiscardsSnapshots(GameTestHelper helper) {
                var server = helper.getLevel().getServer();
                UUID playerId = UUID.randomUUID();
                long now = 1000L;

                var owner = new ChunkTickets.ActiveOwner(
                                Level.OVERWORLD, BlockPos.ZERO, AnkerstoneMode.SINGLE);

                try {
                        var original = ModCommands.listingsFor(server);
                        var listing = original.create(playerId, List.of(owner), now);

                        helper.assertTrue(
                                        ModCommands.listingsFor(server).get(playerId, now).equals(listing),
                                        "Repeated access must use the same server storage");

                        ModCommands.clearListings(server);

                        helper.assertTrue(original.get(playerId, now) == null,
                                        "Clearing must empty the previous storage");

                        var replacement = ModCommands.listingsFor(server);

                        helper.assertTrue(replacement != original,
                                        "Access after clearing must create fresh storage");
                        helper.assertTrue(replacement.get(playerId, now) == null,
                                        "Fresh storage must not inherit previous selections");
                } finally {
                        ModCommands.clearListings(server);
                }

                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:listing_commands")
        public void consoleCanListWithoutChangingOwners(GameTestHelper helper)
                        throws CommandSyntaxException {
                var server = helper.getLevel().getServer();
                var ownersBefore = ChunkTickets.getActiveOwners(server);
                int limitBefore = LimitData.get(server).getActiveLimit();

                int result = server.getCommands().getDispatcher().execute(
                                "ankerstone list", server.createCommandSourceStack());

                helper.assertTrue(result == 1,
                                "Console listing must report success");
                helper.assertTrue(
                                ChunkTickets.getActiveOwners(server).equals(ownersBefore),
                                "Listing must not change active owners");
                helper.assertTrue(
                                LimitData.get(server).getActiveLimit() == limitBefore,
                                "Listing must not change the active limit");

                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:listing_commands")
        public void lowerLevelsCannotListOwners(GameTestHelper helper) {
                var server = helper.getLevel().getServer();
                var ownersBefore = ChunkTickets.getActiveOwners(server);

                for (int level = 0; level < 4; level++) {
                        var source = server.createCommandSourceStack()
                                        .withPermission(LevelBasedPermissionSet.forLevel(
                                                        PermissionLevel.byId(level)));

                        boolean denied = false;
                        try {
                                server.getCommands().getDispatcher()
                                                .execute("ankerstone list", source);
                        } catch (CommandSyntaxException expected) {
                                denied = true;
                        }

                        helper.assertTrue(denied,
                                        "Operator level " + level + " must not list owners");
                }

                helper.assertTrue(
                                ChunkTickets.getActiveOwners(server).equals(ownersBefore),
                                "Denied commands must not change active owners");
                helper.succeed();
        }

        @GameTest
        public void consoleListingIsSeparateAndExpires(GameTestHelper helper) {
                var store = new ModCommands.ListingStore();
                UUID playerId = UUID.randomUUID();
                long now = 1000L;
                long lifetime = Duration.ofMinutes(5).toNanos();

                var owner = new ChunkTickets.ActiveOwner(
                                Level.OVERWORLD, BlockPos.ZERO, AnkerstoneMode.SINGLE);

                helper.assertTrue(store.getConsole(now) == null,
                                "Console must initially have no listing");

                var playerListing = store.create(playerId, List.of(owner), now);
                var consoleListing = store.createConsole(List.of(), now);

                helper.assertTrue(store.getConsole(now).equals(consoleListing),
                                "Console must retrieve its own listing");
                helper.assertTrue(store.get(playerId, now).equals(playerListing),
                                "Creating a console listing must preserve player selections");

                var replacement = store.createConsole(List.of(owner), now + 1);

                helper.assertFalse(replacement.id().equals(consoleListing.id()),
                                "Refreshing the console list must assign a new identifier");
                helper.assertTrue(store.getConsole(now + 1).equals(replacement),
                                "Refreshing must replace the console listing");
                helper.assertTrue(store.get(playerId, now + 1).equals(playerListing),
                                "Refreshing the console must not replace player selections");

                helper.assertTrue(
                                store.getConsole(replacement.createdAtNanos() + lifetime) == null,
                                "Console listing must expire at its deadline");

                store.createConsole(List.of(owner), now + lifetime + 2);
                store.clear();

                helper.assertTrue(store.getConsole(now + lifetime + 2) == null,
                                "Clearing storage must discard the console listing");
                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:listing_commands")
        public void listPageUsesExistingSnapshot(GameTestHelper helper)
                        throws CommandSyntaxException {
                var server = helper.getLevel().getServer();
                var source = server.createCommandSourceStack();
                var store = ModCommands.listingsFor(server);
                var owners = new ArrayList<ChunkTickets.ActiveOwner>();

                for (int i = 0; i < 11; i++) {
                        owners.add(new ChunkTickets.ActiveOwner(
                                        Level.OVERWORLD, new BlockPos(i, 64, 0),
                                        AnkerstoneMode.SINGLE));
                }

                try {
                        var listing = store.createConsole(owners, System.nanoTime());

                        int result = server.getCommands().getDispatcher()
                                        .execute("ankerstone list 2", source);

                        helper.assertTrue(result == 1,
                                        "Page two of the saved listing must succeed");
                        helper.assertTrue(
                                        listing.equals(store.getConsole(System.nanoTime())),
                                        "Browsing must preserve the snapshot and creation time");

                        int invalidResult = server.getCommands().getDispatcher()
                                        .execute("ankerstone list 3", source);

                        helper.assertTrue(invalidResult == 0,
                                        "A page beyond the snapshot must report failure");
                        helper.assertTrue(
                                        listing.equals(store.getConsole(System.nanoTime())),
                                        "An invalid page must not replace the snapshot");
                } finally {
                        ModCommands.clearListings(server);
                }

                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:listing_commands")
        public void listPageRejectsMissingAndExpiredSnapshots(GameTestHelper helper)
                        throws CommandSyntaxException {
                var server = helper.getLevel().getServer();
                var source = server.createCommandSourceStack();

                ModCommands.clearListings(server);

                try {
                        int missingResult = server.getCommands().getDispatcher()
                                        .execute("ankerstone list 1", source);

                        helper.assertTrue(missingResult == 0,
                                        "Page browsing without a listing must report failure");

                        var store = ModCommands.listingsFor(server);
                        helper.assertTrue(store.getConsole(System.nanoTime()) == null,
                                        "A missing-list request must not create a snapshot");

                        long expiredCreation = System.nanoTime()
                                        - Duration.ofMinutes(5).toNanos();
                        store.createConsole(List.of(), expiredCreation);

                        int expiredResult = server.getCommands().getDispatcher()
                                        .execute("ankerstone list 1", source);

                        helper.assertTrue(expiredResult == 0,
                                        "Page browsing with an expired listing must report failure");
                        helper.assertTrue(store.getConsole(System.nanoTime()) == null,
                                        "An expired-list request must not create a replacement");
                } finally {
                        ModCommands.clearListings(server);
                }

                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:listing_commands")
        @SuppressWarnings("removal")
        public void playerListCreatesAndReusesPersonalSnapshot(GameTestHelper helper)
                        throws CommandSyntaxException {
                var server = helper.getLevel().getServer();
                var player = helper.makeMockServerPlayerInLevel();
                var source = server.createCommandSourceStack()
                                .withEntity(player)
                                .withPermission(LevelBasedPermissionSet.OWNER);

                try {
                        var store = ModCommands.listingsFor(server);
                        var console = store.createConsole(List.of(), System.nanoTime());

                        int result = server.getCommands().getDispatcher()
                                        .execute("ankerstone list", source);

                        var personal = store.get(player.getUUID(), System.nanoTime());

                        helper.assertTrue(result == 1 && personal != null,
                                        "Player listing must create a personal snapshot");
                        helper.assertTrue(
                                        personal.owners().equals(ModCommands.sortOwners(
                                                        ChunkTickets.getActiveOwners(server))),
                                        "Personal snapshot must contain the current sorted owners");

                        int pageResult = server.getCommands().getDispatcher()
                                        .execute("ankerstone list 1", source);

                        helper.assertTrue(pageResult == 1,
                                        "Player must be able to browse their listing");
                        helper.assertTrue(
                                        personal.equals(store.get(player.getUUID(), System.nanoTime())),
                                        "Browsing must reuse the player's existing snapshot");
                        helper.assertTrue(console.equals(store.getConsole(System.nanoTime())),
                                        "Player commands must not replace the console snapshot");
                } finally {
                        ModCommands.clearListings(server);
                        server.getPlayerList().remove(player);
                }

                helper.succeed();
        }

        @GameTest(environment = "ankerstone-test:listing_commands")
        @SuppressWarnings("removal")
        public void visitRequiresSpectatorAndCurrentActiveEntry(GameTestHelper helper) {
                var level = helper.getLevel();
                var server = level.getServer();
                var cookie = CommonListenerCookie.createInitial(
                                new GameProfile(UUID.randomUUID(), "visit-test"), false);
                var player = new ServerPlayer(
                                server, level, cookie.gameProfile(), cookie.clientInformation()) {
                        GameType testMode = GameType.CREATIVE;

                        @Override
                        public GameType gameMode() {
                                return testMode;
                        }
                };
                var source = server.createCommandSourceStack()
                                .withEntity(player)
                                .withPermission(LevelBasedPermissionSet.OWNER);
                var pos = helper.absolutePos(new BlockPos(2, 2, 2));
                var owner = new ChunkTickets.ActiveOwner(
                                level.dimension(), pos, AnkerstoneMode.SINGLE);

                try {
                        ChunkTickets.add(level, pos, AnkerstoneMode.SINGLE);
                        var store = ModCommands.listingsFor(server);
                        var listing = store.create(
                                        player.getUUID(), List.of(owner), System.nanoTime());

                        helper.assertTrue(
                                        ModCommands.findVisitTarget(source, listing.id(), 1) == null,
                                        "Creative players must not be allowed to visit");

                        player.testMode = GameType.SPECTATOR;
                        helper.assertTrue(player.isSpectator(),
                                        "Visit test fixture must report Spectator mode");
                        helper.assertTrue(
                                        owner.equals(ModCommands.findVisitTarget(source, listing.id(), 1)),
                                        "Spectator must resolve the current active entry");

                        var refreshed = store.create(
                                        player.getUUID(), List.of(owner), System.nanoTime());
                        helper.assertTrue(
                                        ModCommands.findVisitTarget(source, listing.id(), 1) == null,
                                        "Refreshing must invalidate the old link even if entry 1 is unchanged");

                        ChunkTickets.remove(level, pos, AnkerstoneMode.SINGLE);
                        helper.assertTrue(
                                        ModCommands.findVisitTarget(source, refreshed.id(), 1) == null,
                                        "A current link must reject a deactivated owner");
                } finally {
                        ChunkTickets.remove(level, pos, AnkerstoneMode.SINGLE);
                        ModCommands.clearListings(server);

                }

                helper.succeed();
        }
}
