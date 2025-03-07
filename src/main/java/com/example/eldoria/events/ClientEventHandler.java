package com.example.eldoria.events;

import com.example.eldoria.client.gui.QuestGiverScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {

    private static boolean openGui = false;
    private static boolean guiOpened = false; // ✅ Empêche la réouverture multiple
    private static String question;
    private static String[] answers;
    private static int correctAnswer;
    private static int ticksUntilOpen = 5; // ✅ Ajout d'un léger délai pour éviter un conflit de ticks

    public static void triggerGui(String questionText, String[] possibleAnswers, int correct) {
        if (guiOpened) {
            return; // ✅ Empêche l'ouverture si elle est déjà en cours
        }

        question = questionText;
        answers = possibleAnswers;
        correctAnswer = correct;
        openGui = true;
        guiOpened = true; // ✅ Marque que la GUI est en attente d'ouverture

        // ✅ Ajoute un léger délai avant l'ouverture pour éviter un conflit de tick
        ticksUntilOpen = 5;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (openGui && ticksUntilOpen > 0) {
            ticksUntilOpen--; // ✅ Réduction du compteur de ticks
            return; // Attente du bon moment avant ouverture
        }

        if (openGui && Minecraft.getInstance().screen == null) {
            Minecraft.getInstance().setScreen(new QuestGiverScreen(question, answers, correctAnswer));
            openGui = false;
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        openGui = false;
        guiOpened = false; // ✅ Réinitialise tout à la déconnexion
    }
}