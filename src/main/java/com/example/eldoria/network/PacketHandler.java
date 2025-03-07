package com.example.eldoria.network;

import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraft.resources.ResourceLocation;
import com.example.eldoria.EldoriaMod;

import java.util.Optional;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EldoriaMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int nextID() {
        return packetId++;
    }

    // ✅ Méthode correcte pour enregistrer les messages
    public static void register() {
        CHANNEL.registerMessage(nextID(), QuestGiverPacket.class,
                QuestGiverPacket::encode, QuestGiverPacket::decode, QuestGiverPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(nextID(), QuestAnswerPacket.class,
                QuestAnswerPacket::encode, QuestAnswerPacket::decode, QuestAnswerPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }
}