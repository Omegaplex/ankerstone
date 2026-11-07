package io.github.omegaplex.ankerstone;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.ChunkPos;
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
                            .then(Commands.literal("visit")
                                    .then(Commands.argument("listing", UuidArgument.uuid())
                                            .then(Commands.argument("number", IntegerArgumentType.integer(1))
                                                    .executes(context -> visit(
                                                            context.getSource(),
                                                            UuidArgument.getUuid(context, "listing"),
                                                            IntegerArgumentType.getInteger(context, "number"))))))
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

    static ChunkTickets.ActiveOwner findVisitTarget(
            CommandSourceStack source, UUID listingId, int number) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can visit Ankerstones."));
            return null;
        }
        if (!player.isSpectator()) {
            source.sendFailure(Component.literal(
                    "Switch to Spectator mode to visit this Ankerstone."));
            return null;
        }

        var server = source.getServer();
        ListingStore store = LISTINGS.get(server);
        OwnerListing listing = store == null
                ? null
                : store.get(player.getUUID(), System.nanoTime());

        if (listing == null || !listing.id().equals(listingId)) {
            source.sendFailure(Component.literal(
                    "This listing is no longer current. Run /ankerstone list again."));
            return null;
        }
        if (number < 1 || number > listing.owners().size()) {
            source.sendFailure(Component.literal("No Ankerstone at entry " + number + "."));
            return null;
        }

        var owner = listing.owners().get(number - 1);
        var level = server.getLevel(owner.dimension());
        if (level == null || !ChunkTickets.hasActiveOwner(level, owner.pos())) {
            source.sendFailure(Component.literal(
                    "That position no longer has a registered active Ankerstone."));
            return null;
        }
        return owner;
    }

    private static int visit(
            CommandSourceStack source, UUID listingId, int number) {
        var owner = findVisitTarget(source, listingId, number);
        if (owner == null) {
            return 0;
        }

        var level = source.getServer().getLevel(owner.dimension());
        var pos = owner.pos();
        var chunkPos = ChunkPos.containing(pos);
        var chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
        if (chunk == null) {
            source.sendFailure(Component.literal(
                    "The Ankerstone's chunk is not ready. Try again shortly."));
            return 0;
        }

        var state = chunk.getBlockState(pos);
        if (!state.is(ModBlocks.ANKERSTONE)
                || state.getValue(AnkerstoneBlock.MODE) == AnkerstoneMode.OFF) {
            source.sendFailure(Component.literal(
                    "There is no active Ankerstone at that position."));
            return 0;
        }

        var player = (ServerPlayer) source.getEntity();
        if (!player.teleportTo(level, pos.getX() + 0.5, pos.getY() + 1.5,
                pos.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), true)) {
            source.sendFailure(Component.literal("Could not visit that Ankerstone."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Visiting Ankerstone entry " + number + "."), false);
        return 1;
    }

    private static int showListPage(
            CommandSourceStack source, OwnerListing listing, int page) {
        if (page < 1 || page > listing.pageCount()) {
            source.sendFailure(Component.literal(
                    "Page must be between 1 and " + listing.pageCount() + "."));
            return 0;
        }

        boolean playerSource = source.getEntity() instanceof ServerPlayer;
        var title = Component.literal("Active Ankerstones");
        var details = Component.literal(": " + listing.owners().size()
                + " | Page " + page + "/" + listing.pageCount());
        if (playerSource) {
            title.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            details.withStyle(ChatFormatting.GRAY);
        }
        source.sendSuccess(() -> Component.empty().append(title).append(details), false);

        if (listing.owners().isEmpty()) {
            source.sendSuccess(() -> Component.literal("No active Ankerstones."), false);
            return 1;
        }

        if (playerSource) {
            var divider = Component.literal("------------------------")
                    .withStyle(ChatFormatting.DARK_GRAY);
            source.sendSuccess(() -> divider, false);
            source.sendSuccess(() -> Component.literal(
                    "Switch to Spectator mode, then click an entry to visit.")
                    .withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(() -> divider, false);
        }

        int number = (page - 1) * LIST_PAGE_SIZE + 1;
        for (var owner : listing.pageEntries(page)) {
            int entryNumber = number++;
            var pos = owner.pos();
            var label = Component.literal(entryNumber + ". ");
            var dimension = Component.literal(owner.dimension().identifier() + " ");
            var coords = Component.literal(
                    pos.getX() + " " + pos.getY() + " " + pos.getZ());
            var mode = Component.literal(" [" + owner.mode().getSerializedName() + "]");
            var entry = Component.empty()
                    .append(label).append(dimension).append(coords).append(mode);

            if (playerSource) {
                label.withStyle(ChatFormatting.GOLD);
                dimension.withStyle(ChatFormatting.GRAY);
                mode.withStyle(owner.mode() == AnkerstoneMode.WIDE
                        ? ChatFormatting.AQUA
                        : ChatFormatting.GREEN);
                String command = "/ankerstone visit " + listing.id() + " " + entryNumber;
                entry.withStyle(style -> style
                        .withClickEvent(new ClickEvent.RunCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("Visit in Spectator mode"))));
            }
            source.sendSuccess(() -> entry, false);
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