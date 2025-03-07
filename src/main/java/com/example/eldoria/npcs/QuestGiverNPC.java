package com.example.eldoria.npcs;

import com.example.eldoria.EldoriaMod;
import com.example.eldoria.entities.ModEntities;
import com.example.eldoria.exploration.ExplorationRewards;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * PNJ qui donne des énigmes et des indices pour les trésors.
 */
public class QuestGiverNPC extends Villager {

    private static final Map<String, String[]> ENIGMES = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final Map<String, String> playerQuestions = new HashMap<>();
    private static final Map<String, Integer> playerAttempts = new HashMap<>();

    static {
        // 🔥 Zelda - Énigmes inspirées des temples
        ENIGMES.put("J’obéis au vent et je tourne sans fin, mais quand le vent cesse, je me fige. Qui suis-je ?",
                new String[]{"Moulin", "Je produis parfois de la musique ou de l’eau."});

        ENIGMES.put("Je suis né de la lumière, mais je vis dans l’ombre. On me chasse avec la clarté. Qui suis-je ?",
                new String[]{"Fantôme", "Je hante souvent des temples abandonnés."});

        ENIGMES.put("Je vis sous terre et garde les secrets enfouis. Qui suis-je ?",
                new String[]{"Statue", "Je veille silencieusement dans les ruines anciennes."});

        ENIGMES.put("J’aime l’eau mais je ne suis pas un poisson. Je suis souvent rond et parfois je flotte. Qui suis-je ?",
                new String[]{"Bulle", "On me trouve dans les grottes humides et les rivières souterraines."});

        ENIGMES.put("Je danse avec le feu et éclaire les ténèbres. Qui suis-je ?",
                new String[]{"Flamme", "Je brûle, mais je peux être soufflé."});

        ENIGMES.put("On me joue mais je ne suis pas un instrument. On me tourne pour changer la mélodie. Qui suis-je ?",
                new String[]{"Carillon", "On me trouve parfois dans les tours ou les maisons anciennes."});

        ENIGMES.put("Je suis léger comme l’air mais je peux t’emporter très haut. Qui suis-je ?",
                new String[]{"Courant ascendant", "Je peux apparaître près des volcans ou des temples célestes."});

        // 🏹 Autres jeux d’aventure (Skyrim, Dark Souls, Elden Ring)
        ENIGMES.put("On m’utilise pour voir dans l’ombre, mais je ne suis pas une torche. Qui suis-je ?",
                new String[]{"Lanterne", "On me porte souvent en bandoulière."});

        ENIGMES.put("Je suis une pierre, mais j’ai un œil. Qui suis-je ?",
                new String[]{"Pierre de sagesse", "On me place souvent sur des portes anciennes."});

        ENIGMES.put("Je protège mais je ne suis pas un mur. Parfois, je suis magique. Qui suis-je ?",
                new String[]{"Bouclier", "Certains me portent pour bloquer le feu ou la glace."});

        ENIGMES.put("On me cherche, mais une fois trouvé, on ne peut plus me voir. Qui suis-je ?",
                new String[]{"Secret", "Je peux être derrière un mur ou sous une cascade."});

        ENIGMES.put("Je m’ouvre avec une clé, mais je ne suis pas une porte. Qui suis-je ?",
                new String[]{"Coffre", "Je contiens parfois des trésors ou des pièges."});

        // 🌿 Mystères de la nature et de l’exploration
        ENIGMES.put("Je bois sans avoir soif et je grandis sans manger. Qui suis-je ?",
                new String[]{"Arbre", "Je peux vivre des siècles et cacher des secrets."});

        ENIGMES.put("Je peux être tranchante comme une épée, mais je ne suis pas un métal. Qui suis-je ?",
                new String[]{"Feuille", "Certains aventuriers me tissent pour faire des habits."});

        ENIGMES.put("Je chante avec le vent mais je n’ai pas de bouche. Qui suis-je ?",
                new String[]{"Flûte", "Les bardes m’aiment bien."});

        ENIGMES.put("Je vis la nuit et meurs au matin. Qui suis-je ?",
                new String[]{"Étoile filante", "On me voit souvent quand le ciel est dégagé."});

        ENIGMES.put("On me cherche dans les ruines et parfois sous la terre. Qui suis-je ?",
                new String[]{"Trésor", "Parfois, il faut une carte pour me trouver."});
    }

    public QuestGiverNPC(EntityType<? extends Villager> entityType, Level world) {
        super(entityType, world);
        this.setVillagerData(this.getVillagerData().setProfession(VillagerProfession.NONE)); // Pas de profession spécifique
    }


