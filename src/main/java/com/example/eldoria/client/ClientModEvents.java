
package com.example.eldoria.client;

import com.example.eldoria.entities.ModEntities;
import com.example.eldoria.client.renderer.QuestGiverRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.example.eldoria.EldoriaMod;

@Mod.EventBusSubscriber(modid = EldoriaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.QUEST_GIVER_NPC.get(), QuestGiverRenderer::new);
    }
}
