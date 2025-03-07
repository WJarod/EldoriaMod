package com.example.eldoria.client.renderer;

import com.example.eldoria.EldoriaMod;
import com.example.eldoria.npcs.QuestGiverNPC;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class QuestGiverRenderer extends HumanoidMobRenderer<QuestGiverNPC, HumanoidModel<QuestGiverNPC>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("eldoria", "textures/entity/quest_giver.png");

    public QuestGiverRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        EldoriaMod.LOGGER.info("Chargement de la texture : " + TEXTURE);
    }

    @Override
    public ResourceLocation getTextureLocation(QuestGiverNPC entity) {
        return TEXTURE;
    }
}