    // ✅ Ajout d'un planificateur global pour gérer les tâches différées sans bloquer
    private static final java.util.concurrent.ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 8.0F));
    }

    /**
     * Permet l'interaction avec le joueur lorsqu'il fait un clic droit sur le PNJ.
     */
    @Override
    public InteractionResult interactAt(net.minecraft.world.entity.player.Player player, Vec3 hitVec, InteractionHand hand) {
        if (!this.level().isClientSide) { // Vérifier que nous sommes côté serveur
            interactWithPlayer(player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Génère un PNJ aléatoirement dans le monde.
     */
    public static BlockPos spawnRandomly(ServerLevel world) {
        try {
            List<ServerPlayer> players = world.getPlayers(player -> true);
            if (players.isEmpty()) {
                EldoriaMod.LOGGER.warn("[DEBUG] Aucun joueur connecté, annulation du spawn.");
                return null;
            }

            ServerPlayer targetPlayer = players.get(world.getRandom().nextInt(players.size()));

            int attempts = 0;
            while (attempts < 10) { // 🔄 Essayer jusqu'à 10 fois
                attempts++;

                int offsetX = world.getRandom().nextInt(201) - 100;
                int offsetZ = world.getRandom().nextInt(201) - 100;
                int x = targetPlayer.blockPosition().getX() + offsetX;
                int z = targetPlayer.blockPosition().getZ() + offsetZ;
                int y = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
                BlockPos spawnPos = new BlockPos(x, y, z);

                if (y < world.getMinBuildHeight()) {
                    EldoriaMod.LOGGER.warn("[DEBUG] Tentative {} : Spawn annulé (trop bas, Y = {}).", attempts, y);
                    continue; // 🔁 Essayer une autre position
                }

                if (!world.getBlockState(spawnPos.below()).isSolid()) {
                    EldoriaMod.LOGGER.warn("[DEBUG] Tentative {} : Bloc non solide à {}, recalcul...", attempts, spawnPos);
                    continue; // 🔁 Essayer une autre position
                }

                if (world.getBlockState(spawnPos).getFluidState().isSource()) {
                    EldoriaMod.LOGGER.warn("[DEBUG] Tentative {} : Bloc dans l'eau à {}, recalcul...", attempts, spawnPos);
                    continue; // 🔁 Essayer une autre position
                }

                // ✅ Si toutes les vérifications passent, spawn du PNJ
                QuestGiverNPC npc = new QuestGiverNPC(ModEntities.QUEST_GIVER_NPC.get(), world);
                npc.moveTo(x + 0.5, y, z + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F);
                npc.setCustomName(Component.literal("Aventurier Mystérieux"));
                npc.setCustomNameVisible(true);
                world.addFreshEntity(npc);

                // ✅ Génération aléatoire du campement autour du PNJ
                placeCampfireAndChest(world, spawnPos);

                EldoriaMod.LOGGER.info("[DEBUG] PNJ spawné avec succès à X: {}, Y: {}, Z: {} autour de {}", x, y, z, targetPlayer.getName().getString());
                return spawnPos;
            }

            // 🚨 Si après 10 tentatives on ne trouve pas d'endroit, on abandonne
            EldoriaMod.LOGGER.error("❌ Impossible de spawn le PNJ après 10 tentatives !");
            return null;

        } catch (Exception e) {
            EldoriaMod.LOGGER.error("[ERREUR] Impossible de spawn le PNJ de quête !", e);
            return null;
        }
    }


    /**
     * Interaction avec le joueur pour lui poser une énigme.
     */
    public void interactWithPlayer(net.minecraft.world.entity.player.Player player) {
        String playerName = player.getName().getString();

        if (!playerQuestions.containsKey(playerName)) {
            String question = getRandomEnigme();
            playerQuestions.put(playerName, question);
            playerAttempts.put(playerName, 0);
            player.sendSystemMessage(Component.literal("📜 [Aventurier Mystérieux] : " + question));
        } else {
            player.sendSystemMessage(Component.literal("📜 [Aventurier Mystérieux] : Réponds-moi d'abord !"));
        }
    }

    public static boolean hasPendingQuestion(String playerName) {
        return playerQuestions.containsKey(playerName);
    }

    public static void removeQuestion(String playerName) {
        playerQuestions.remove(playerName);
        playerAttempts.remove(playerName);
    }

    /**
     * Vérification de la réponse du joueur.
     */
    public static void checkAnswer(String playerName, String response, ServerChatEvent event) {
        String receivedMessage = response.trim(); // Nettoyage de la réponse
        String cleanedMessage = receivedMessage.replaceAll("literal\\{(.*)}", "$1"); // Enlève "literal{}"
        String expectedAnswer = ENIGMES.getOrDefault(playerQuestions.get(playerName), new String[]{"", ""})[0];

        // ✅ Log après nettoyage
        EldoriaMod.LOGGER.info("[DEBUG] Réponse reçue de {} (nettoyée) : '{}'", playerName, cleanedMessage);
        EldoriaMod.LOGGER.info("[DEBUG] Réponse attendue : '{}'", expectedAnswer);

        if (!playerQuestions.containsKey(playerName)) {
            event.getPlayer().sendSystemMessage(Component.literal("📜 [Aventurier Mystérieux] : Je ne t’ai pas encore posé de question !"));
            return;
        }

        int attempts = playerAttempts.getOrDefault(playerName, 0);

        if (cleanedMessage.equalsIgnoreCase(expectedAnswer)) {
            event.getPlayer().sendSystemMessage(Component.literal("🎉 [Aventurier Mystérieux] : Bravo, aventurier ! Voici ton indice..."));
            BlockPos treasureCoords = ExplorationRewards.generateTreasure(event.getPlayer());

            // ✅ Suppression de la question et des tentatives après succès
            playerQuestions.remove(playerName);
            playerAttempts.remove(playerName);

            // ✅ Trouver le PNJ à proximité pour le faire disparaître après 30 secondes
            ServerLevel world = (ServerLevel) event.getPlayer().level();
            final QuestGiverNPC pnj = (QuestGiverNPC) world.getEntities(null, event.getPlayer().getBoundingBox().inflate(10))
                    .stream()
                    .filter(entity -> entity instanceof QuestGiverNPC)
                    .findFirst()
                    .orElse(null);

            if (pnj != null) {
                BlockPos campfirePos = pnj.blockPosition().below(); // Position du camp

                // ✅ Planifier la disparition du PNJ et du camp après 30 secondes SANS bloquer
                scheduler.schedule(() -> {
                    world.getServer().execute(() -> { // Exécuter la suppression sur le thread principal de Minecraft
                        if (pnj.isAlive()) {
                            pnj.discard(); // Supprimer le PNJ
                            world.setBlock(campfirePos, Blocks.AIR.defaultBlockState(), 3); // Supprimer le feu de camp
                            world.setBlock(campfirePos.offset(1, 0, 0), Blocks.AIR.defaultBlockState(), 3); // Supprimer le coffre
                            event.getPlayer().sendSystemMessage(Component.literal("🌫️ L'Aventurier Mystérieux a replié son camp et est parti explorer d'autres terres..."));
                            EldoriaMod.LOGGER.info("[DEBUG] L'Aventurier Mystérieux et son camp ont disparu.");
                        }
                    });
                }, 30, TimeUnit.SECONDS);
            }
        } else {
            attempts++;
            if (attempts == 2) {
                event.getPlayer().sendSystemMessage(Component.literal("💡 [Aventurier Mystérieux] : Indice : " + ENIGMES.get(playerQuestions.get(playerName))[1]));
            } else if (attempts >= 3) {
                event.getPlayer().sendSystemMessage(Component.literal("⏳ [Aventurier Mystérieux] : Tu as échoué... Reviens plus tard !"));
                playerQuestions.remove(playerName);
                playerAttempts.remove(playerName);
            } else {
                event.getPlayer().sendSystemMessage(Component.literal("❌ [Aventurier Mystérieux] : Ce n'est pas la bonne réponse ! Réessaye."));
            }
            playerAttempts.put(playerName, attempts);
        }
    }

    private String getRandomEnigme() {
        Object[] keys = ENIGMES.keySet().toArray();
        return (String) keys[RANDOM.nextInt(keys.length)];
    }

    /**
     * Place un feu de camp et un coffre dans un rayon de 4 blocs autour du PNJ.
     */
    private static void placeCampfireAndChest(ServerLevel world, BlockPos centerPos) {
        Random random = new Random();
        BlockPos campfirePos = null;
        BlockPos chestPos = null;

        int attempts = 0;
        while (attempts < 10) { // 🔄 Essayer jusqu'à 10 fois
            attempts++;

            int dx = random.nextInt(6) - 4; // Entre -4 et +4
            int dz = random.nextInt(6) - 4;
            BlockPos candidatePos = centerPos.offset(dx, 0, dz);
            int y = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, candidatePos.getX(), candidatePos.getZ());
            BlockPos surfacePos = new BlockPos(candidatePos.getX(), y, candidatePos.getZ());

            if (world.getBlockState(surfacePos.below()).isSolid()) { // ✅ Si le bloc est solide, on valide
                if (campfirePos == null) {
                    campfirePos = surfacePos;
                } else if (chestPos == null) {
                    chestPos = surfacePos;
                    break; // ✅ On arrête dès qu'on a trouvé le coffre
                }
            }

            EldoriaMod.LOGGER.warn("⚠️ [WARNING] Tentative de spawn sur un bloc non solide ! Recalcul...");
        }

        // ✅ Placer le feu de camp s'il a une position valide
        if (campfirePos != null) {
            world.setBlock(campfirePos, Blocks.CAMPFIRE.defaultBlockState(), 3);
            EldoriaMod.LOGGER.info("🔥 Feu de camp placé au camp à {}", campfirePos);
        }

        // ✅ Placer le coffre s'il a une position valide
        if (chestPos != null) {
            world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
            EldoriaMod.LOGGER.info("📦 Coffre placé au camp à {}", chestPos);

            // 🔧 Solution : copier chestPos dans une variable finale pour éviter les erreurs avec les lambdas
            final BlockPos finalChestPos = chestPos;

            // ✅ Ajouter un léger délai pour éviter le bug du coffre vide
            world.getServer().execute(() -> {
                ChestBlockEntity chest = (ChestBlockEntity) world.getBlockEntity(finalChestPos);
                if (chest == null) {
                    EldoriaMod.LOGGER.error("❌ Impossible de récupérer l'entité du coffre à {}, tentative de correction...", finalChestPos);

                    // ✅ Vérifier si le bloc est bien un coffre, sinon, on le replace
                    if (!world.getBlockState(finalChestPos).is(Blocks.CHEST)) {
                        world.setBlock(finalChestPos, Blocks.CHEST.defaultBlockState(), 3);
                        EldoriaMod.LOGGER.info("🔄 Coffre replacé à {}", finalChestPos);
                    }

                    // ✅ Nouvelle tentative après un léger délai
                    world.getServer().execute(() -> {
                        ChestBlockEntity retryChest = (ChestBlockEntity) world.getBlockEntity(finalChestPos);
                        if (retryChest != null) {
                            fillChestWithSupplies(retryChest);
                        } else {
                            EldoriaMod.LOGGER.error("❌ Échec de récupération du coffre après nouvelle tentative à {}", finalChestPos);
                        }
                    });
                } else {
                    fillChestWithSupplies(chest);
                }
            });
        } else {
            EldoriaMod.LOGGER.warn("⚠️ Impossible de trouver une position valide pour le coffre !");
        }
    }

    private static void fillChestWithSupplies(ChestBlockEntity chest) {
        List<ItemStack> possibleLoot = new ArrayList<>();

        // ✅ Ajout des aliments (avec un minimum de 10 unités par item)
        possibleLoot.add(new ItemStack(Items.COOKED_BEEF, 10 + new Random().nextInt(6)));
        possibleLoot.add(new ItemStack(Items.BREAD, 10 + new Random().nextInt(6)));
        possibleLoot.add(new ItemStack(Items.GOLDEN_CARROT, 10 + new Random().nextInt(6)));
        possibleLoot.add(new ItemStack(Items.COOKED_CHICKEN, 10 + new Random().nextInt(6)));
        possibleLoot.add(new ItemStack(Items.COOKED_SALMON, 10 + new Random().nextInt(6)));

        // ✅ Ajout d'objets utilitaires (ex : torches, pelle, pioche)
        possibleLoot.add(new ItemStack(Items.TORCH, 10 + new Random().nextInt(6)));
        possibleLoot.add(new ItemStack(Items.IRON_PICKAXE));
        possibleLoot.add(new ItemStack(Items.STONE_AXE));
        possibleLoot.add(new ItemStack(Items.LEATHER_BOOTS));

        // 🔥 CORRECTION : Convertir la liste en liste modifiable
        List<ItemStack> shuffledLoot = new ArrayList<>(possibleLoot);
        Collections.shuffle(shuffledLoot);

        // ✅ Ajouter les objets mélangés au coffre (max 6 items)
        for (int i = 0; i < Math.min(shuffledLoot.size(), 6); i++) {
            chest.setItem(i, shuffledLoot.get(i));
        }

        chest.setChanged(); // Assure la sauvegarde du coffre
        EldoriaMod.LOGGER.info("✅ Loot ajouté dans le coffre du camp !");
    }
}