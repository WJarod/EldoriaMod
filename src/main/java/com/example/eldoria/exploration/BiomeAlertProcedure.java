package com.example.eldoria.exploration;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class BiomeAlertProcedure {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Stocker les biomes découverts pour chaque joueur
    private static final Map<UUID, HashSet<String>> discoveredBiomes = new HashMap<>();

    // Stocker le dernier biome pour éviter le spam
    private static final Map<UUID, String> lastBiomeMap = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Player player = event.player;
            Level world = player.level();

            if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
                MinecraftServer server = serverWorld.getServer();

                // Récupérer le biome actuel du joueur
                Biome biome = serverWorld.getNoiseBiome(
                        player.getBlockX() >> 2,
                        player.getBlockY() >> 2,
                        player.getBlockZ() >> 2
                ).value();

                ResourceKey<Biome> biomeKey = server.registryAccess()
                        .registryOrThrow(Registries.BIOME)
                        .getResourceKey(biome)
                        .orElse(null);

                // Formater le nom du biome
                String biomeName = (biomeKey != null) ? formatBiomeName(biomeKey.location().getPath()) : "Unknown Biome";

                UUID playerId = player.getUUID();
                String lastBiome = lastBiomeMap.getOrDefault(playerId, "");

                // Vérifier si le joueur a changé de biome
                if (!biomeName.equals(lastBiome)) {
                    lastBiomeMap.put(playerId, biomeName); // Mettre à jour le dernier biome

                    HashSet<String> playerBiomes = discoveredBiomes.computeIfAbsent(playerId, k -> new HashSet<>());

                    if (playerBiomes.add(biomeName)) {
                        // ✅ Nouveau biome découvert
                        LOGGER.info("🌍 Nouveau biome découvert : {}", biomeName);

                        // ✅ Affichage **au-dessus de la barre d'items** ✅
                        ((ServerPlayer) player).displayClientMessage(Component.literal("🌍 Nouveau biome découvert : " + biomeName), true);

                        ExplorationRanking.updateBiomeCount(player);
                    } else {
                        // ✅ Biome déjà visité → Juste afficher le nom
                        ((ServerPlayer) player).displayClientMessage(Component.literal("🌍 " + biomeName), true);
                    }
                }
            }
        }
    }

    /**
     * Convertit "minecraft:dark_forest" en "Dark Forest"
     */
    private static String formatBiomeName(String biomeId) {
        String[] words = biomeId.split("_");
        StringBuilder formattedName = new StringBuilder();
        for (String word : words) {
            formattedName.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }
        return formattedName.toString().trim();
    }
}