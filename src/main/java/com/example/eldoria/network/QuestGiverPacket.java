package com.example.eldoria.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class QuestGiverPacket {
    private final String question;
    private final String[] answers;
    private final int correctAnswerIndex;

    public QuestGiverPacket(String question, String[] answers, int correctAnswerIndex) {
        this.question = question;
        this.answers = answers;
        this.correctAnswerIndex = correctAnswerIndex;
    }

    public static void encode(QuestGiverPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.question);
        buf.writeVarInt(packet.answers.length);
        for (String answer : packet.answers) {
            buf.writeUtf(answer);
        }
        buf.writeVarInt(packet.correctAnswerIndex);
    }

    public static QuestGiverPacket decode(FriendlyByteBuf buf) {
        String question = buf.readUtf();
        int length = buf.readVarInt();
        String[] answers = new String[length];
        for (int i = 0; i < length; i++) {
            answers[i] = buf.readUtf();
        }
        int correctAnswerIndex = buf.readVarInt();
        return new QuestGiverPacket(question, answers, correctAnswerIndex);
    }

    public static void handle(QuestGiverPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.example.eldoria.client.gui.QuestGiverScreen(
                            packet.question,
                            packet.answers,
                            packet.correctAnswerIndex
                    )
            );
        });
        ctx.get().setPacketHandled(true);
    }
}