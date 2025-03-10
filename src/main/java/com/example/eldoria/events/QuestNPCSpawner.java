package com.example.eldoria.events;

import com.example.eldoria.EldoriaMod;
import com.example.eldoria.npcs.QuestGiverNPC;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber
public class QuestNPCSpawner {

    private static final int SPAWN_INTERVAL = 6000; // ✅ 5 minutes (6000 ticks)
    private static final double SPAWN_CHANCE = 0.20; // ✅ 20% de chance
    private static final Random random = new Random();
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;

            if (tickCounter >= SPAWN_INTERVAL) { // ✅ Vérifie si 5 minutes sont écoulées
                tickCounter = 0; // ✅ Réinitialisation du compteur

                if (event.getServer().getPlayerList().getPlayers().isEmpty()) {
                    EldoriaMod.LOGGER.warn("[DEBUG] Aucun joueur connecté, annulation du spawn.");
                    return;  // ✅ Annule si aucun joueur n'est en ligne
                }

                if (random.nextDouble() <= SPAWN_CHANCE) { // ✅ 20% de chance d’exécuter le spawn
                    boolean spawned = false;
                    for (ServerLevel world : event.getServer().getAllLevels()) {
                        if (!spawned) {  // ✅ Un seul PNJ par cycle
                            EldoriaMod.LOGGER.info("[DEBUG] Tentative de spawn automatique du PNJ...");
                            BlockPos pos = QuestGiverNPC.spawnRandomly(world);
                            if (pos != null) {
                                EldoriaMod.LOGGER.info("[DEBUG] PNJ spawné avec succès à X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ());
                                spawned = true; // ✅ Empêche d'autres spawns dans cette boucle
                            } else {
                                EldoriaMod.LOGGER.warn("[DEBUG] Échec du spawn du PNJ.");
                            }
                        }
                    }
                } else {
                    EldoriaMod.LOGGER.info("[DEBUG] Aucune apparition de PNJ cette fois-ci (20% de chance).");
                }
            }
        }
    }
}