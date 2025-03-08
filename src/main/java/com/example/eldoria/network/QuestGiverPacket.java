package com.example.eldoria.network;

import com.example.eldoria.client.gui.QuestGiverScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class QuestGiverPacket {
    private final String question;
    private final String[] answers;
    private final int correctAnswerIndex;
    private final String hint; // ✅ Ajout de l'indice

    public QuestGiverPacket(String question, String[] answers, int correctAnswerIndex, String hint) {
        this.question = question;
        this.answers = answers;
        this.correctAnswerIndex = correctAnswerIndex;
        this.hint = hint; // ✅ Stocke l'indice
    }

    public static void encode(QuestGiverPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.question);
        buffer.writeVarInt(packet.answers.length);
        for (String answer : packet.answers) {
            buffer.writeUtf(answer);
        }
        buffer.writeVarInt(packet.correctAnswerIndex);
        buffer.writeUtf(packet.hint); // ✅ Encode aussi l'indice
    }

    public static QuestGiverPacket decode(FriendlyByteBuf buffer) {
        String question = buffer.readUtf();
        int answerCount = buffer.readVarInt();
        String[] answers = new String[answerCount];

        for (int i = 0; i < answerCount; i++) {
            answers[i] = buffer.readUtf();
        }

        int correctAnswerIndex = buffer.readVarInt();
        String hint = buffer.readUtf(); // ✅ Récupère l'indice

        return new QuestGiverPacket(question, answers, correctAnswerIndex, hint);
    }

    public static void handle(QuestGiverPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new QuestGiverScreen(
                    packet.question, packet.answers, packet.correctAnswerIndex, packet.hint // ✅ Passe aussi l'indice
            ));
        });
        ctx.get().setPacketHandled(true);
    }
}