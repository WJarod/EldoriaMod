package com.example.eldoria.client.gui;

import com.example.eldoria.EldoriaMod;
import com.example.eldoria.network.PacketHandler;
import com.example.eldoria.network.QuestAnswerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class QuestGiverScreen extends Screen {
    private final String question;
    private final String[] answers;
    private final int correctAnswerIndex; // ✅ Stocke l'index de la bonne réponse

    public QuestGiverScreen(String question, String[] answers, int correctAnswer) {
        super(Component.literal("Énigme de l'Aventurier Mystérieux"));
        this.question = question;
        this.answers = answers;
        this.correctAnswerIndex = correctAnswer; // ✅ Correction de l'initialisation
    }

    @Override
    protected void init() {
        int startX = (this.width / 2) - 75;
        int startY = (this.height / 2) - 50;
        int buttonWidth = 150;
        int buttonHeight = 20;

        // ✅ Affichage de la question sous forme de texte statique
        this.addRenderableWidget(
                Button.builder(Component.literal(question), (button) -> {})
                        .bounds(startX, startY - 30, buttonWidth, buttonHeight)
                        .build()
        );

        for (int i = 0; i < answers.length; i++) {
            final int index = i; // ✅ Stocke l'index pour éviter les problèmes de lambda
            final String answer = answers[i];

            // ✅ Utilisation de Button.builder() pour la compatibilité avec 1.20.1
            this.addRenderableWidget(
                    Button.builder(Component.literal(answer), (button) -> {
                        onAnswerSelected(index); // 🎯 Vérifie la réponse sélectionnée
                        this.onClose();
                    }).bounds(startX, startY + (i * 25), buttonWidth, buttonHeight).build()
            );
        }
    }

    // 🎯 Vérification de la réponse quand le joueur clique sur une option
    private void onAnswerSelected(int selectedIndex) {
        if (selectedIndex == correctAnswerIndex) {
            // ✅ Bonne réponse
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("🎉 Bonne réponse !"));

            // ✅ Envoie la réponse correcte au serveur
            if (PacketHandler.CHANNEL != null) {
                Minecraft.getInstance().execute(() -> {
                    EldoriaMod.LOGGER.info("[CLIENT] Envoi de la réponse '{}' au serveur", answers[selectedIndex]);
                    PacketHandler.CHANNEL.sendToServer(new QuestAnswerPacket(answers[selectedIndex]));
                });
            } else {
                EldoriaMod.LOGGER.error("❌ Erreur : Le canal réseau n'est pas encore initialisé !");
            }
        } else {
            // ❌ Mauvaise réponse
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("❌ Mauvaise réponse, réessaie !"));
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
    }
}