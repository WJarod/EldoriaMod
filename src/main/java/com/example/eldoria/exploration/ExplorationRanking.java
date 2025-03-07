package com.example.eldoria.exploration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.*;

public class ExplorationRanking {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final HashMap<UUID, Integer> playerBiomeCount = new HashMap<>();

    public static void updateBiomeCount(Player player) {
        UUID playerId = player.getUUID();
        playerBiomeCount.put(playerId, playerBiomeCount.getOrDefault(playerId, 0) + 1);

        LOGGER.info("📊 Mise à jour du classement : {} biomes explorés par {}", playerBiomeCount.get(playerId), player.getName().getString());
        ExplorationRewards.checkForRewards(player);
    }

    public static int getPlayerBiomeCount(Player player) {
        return playerBiomeCount.getOrDefault(player.getUUID(), 0);
    }

    // ✅ Méthode pour récupérer le classement
    public static List<Map.Entry<UUID, Integer>> getRanking() {
        List<Map.Entry<UUID, Integer>> ranking = new ArrayList<>(playerBiomeCount.entrySet());
        ranking.sort((a, b) -> b.getValue() - a.getValue()); // Tri décroissant
        return ranking;
    }

    // ✅ Méthode pour afficher le top 5 des explorateurs
    public static void displayRanking(ServerPlayer player) {
        List<Map.Entry<UUID, Integer>> ranking = getRanking();

        player.displayClientMessage(Component.literal("🏆 Classement des Explorateurs 🏆"), false);

        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : ranking.subList(0, Math.min(5, ranking.size()))) {
            String playerName = player.getServer().getPlayerList().getPlayer(entry.getKey()) != null ?
                    player.getServer().getPlayerList().getPlayer(entry.getKey()).getName().getString() : "Inconnu";
            player.displayClientMessage(Component.literal(rank + ". " + playerName + " - " + entry.getValue() + " biomes"), false);
            rank++;
        }

        if (ranking.isEmpty()) {
            player.displayClientMessage(Component.literal("Aucun joueur n'a encore exploré de biomes."), false);
        }
    }
}