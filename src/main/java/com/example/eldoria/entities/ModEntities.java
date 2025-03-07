package com.example.eldoria.entities;

import com.example.eldoria.EldoriaMod;
import com.example.eldoria.npcs.QuestGiverNPC;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = EldoriaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EldoriaMod.MODID);

    public static final RegistryObject<EntityType<QuestGiverNPC>> QUEST_GIVER_NPC = ENTITIES.register(
            "quest_giver_npc",
            () -> EntityType.Builder.of(QuestGiverNPC::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F) // Taille du PNJ
                    .clientTrackingRange(10)
                    .build("quest_giver_npc")
    );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
        // Suppression de eventBus.addListener(ModEntities::registerAttributes);
        // Car l'événement est déjà géré par @SubscribeEvent ci-dessous
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(QUEST_GIVER_NPC.get(), QuestGiverNPC.createAttributes().build());
    }
}