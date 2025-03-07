package com.example.eldoria.exploration;

import com.example.eldoria.EldoriaMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;

public class ExplorationRewards {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random random = new Random();

    public static void checkForRewards(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("❌ Impossible de générer un trésor : le joueur {} n'est pas un ServerPlayer !", player.getName().getString());
            return; // 🚨 On arrête l'exécution si ce n'est pas un joueur serveur
        }

        int biomesExplored = ExplorationRanking.getPlayerBiomeCount(player);
        LOGGER.info("🎖 Vérification des récompenses... {} biomes explorés.", biomesExplored);

        // ✅ Ajout d'un log pour vérifier si la condition est remplie
        if (biomesExplored % 5 == 0 && biomesExplored > 0) {
            LOGGER.info("🎁 Génération d'un trésor car {} biomes ont été explorés par {}", biomesExplored, player.getName().getString());
            generateTreasure(serverPlayer);
        } else {
            LOGGER.info("❌ Condition non remplie : pas de trésor généré. (Biomes explorés : {})", biomesExplored);
        }
    }

    public static BlockPos generateTreasure(ServerPlayer player) {
        ServerLevel world = player.serverLevel();
        BlockPos playerPos = player.blockPosition();
        int attempts = 0;

        EldoriaMod.LOGGER.info("[DEBUG] Lancement de generateTreasure pour {}", player.getName().getString());

        while (attempts < 10) { // 🔄 Essayer jusqu'à 10 fois
            attempts++;

            int distance = 250 + random.nextInt(251);
            double angle = random.nextDouble() * 2 * Math.PI;
            int x = playerPos.getX() + (int) (distance * Math.cos(angle));
            int z = playerPos.getZ() + (int) (distance * Math.sin(angle));
            int y = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);

            BlockPos surfacePos = new BlockPos(x, y, z);

            // ✅ Vérification améliorée : solide, non liquide, pas de feuillage, pas de vigne
            if (world.getBlockState(surfacePos.below()).isSolid() &&
                    !world.getBlockState(surfacePos.below()).getFluidState().isSource() &&
                    !isInvalidBlock(world, surfacePos.below())) {

                world.setBlock(surfacePos, Blocks.CHEST.defaultBlockState(), 3);
                EldoriaMod.LOGGER.info("📜 Trésor généré aux coordonnées : X={}, Y={}, Z={}", x, y, z);

                world.getServer().execute(() -> {
                    boolean lootAdded = addLootToChest(world, surfacePos);
                    if (!lootAdded) {
                        EldoriaMod.LOGGER.error("❌ [ERREUR] Impossible de remplir le coffre au trésor !");
                    } else {
                        EldoriaMod.LOGGER.info("✅ Loot ajouté avec succès dans le coffre !");
                        giveExplorerBook(player, surfacePos);
                    }
                });

                return surfacePos;
            }

            EldoriaMod.LOGGER.warn("⚠️ [WARNING] Tentative {} : Spawn sur un bloc non valide. Recalcul...", attempts);
        }

        // 🚨 Dernier recours : Spawn proche du joueur si impossible de générer ailleurs
        EldoriaMod.LOGGER.error("❌ Impossible de générer un trésor après 10 tentatives ! Tentative de spawn à proximité du joueur.");

        for (int i = 0; i < 5; i++) { // Essayer jusqu'à 5 fois autour du joueur
            int offsetX = -5 + random.nextInt(11);
            int offsetZ = -5 + random.nextInt(11);
            int y = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, playerPos.getX() + offsetX, playerPos.getZ() + offsetZ);
            BlockPos closePos = new BlockPos(playerPos.getX() + offsetX, y, playerPos.getZ() + offsetZ);

            if (world.getBlockState(closePos.below()).isSolid() &&
                    !world.getBlockState(closePos.below()).getFluidState().isSource() &&
                    !isInvalidBlock(world, closePos.below())) {

                world.setBlock(closePos, Blocks.CHEST.defaultBlockState(), 3);
                EldoriaMod.LOGGER.info("📜 Trésor forcé aux coordonnées proches du joueur : X={}, Y={}, Z={}", closePos.getX(), closePos.getY(), closePos.getZ());

                world.getServer().execute(() -> {
                    boolean lootAdded = addLootToChest(world, closePos);
                    if (!lootAdded) {
                        EldoriaMod.LOGGER.error("❌ [ERREUR] Impossible de remplir le coffre au trésor !");
                    } else {
                        giveExplorerBook(player, closePos);
                    }
                });

                return closePos;
            }
        }

        EldoriaMod.LOGGER.error("❌ Impossible de générer un trésor même à proximité du joueur !");
        return null; // 🚨 Aucun trésor généré
    }

    private static boolean isInvalidBlock(ServerLevel world, BlockPos pos) {
        return world.getBlockState(pos).is(Blocks.OAK_LEAVES) ||
                world.getBlockState(pos).is(Blocks.BIRCH_LEAVES) ||
                world.getBlockState(pos).is(Blocks.SPRUCE_LEAVES) ||
                world.getBlockState(pos).is(Blocks.JUNGLE_LEAVES) ||
                world.getBlockState(pos).is(Blocks.ACACIA_LEAVES) ||
                world.getBlockState(pos).is(Blocks.DARK_OAK_LEAVES) ||
                world.getBlockState(pos).is(Blocks.MANGROVE_LEAVES) ||
                world.getBlockState(pos).is(Blocks.AZALEA_LEAVES) ||
                world.getBlockState(pos).is(Blocks.FLOWERING_AZALEA_LEAVES) ||
                world.getBlockState(pos).is(Blocks.VINE) ||
                world.getBlockState(pos).is(Blocks.CAVE_VINES) ||
                world.getBlockState(pos).is(Blocks.CAVE_VINES_PLANT) ||
                world.getBlockState(pos).is(Blocks.WEEPING_VINES) ||
                world.getBlockState(pos).is(Blocks.WEEPING_VINES_PLANT) ||
                world.getBlockState(pos).is(Blocks.TWISTING_VINES) ||
                world.getBlockState(pos).is(Blocks.TWISTING_VINES_PLANT);
    }

    private static boolean addLootToChest(ServerLevel world, BlockPos chestPos) {
        BlockEntity blockEntity = world.getBlockEntity(chestPos);

        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            LOGGER.error("❌ [ERREUR] Impossible de récupérer l'entité de coffre à [{}]", chestPos);
            return false;
        }

        List<ItemStack> possibleLoot = new ArrayList<>();

        // ✅ Ressources de base
        Collections.addAll(possibleLoot,
                new ItemStack(Items.IRON_INGOT, 10 + random.nextInt(10)),
                new ItemStack(Items.GOLD_INGOT, 8 + random.nextInt(6)),
                new ItemStack(Items.EMERALD, 5 + random.nextInt(5)),
                new ItemStack(Items.COAL, 16 + random.nextInt(16))
        );

        // ✅ Nourriture
        Collections.addAll(possibleLoot,
                new ItemStack(Items.GOLDEN_CARROT, 10 + random.nextInt(6)),
                new ItemStack(Items.COOKED_BEEF, 10 + random.nextInt(10)),
                new ItemStack(Items.BREAD, 12 + random.nextInt(6))
        );

        // ✅ Outils enchantés
        List<ItemStack> tools = List.of(
                enchantIfLucky(new ItemStack(Items.IRON_PICKAXE)),
                enchantIfLucky(new ItemStack(Items.DIAMOND_PICKAXE)),
                enchantIfLucky(new ItemStack(Items.IRON_AXE)),
                enchantIfLucky(new ItemStack(Items.DIAMOND_AXE))
        );
        possibleLoot.add(tools.get(random.nextInt(tools.size())));

        // ✅ Armes et munitions
        List<ItemStack> weapons = List.of(
                enchantIfLucky(new ItemStack(Items.IRON_SWORD)),
                enchantIfLucky(new ItemStack(Items.DIAMOND_SWORD)),
                enchantIfLucky(new ItemStack(Items.BOW)),
                enchantIfLucky(new ItemStack(Items.CROSSBOW))
        );
        ItemStack chosenWeapon = weapons.get(random.nextInt(weapons.size()));
        possibleLoot.add(chosenWeapon);

        if (chosenWeapon.getItem() == Items.BOW || chosenWeapon.getItem() == Items.CROSSBOW) {
            possibleLoot.add(new ItemStack(Items.ARROW, 16 + random.nextInt(16)));
        }

        // ✅ Armure enchantée
        List<ItemStack> armors = List.of(
                enchantIfLucky(new ItemStack(Items.IRON_HELMET)),
                enchantIfLucky(new ItemStack(Items.DIAMOND_HELMET)),
                enchantIfLucky(new ItemStack(Items.IRON_CHESTPLATE)),
                enchantIfLucky(new ItemStack(Items.DIAMOND_CHESTPLATE)),
                enchantIfLucky(new ItemStack(Items.IRON_LEGGINGS)),
                enchantIfLucky(new ItemStack(Items.DIAMOND_LEGGINGS)),
                enchantIfLucky(new ItemStack(Items.IRON_BOOTS)),
                enchantIfLucky(new ItemStack(Items.DIAMOND_BOOTS))
        );
        possibleLoot.add(armors.get(random.nextInt(armors.size())));

        // ✅ Items rares boostés
        if (random.nextFloat() < 0.6) possibleLoot.add(new ItemStack(Items.DIAMOND, 2 + random.nextInt(3)));
        if (random.nextFloat() < 0.5) possibleLoot.add(new ItemStack(Items.GOLDEN_APPLE, 2 + random.nextInt(2)));
        if (random.nextFloat() < 0.4) possibleLoot.add(new ItemStack(Items.NETHERITE_SCRAP, 2 + random.nextInt(2)));
        if (random.nextFloat() < 0.3) possibleLoot.add(new ItemStack(Items.TOTEM_OF_UNDYING));
        if (random.nextFloat() < 0.2) possibleLoot.add(new ItemStack(Items.NETHERITE_INGOT, 2 + random.nextInt(2)));

        // ✅ Potions avec effets corrigés
        List<ItemStack> potions = List.of(
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.STRENGTH),
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.FIRE_RESISTANCE),
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.SWIFTNESS)
        );
        if (random.nextFloat() < 0.5) possibleLoot.add(potions.get(random.nextInt(potions.size())));

        // ✅ Mélange et placement des objets (14 à 20 slots remplis)
        Collections.shuffle(possibleLoot);
        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < chest.getContainerSize(); i++) availableSlots.add(i);
        Collections.shuffle(availableSlots);

        int itemsToPlace = 14 + random.nextInt(7);
        for (int i = 0; i < Math.min(itemsToPlace, possibleLoot.size()); i++) {
            chest.setItem(availableSlots.remove(0), possibleLoot.get(i));
        }

        chest.setChanged();
        LOGGER.info("✅ Loot boosté ajouté dans le coffre !");
        return true;
    }

    public static void giveExplorerBook(ServerPlayer player, BlockPos treasureCoords) {
        if (player == null || treasureCoords == null) {
            EldoriaMod.LOGGER.error("❌ [ERREUR] Impossible de donner le Journal de l'explorateur : Joueur ou coordonnées invalides !");
            return;
        }

        EldoriaMod.LOGGER.info("[DEBUG] Tentative de don du Journal de l'explorateur à {} aux coordonnées {}", player.getName().getString(), treasureCoords);

        player.getServer().execute(() -> { // ✅ Exécution sur le bon thread
            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
            CompoundTag tag = new CompoundTag();

            tag.putString("title", "Journal de l'Explorateur");
            tag.putString("author", "Ancien Voyageur");

            String content = """
        Cher aventurier,

        Votre exploration vous a mené à un précieux trésor !

        📍 Un coffre a été dissimulé à ces coordonnées :

        X: %d | Y: %d | Z: %d

        🏆 Bonne chance pour le retrouver !
        """.formatted(treasureCoords.getX(), treasureCoords.getY(), treasureCoords.getZ());

            ListTag pages = new ListTag();
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(content))));
            tag.put("pages", pages);

            book.setTag(tag);

            boolean added = player.getInventory().add(book);
            if (!added) {
                player.drop(book, false);
                EldoriaMod.LOGGER.warn("⚠️ L'inventaire de {} est plein, le livre a été drop au sol.", player.getName().getString());
            } else {
                EldoriaMod.LOGGER.info("📖 Journal de l'explorateur donné avec succès à {}", player.getName().getString());
            }
        });
    }

    /**
     * Ajoute un enchantement aléatoire à un item avec 40% de chances.
     */
    private static ItemStack enchantIfLucky(ItemStack item) {
        if (random.nextFloat() < 0.4) {
            item.enchant(Enchantments.UNBREAKING, 1 + random.nextInt(3));
        }
        return item;
    }
}