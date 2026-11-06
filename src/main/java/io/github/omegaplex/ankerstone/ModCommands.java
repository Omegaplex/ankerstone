package io.github.omegaplex.ankerstone;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import com.mojang.brigadier.arguments.IntegerArgumentType;

public class ModCommands {
    private static final long LIST_LIFETIME_NANOS = Duration.ofMinutes(5).toNanos();
    private static final int LIST_PAGE_SIZE = 10;
    private static final Map<MinecraftServer, ListingStore> LISTINGS = new HashMap<>();

    record OwnerListing(
            UUID id,
            List<ChunkTickets.ActiveOwner> owners,
            long createdAtNanos) {

        OwnerListing {
            owners = List.copyOf(owners);
        }

        boolean isExpired(long nowNanos) {
            return nowNanos - createdAtNanos >= LIST_LIFETIME_NANOS;
        }

        int pageCount() {
            return owners.isEmpty()
                    ? 1
                    : 1 + (owners.size() - 1) / LIST_PAGE_SIZE;
        }

        List<ChunkTickets.ActiveOwner> pageEntries(int page) {
            if (page < 1 || page > pageCount()) {
                throw new IllegalArgumentException(
                        "Page must be between 1 and " + pageCount());
            }

            int start = (page - 1) * LIST_PAGE_SIZE;
            int end = start + Math.min(LIST_PAGE_SIZE, owners.size() - start);
            return owners.subList(start, end);
        }
    }

    static final class ListingStore {
        private OwnerListing consoleListing;
        private final Map<UUID, OwnerListing> listings = new HashMap<>();

        OwnerListing create(UUID playerId,
                List<ChunkTickets.ActiveOwner> owners, long nowNanos) {
            discardExpired(nowNanos);

            var listing = new OwnerListing(
                    UUID.randomUUID(), sortOwners(owners), nowNanos);
            listings.put(playerId, listing);
            return listing;
        }

        OwnerListing get(UUID playerId, long nowNanos) {
            discardExpired(nowNanos);
            return listings.get(playerId);
        }

        void remove(UUID playerId) {
            listings.remove(playerId);
        }

        void clear() {
            listings.clear();
            consoleListing = null;
        }

        private void discardExpired(long nowNanos) {
            listings.values().removeIf(listing -> listing.isExpired(nowNanos));
            if (consoleListing != null && consoleListing.isExpired(nowNanos)) {
                consoleListing = null;
            }
        }

        OwnerListing createConsole(
                List<ChunkTickets.ActiveOwner> owners, long nowNanos) {
            discardExpired(nowNanos);
            consoleListing = new OwnerListing(
                    UUID.randomUUID(), sortOwners(owners), nowNanos);
            return consoleListing;
        }

        OwnerListing getConsole(long nowNanos) {
            discardExpired(nowNanos);
            return consoleListing;
        }
    }

    static List<ChunkTickets.ActiveOwner> sortOwners(
            List<ChunkTickets.ActiveOwner> owners) {
        return owners.stream()
                .sorted(Comparator
                        .comparingInt((ChunkTickets.ActiveOwner owner) -> dimensionOrder(owner.dimension()))
                        .thenComparing(owner -> owner.dimension().identifier().toString())
                        .thenComparingInt(owner -> owner.pos().getX())
                        .thenComparingInt(owner -> owner.pos().getY())
                        .thenComparingInt(owner -> owner.pos().getZ()))
                .toList();
    }

    static ListingStore listingsFor(MinecraftServer server) {
        return LISTINGS.computeIfAbsent(server, ignored -> new ListingStore());
    }

    static void clearListings(MinecraftServer server) {
        ListingStore store = LISTINGS.remove(server);
        if (store != null) {
            store.clear();
        }
    }

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STOPPED.register(ModCommands::clearListings);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ListingStore store = LISTINGS.get(server);
            if (store != null) {
                store.remove(handler.player.getUUID());
            }
        });
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> {
                    dispatcher.register(Commands.literal("ankerstone")
                            .requires(source -> source.permissions()
                                    .hasPermission(Permissions.COMMANDS_OWNER))
                            .then(Commands.literal("list")
                                    .executes(context -> showList(context.getSource()))
                                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                            .executes(context -> showExistingListPage(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "page")))))
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

    private static int showList(CommandSourceStack source) {
        long now = System.nanoTime();
        var owners = ChunkTickets.getActiveOwners(source.getServer());

        OwnerListing listing;
        if (source.getEntity() instanceof ServerPlayer player) {
            listing = listingsFor(source.getServer())
                    .create(player.getUUID(), owners, now);
        } else {
            listing = listingsFor(source.getServer()).createConsole(owners, now);
        }

        return showListPage(source, listing, 1);
    }

    private static int showExistingListPage(
            CommandSourceStack source, int page) {
        ListingStore store = LISTINGS.get(source.getServer());
        OwnerListing listing = null;
        long now = System.nanoTime();

        if (store != null) {
            if (source.getEntity() instanceof ServerPlayer player) {
                listing = store.get(player.getUUID(), now);
            } else {
                listing = store.getConsole(now);
            }
        }

        if (listing == null) {
            source.sendFailure(Component.literal(
                    "No current listing. Run /ankerstone list to create one."));
            return 0;
        }

        return showListPage(source, listing, page);
    }

    private static int showListPage(
            CommandSourceStack source, OwnerListing listing, int page) {
        if (page < 1 || page > listing.pageCount()) {
            source.sendFailure(Component.literal(
                    "Page must be between 1 and " + listing.pageCount() + "."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Active Ankerstones: " + listing.owners().size()
                        + ". Page " + page + " of " + listing.pageCount() + "."),
                false);

        if (listing.owners().isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No active Ankerstones."), false);
            return 1;
        }

        int number = (page - 1) * LIST_PAGE_SIZE + 1;
        for (var owner : listing.pageEntries(page)) {
            var pos = owner.pos();
            String line = number++ + ". "
                    + owner.dimension().identifier() + " "
                    + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                    + " " + owner.mode().getSerializedName();

            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
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

    private static int dimensionOrder(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.OVERWORLD))
            return 0;
        if (dimension.equals(Level.NETHER))
            return 1;
        if (dimension.equals(Level.END))
            return 2;
        return 3;
    }
}