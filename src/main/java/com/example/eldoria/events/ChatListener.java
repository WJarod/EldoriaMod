package com.example.eldoria.events;

import com.example.eldoria.npcs.QuestGiverNPC;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ChatListener {

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        String playerName = event.getPlayer().getName().getString();
        String message = String.valueOf(event.getMessage());

        // Vérifie si le joueur a une question en attente
        if (QuestGiverNPC.hasPendingQuestion(playerName)) {
            QuestGiverNPC.checkAnswer(playerName, message, event);
            event.setCanceled(true); // Empêche l'affichage du message dans le chat normal
        }
    }
}