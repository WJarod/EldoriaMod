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
    private final int correctAnswerIndex;
    private final String hint;
    private int attempts = 0;
    private Button hintButton;

    public QuestGiverScreen(String question, String[] answers, int correctAnswer, String hint) {
        super(Component.literal("Énigme de l'Aventurier Mystérieux"));
        this.question = question;
        this.answers = answers;
        this.correctAnswerIndex = correctAnswer;
        this.hint = hint;
    }

    @Override
    protected void init() {
        int startX = (this.width / 2) - 125;
        int startY = (this.height / 2) - 90;
        int buttonWidth = 250;
        int buttonHeight = 20;

        this.addRenderableWidget(Button.builder(Component.literal(question), (button) -> {})
                .bounds(startX, startY, buttonWidth, buttonHeight)
                .build()
        );

        for (int i = 0; i < answers.length; i++) {
            final int index = i;
            this.addRenderableWidget(Button.builder(Component.literal(answers[i]), (button) -> onAnswerSelected(index))
                    .bounds(startX, startY + (i * 25) + 30, buttonWidth, buttonHeight)
                    .build()
            );
        }

        hintButton = Button.builder(Component.literal("💡 Indice : ???"), (button) -> {})
                .bounds(startX, startY + 130, buttonWidth, buttonHeight)
                .build();
        hintButton.active = false;
        this.addRenderableWidget(hintButton);
    }

    private void onAnswerSelected(int selectedIndex) {
        if (selectedIndex == correctAnswerIndex) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("🎉 Bonne réponse !"), true);

            if (PacketHandler.CHANNEL != null) {
                Minecraft.getInstance().execute(() -> {
                    EldoriaMod.LOGGER.info("[CLIENT] Envoi de la réponse '{}' au serveur", answers[selectedIndex]);
                    PacketHandler.CHANNEL.sendToServer(new QuestAnswerPacket(answers[selectedIndex]));
                });
            }
            this.onClose();

        } else {
            attempts++;

            if (attempts == 1) {
                hintButton.setMessage(Component.literal("💡 Indice : " + hint));
                hintButton.active = true;
            } else {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("❌ Mauvaise réponse, réessaie !"), true);
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
    }
}