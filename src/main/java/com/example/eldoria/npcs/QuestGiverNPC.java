package com.example.eldoria.npcs;

import com.example.eldoria.EldoriaMod;
import com.example.eldoria.entities.ModEntities;
import com.example.eldoria.events.ClientEventHandler;
import com.example.eldoria.exploration.ExplorationRewards;
import com.example.eldoria.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import com.example.eldoria.network.QuestGiverPacket;
import net.minecraftforge.network.NetworkDirection;

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
    private BlockPos campfirePos;
    private BlockPos chestPos;

    static {
        // ⚔️ Énigmes avec plusieurs réponses possibles

        ENIGMES.put("Je protège mais je ne suis pas un mur. Qui suis-je ?",
                new String[]{"Bouclier", "Épée", "Casque", "Plastron", "On me porte pour bloquer les attaques."});

        ENIGMES.put("Je m'ouvre avec une clé, mais je ne suis pas une porte. Qui suis-je ?",
                new String[]{"Coffre", "Livre", "Porte", "Sac", "Je peux contenir des trésors ou des pièges."});

        ENIGMES.put("Je suis une arme tranchante mais je ne coupe pas le papier. Qui suis-je ?",
                new String[]{"Épée", "Dague", "Hache", "Flèche", "On me forge pour les combats."});

        ENIGMES.put("J’éclaire la nuit mais je ne suis pas une étoile. Qui suis-je ?",
                new String[]{"Torche", "Flamme", "Lanterne", "Lampe", "On me porte souvent dans les grottes."});

        ENIGMES.put("Je danse avec le feu et éclaire les ténèbres. Qui suis-je ?",
                new String[]{"Flamme", "Lumière", "Torche", "Éclair", "Je brûle mais je peux être soufflée."});

        ENIGMES.put("Je bois sans avoir soif et je grandis sans manger. Qui suis-je ?",
                new String[]{"Arbre", "Plante", "Fleur", "Buisson", "Je peux vivre des siècles et cacher des secrets."});

        ENIGMES.put("Je tombe sans jamais me faire mal. Qui suis-je ?",
                new String[]{"Pluie", "Neige", "Feuille", "Cendre", "On me voit souvent dans le ciel gris."});

        ENIGMES.put("Je suis visible le jour et disparais la nuit. Qui suis-je ?",
                new String[]{"Ombre", "Fantôme", "Mirage", "Reflet", "Je suis attachée à toi mais tu ne peux pas me toucher."});

        ENIGMES.put("On me cherche mais une fois trouvé, on ne peut plus me voir. Qui suis-je ?",
                new String[]{"Secret", "Étoile", "Souvenir", "Mirage", "Je peux être caché derrière un mur ou sous une cascade."});

        ENIGMES.put("On me joue mais je ne suis pas un instrument. On me tourne pour changer la mélodie. Qui suis-je ?",
                new String[]{"Carillon", "Flûte", "Cloche", "Harmonica", "On me trouve parfois dans les tours ou les maisons anciennes."});

        ENIGMES.put("J'ai un dos mais pas de corps. Qui suis-je ?",
                new String[]{"Livre", "Sac", "Table", "Boîte", "Je contiens des histoires et des connaissances."});
    }

    public QuestGiverNPC(EntityType<? extends Villager> entityType, Level world) {
        super(entityType, world);
        this.setVillagerData(this.getVillagerData().setProfession(VillagerProfession.NONE));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    // ✅ Ajout d'un planificateur global pour gérer les tâches différées sans bloquer
    private static final java.util.concurrent.ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    /**
     * Interaction avec le PNJ pour poser une énigme au joueur.
     */
    @Override
    public InteractionResult interactAt(Player player, Vec3 hitVec, InteractionHand hand) {
        if (!this.level().isClientSide) {
            String question = getRandomEnigme();
            String[] possibleAnswers = ENIGMES.get(question); // Récupérer les réponses possibles
            String correctAnswer = possibleAnswers[0]; // La première réponse est toujours la bonne avant mélange

            // Mélanger les réponses pour éviter que la bonne soit toujours en premier
            List<String> shuffledAnswers = new ArrayList<>(Arrays.asList(possibleAnswers));
            Collections.shuffle(shuffledAnswers);

            // Trouver le nouvel index de la bonne réponse après mélange
            int correctAnswerIndex = shuffledAnswers.indexOf(correctAnswer);

            // ✅ Stocke la question pour éviter l'erreur "Je ne t’ai pas encore posé de question !"
            String playerName = player.getName().getString();
            playerQuestions.put(playerName, question);

            EldoriaMod.LOGGER.info("[QUEST GIVER] {} reçoit la question '{}'", playerName, question);
            EldoriaMod.LOGGER.info("[QUEST GIVER] Contenu de playerQuestions après ajout : {}", playerQuestions);

            // ✅ Ouvre la GUI côté client en envoyant un packet au lieu d'appeler directement la GUI
            if (player instanceof ServerPlayer serverPlayer) {
                PacketHandler.CHANNEL.sendTo(
                        new QuestGiverPacket(question, shuffledAnswers.toArray(new String[0]), correctAnswerIndex),
                        serverPlayer.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT
                );
            }

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
                npc.placeCampfireAndChest(world, spawnPos);

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

    public static boolean hasPendingQuestion(String playerName) {
        return playerQuestions.containsKey(playerName);
    }

    public static void removeQuestion(String playerName) {
        playerQuestions.remove(playerName);
        playerAttempts.remove(playerName);
    }

    /**
     * Vérification de la réponse du joueur v1.
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

        String correctAnswer = ENIGMES.getOrDefault(playerQuestions.get(playerName), new String[]{"", ""})[0];

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
                    world.getServer().execute(() -> {
                        try {
                            Thread.sleep(30000); // ⏳ Attente de 30 secondes
                            world.getServer().execute(() -> {
                                if (pnj.isAlive()) {
                                    pnj.discard(); // ✅ Supprimer le PNJ
                                    if (pnj.campfirePos != null) {
                                        world.setBlock(pnj.campfirePos, Blocks.AIR.defaultBlockState(), 3);
                                        EldoriaMod.LOGGER.info("🔥 Feu de camp retiré à {}", pnj.campfirePos);
                                    }
                                    if (pnj.chestPos != null) {
                                        world.setBlock(pnj.chestPos, Blocks.AIR.defaultBlockState(), 3);
                                        EldoriaMod.LOGGER.info("📦 Coffre retiré à {}", pnj.chestPos);
                                    }
                                    event.getPlayer().sendSystemMessage(Component.literal("🌫️ L'Aventurier Mystérieux a replié son camp et est parti explorer d'autres terres..."));
                                    EldoriaMod.LOGGER.info("[DEBUG] L'Aventurier Mystérieux et son camp ont disparu.");
                                }
                            });
                        } catch (InterruptedException e) {
                            EldoriaMod.LOGGER.error("❌ Erreur dans le timer de disparition du PNJ", e);
                            Thread.currentThread().interrupt();
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
    }*/

    /**
     * Vérifie la réponse du joueur et affiche le résultat dans l'interface sans utiliser le chat.
     */
    public static void checkAnswer(String playerName, String response, ServerPlayer player) {
        EldoriaMod.LOGGER.info("[CHECK ANSWER] Contenu de playerQuestions : {}", playerQuestions);
        EldoriaMod.LOGGER.info("[CHECK ANSWER] Le joueur {} tente de répondre à une question.", playerName);

        if (!playerQuestions.containsKey(playerName)) {
            player.displayClientMessage(Component.literal("📜 [Aventurier Mystérieux] : Je ne t’ai pas encore posé de question !"), true);
            EldoriaMod.LOGGER.error("[CHECK ANSWER] ERREUR: Aucune question trouvée pour {}", playerName);
            return;
        }

        String question = playerQuestions.get(playerName);
        String expectedAnswer = ENIGMES.getOrDefault(question, new String[]{"", ""})[0];

        EldoriaMod.LOGGER.info("[DEBUG] Réponse reçue de {} : '{}'", playerName, response);
        EldoriaMod.LOGGER.info("[DEBUG] Réponse attendue : '{}'", expectedAnswer);

        int attempts = playerAttempts.getOrDefault(playerName, 0);

        if (response.equalsIgnoreCase(expectedAnswer)) {
            // ✅ Affichage de l'animation UI avant toute autre action
            player.displayClientMessage(Component.literal("🎉 [Aventurier Mystérieux] : Bravo, aventurier ! Voici ton indice..."), true);

            EldoriaMod.LOGGER.info("[CHECK ANSWER] Appel de generateTreasure pour {}", player.getName().getString());

            player.getServer().execute(() -> {
                BlockPos treasureCoords = ExplorationRewards.generateTreasure(player);
                if (treasureCoords == null) {
                    EldoriaMod.LOGGER.error("[CHECK ANSWER] Échec : generateTreasure a retourné null !");
                } else {
                    EldoriaMod.LOGGER.info("[CHECK ANSWER] Trésor généré avec succès à {}", treasureCoords);
                    // ❌ Ne pas appeler `giveExplorerBook()` ici car il est déjà dans `generateTreasure()`
                }
            });

            // ✅ Nettoyage de la question après validation
            playerQuestions.remove(playerName);
            playerAttempts.remove(playerName);
        } else {
            attempts++;

            if (attempts == 2) {
                player.displayClientMessage(Component.literal("💡 Indice : " + ENIGMES.get(question)[1]), true);
            } else if (attempts >= 3) {
                player.displayClientMessage(Component.literal("⏳ [Aventurier Mystérieux] : Tu as échoué... Reviens plus tard !"), true);
                playerQuestions.remove(playerName);
                playerAttempts.remove(playerName);
            } else {
                player.displayClientMessage(Component.literal("❌ [Aventurier Mystérieux] : Ce n'est pas la bonne réponse ! Réessaye."), true);
            }

            playerAttempts.put(playerName, attempts);
        }
    }

    /**
     * Sélectionne une énigme aléatoire.
     */
    private String getRandomEnigme() {
        Object[] keys = ENIGMES.keySet().toArray();
        return (String) keys[RANDOM.nextInt(keys.length)];
    }

    /**
     * Place un feu de camp et un coffre dans un rayon de 4 blocs autour du PNJ.
     */
    private void placeCampfireAndChest(ServerLevel world, BlockPos centerPos) {
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
            this.campfirePos = campfirePos;  // ✅ Sauvegarde dans l'instance
            EldoriaMod.LOGGER.info("🔥 Feu de camp placé au camp à {}", campfirePos);
        }

        // ✅ Placer le coffre s'il a une position valide
        if (chestPos != null) {
            world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
            this.chestPos = chestPos;
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