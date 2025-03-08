package com.example.eldoria.events;

import com.example.eldoria.EldoriaMod;
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
    private static String hint; // ✅ Ajout de l'indice en tant que variable statique
    private static int ticksUntilOpen = 5; // ✅ Ajout d'un léger délai pour éviter un conflit de ticks

    public static void triggerGui(String questionText, String[] possibleAnswers, int correct, String hintText) { // ✅ Ajout du paramètre hintText
        if (guiOpened) {
            return; // ✅ Empêche l'ouverture si elle est déjà en cours
        }

        question = questionText;
        answers = possibleAnswers;
        correctAnswer = correct;
        hint = hintText; // ✅ Stocke l'indice
        openGui = true;
        guiOpened = true; // ✅ Marque que la GUI est en attente d'ouverture

        // ✅ Ajoute un léger délai avant l'ouverture pour éviter un conflit de tick
        ticksUntilOpen = 5;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (openGui && ticksUntilOpen > 0) {
            ticksUntilOpen--; // ✅ Ajoute un léger délai avant ouverture pour éviter conflit de ticks
            return;
        }

        if (openGui && Minecraft.getInstance().screen == null) {
            EldoriaMod.LOGGER.info("[GUI] Ouverture de la fenêtre QuestGiverScreen.");
            Minecraft.getInstance().setScreen(new QuestGiverScreen(question, answers, correctAnswer, hint)); // ✅ Maintenant, hint est bien défini
            openGui = false;
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        openGui = false;
        guiOpened = false; // ✅ Réinitialise tout à la déconnexion
    }
}