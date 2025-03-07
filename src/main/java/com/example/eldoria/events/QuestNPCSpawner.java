package com.example.eldoria.events;

import com.example.eldoria.EldoriaMod;
import com.example.eldoria.npcs.QuestGiverNPC;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class QuestNPCSpawner {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;

            if (tickCounter >= 1200) { // ✅ 1 minute (1200 ticks)
                tickCounter = 0;

                if (event.getServer().getPlayerList().getPlayers().isEmpty()) {
                    EldoriaMod.LOGGER.warn("[DEBUG] Aucun joueur connecté, annulation du spawn.");
                    return;  // ✅ Ne rien faire si aucun joueur n'est connecté
                }

                boolean spawned = false;
                for (ServerLevel world : event.getServer().getAllLevels()) {
                    if (!spawned) {  // ✅ On ne spawn qu'un seul PNJ par cycle
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
            }
        }
    }
}