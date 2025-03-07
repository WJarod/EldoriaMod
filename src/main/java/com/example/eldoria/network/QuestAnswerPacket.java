package com.example.eldoria.network;

import com.example.eldoria.EldoriaMod;
import com.example.eldoria.npcs.QuestGiverNPC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class QuestAnswerPacket {
    private final String answer;

    public QuestAnswerPacket(String answer) {
        this.answer = answer;
    }

    public static void encode(QuestAnswerPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.answer);
    }

    public static QuestAnswerPacket decode(FriendlyByteBuf buf) {
        return new QuestAnswerPacket(buf.readUtf());
    }

    public static void handle(QuestAnswerPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                EldoriaMod.LOGGER.info("[SERVER] Réponse reçue : '{}' de {}", packet.answer, player.getName().getString());

                // ✅ Appel de checkAnswer pour valider la réponse et déclencher les récompenses
                QuestGiverNPC.checkAnswer(player.getName().getString(), packet.answer, player);
            } else {
                EldoriaMod.LOGGER.error("❌ Impossible de récupérer le joueur dans handle !");
            }
        });
        ctx.get().setPacketHandled(true);
    }
}