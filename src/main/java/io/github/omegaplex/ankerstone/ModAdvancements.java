package io.github.omegaplex.ankerstone;

import java.util.UUID;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ModAdvancements {
    public static void onActivated(ServerLevel level, BlockPos pos) {
        AttributionData data = AttributionData.get(level);
        GlobalPos position = GlobalPos.of(level.dimension(), pos);
        UUID placer = data.getPlacer(position);

        if (placer == null) {
            return;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(placer);
        if (player == null) {
            data.queueCredit(placer);
            data.removePlacer(position);
            return;
        }

        AdvancementHolder advancement = level.getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath(
                        Ankerstone.MOD_ID, "stay_a_while"));
        if (advancement == null) {
            return;
        }

        player.getAdvancements().award(advancement, "activated");
        data.removePlacer(position);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        ServerLevel level = player.level();
        AttributionData data = AttributionData.get(level);
        UUID playerId = player.getUUID();

        if (!data.hasPendingCredit(playerId)) {
            return;
        }

        AdvancementHolder advancement = level.getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath(
                        Ankerstone.MOD_ID, "stay_a_while"));
        if (advancement == null) {
            return;
        }

        player.getAdvancements().award(advancement, "activated");
        data.removePendingCredit(playerId);
    }
}
