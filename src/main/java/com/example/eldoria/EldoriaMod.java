package com.example.eldoria;

import com.example.eldoria.entities.ModEntities;
import com.example.eldoria.events.QuestNPCSpawner;
import com.example.eldoria.exploration.BiomeAlertProcedure;
import com.example.eldoria.exploration.ExplorationRanking;
import com.example.eldoria.exploration.ExplorationRewards;
import com.example.eldoria.commands.RankingCommand;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EldoriaMod.MODID)
public class EldoriaMod {
    public static final String MODID = "eldoria";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EldoriaMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        LOGGER.info("✅ Mod Eldoria chargé avec succès !");

        // Vérifier que les événements sont bien enregistrés
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this); // Enregistre les événements globaux

        ModEntities.register(FMLJavaModLoadingContext.get().getModEventBus());

        // Enregistrer les classes de procédures
        MinecraftForge.EVENT_BUS.register(new BiomeAlertProcedure());
        MinecraftForge.EVENT_BUS.register(new ExplorationRanking());
        MinecraftForge.EVENT_BUS.register(new ExplorationRewards());
        MinecraftForge.EVENT_BUS.register(new QuestNPCSpawner());
    }

    private void commonSetup(final net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("⚙️ Configuration du mod Eldoria...");
    }

    // Enregistrement des commandes
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RankingCommand.register(event.getDispatcher());
        LOGGER.info("📜 Commande /classement enregistrée !");
    }
}