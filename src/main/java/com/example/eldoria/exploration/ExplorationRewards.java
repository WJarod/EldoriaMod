package com.example.eldoria.exploration;

import com.example.eldoria.EldoriaMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
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

        // ✅ Ressources de base ultra boostées
        Collections.addAll(possibleLoot,
                new ItemStack(Items.IRON_INGOT, 16 + random.nextInt(10)),
                new ItemStack(Items.GOLD_INGOT, 12 + random.nextInt(8)),
                new ItemStack(Items.EMERALD, 8 + random.nextInt(5)),
                new ItemStack(Items.COAL, 20 + random.nextInt(10))
        );

        // ✅ Nourriture en quantité généreuse
        Collections.addAll(possibleLoot,
                new ItemStack(Items.GOLDEN_CARROT, 12 + random.nextInt(6)),
                new ItemStack(Items.COOKED_BEEF, 14 + random.nextInt(8)),
                new ItemStack(Items.BREAD, 16 + random.nextInt(6))
        );

        // ✅ Outils OP avec multi-enchantements
        List<ItemStack> tools = List.of(
                enchantOP(new ItemStack(Items.NETHERITE_PICKAXE)),
                enchantOP(new ItemStack(Items.NETHERITE_AXE)),
                enchantOP(new ItemStack(Items.NETHERITE_SHOVEL)),
                enchantOP(new ItemStack(Items.DIAMOND_PICKAXE)),
                enchantOP(new ItemStack(Items.DIAMOND_AXE)),
                enchantOP(new ItemStack(Items.DIAMOND_SHOVEL))
        );
        possibleLoot.add(tools.get(random.nextInt(tools.size())));

        // ✅ Armes boostées avec enchantements avancés
        List<ItemStack> weapons = List.of(
                enchantOP(new ItemStack(Items.NETHERITE_SWORD)),
                enchantOP(new ItemStack(Items.DIAMOND_SWORD)),
                enchantOP(new ItemStack(Items.BOW)),
                enchantOP(new ItemStack(Items.CROSSBOW)),
                enchantOP(new ItemStack(Items.TRIDENT))
        );
        ItemStack chosenWeapon = weapons.get(random.nextInt(weapons.size()));
        possibleLoot.add(chosenWeapon);

        if (chosenWeapon.getItem() == Items.BOW || chosenWeapon.getItem() == Items.CROSSBOW) {
            possibleLoot.add(new ItemStack(Items.ARROW, 24 + random.nextInt(10)));
        }

        // ✅ Armures légendaires
        List<ItemStack> armors = List.of(
                enchantOP(new ItemStack(Items.NETHERITE_HELMET)),
                enchantOP(new ItemStack(Items.NETHERITE_CHESTPLATE)),
                enchantOP(new ItemStack(Items.NETHERITE_LEGGINGS)),
                enchantOP(new ItemStack(Items.NETHERITE_BOOTS)),
                enchantOP(new ItemStack(Items.DIAMOND_HELMET)),
                enchantOP(new ItemStack(Items.DIAMOND_CHESTPLATE)),
                enchantOP(new ItemStack(Items.DIAMOND_LEGGINGS)),
                enchantOP(new ItemStack(Items.DIAMOND_BOOTS))
        );
        possibleLoot.add(armors.get(random.nextInt(armors.size())));

        // ✅ Drops ultra rares (avec chances améliorées)
        if (random.nextFloat() < 0.8) possibleLoot.add(new ItemStack(Items.DIAMOND, 4 + random.nextInt(3)));
        if (random.nextFloat() < 0.7) possibleLoot.add(new ItemStack(Items.GOLDEN_APPLE, 3 + random.nextInt(2)));
        if (random.nextFloat() < 0.6) possibleLoot.add(new ItemStack(Items.NETHERITE_SCRAP, 3 + random.nextInt(2)));
        if (random.nextFloat() < 0.5) possibleLoot.add(new ItemStack(Items.TOTEM_OF_UNDYING));
        if (random.nextFloat() < 0.4) possibleLoot.add(new ItemStack(Items.NETHERITE_INGOT, 2 + random.nextInt(2)));

        // ✅ Potions ultra cheatées
        List<ItemStack> potions = List.of(
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.STRENGTH),
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.FIRE_RESISTANCE),
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.SWIFTNESS),
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.REGENERATION),
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING),
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.LONG_SLOW_FALLING)
        );
        if (random.nextFloat() < 0.7) possibleLoot.add(potions.get(random.nextInt(potions.size())));

        // ✅ Placement amélioré du loot (18 à 24 slots remplis)
        Collections.shuffle(possibleLoot);
        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < chest.getContainerSize(); i++) availableSlots.add(i);
        Collections.shuffle(availableSlots);

        int itemsToPlace = 18 + random.nextInt(7);
        for (int i = 0; i < Math.min(itemsToPlace, possibleLoot.size()); i++) {
            chest.setItem(availableSlots.remove(0), possibleLoot.get(i));
        }

        chest.setChanged();
        LOGGER.info("✅ Loot LEGEND ajouté dans le coffre !");
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
     * Applique plusieurs enchantements puissants à un objet.
     */
    private static ItemStack enchantOP(ItemStack item) {
        if (item.getItem() instanceof SwordItem) {
            item.enchant(Enchantments.SHARPNESS, 3 + random.nextInt(3));
            item.enchant(Enchantments.MOB_LOOTING, 2 + random.nextInt(2));
            item.enchant(Enchantments.UNBREAKING, 3);
            if (random.nextFloat() < 0.5) item.enchant(Enchantments.FIRE_ASPECT, 2);
        }

        if (item.getItem() instanceof PickaxeItem) {
            item.enchant(Enchantments.BLOCK_EFFICIENCY, 4 + random.nextInt(2));
            item.enchant(Enchantments.UNBREAKING, 3);
            item.enchant(Enchantments.BLOCK_FORTUNE, 2 + random.nextInt(2));
        }

        if (item.getItem() instanceof AxeItem) {
            item.enchant(Enchantments.BLOCK_EFFICIENCY, 4 + random.nextInt(2));
            item.enchant(Enchantments.UNBREAKING, 3);
            if (random.nextFloat() < 0.6) item.enchant(Enchantments.SHARPNESS, 3 + random.nextInt(2));
        }

        if (item.getItem() instanceof ArmorItem) {
            item.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
            item.enchant(Enchantments.UNBREAKING, 3);
            if (random.nextFloat() < 0.5) item.enchant(Enchantments.THORNS, 2 + random.nextInt(2));
        }

        if (item.getItem() instanceof BowItem || item.getItem() instanceof CrossbowItem) {
            item.enchant(Enchantments.POWER_ARROWS, 4 + random.nextInt(2));
            item.enchant(Enchantments.UNBREAKING, 3);
            if (random.nextFloat() < 0.5) item.enchant(Enchantments.FLAMING_ARROWS, 1);
        }

        if (item.getItem() instanceof TridentItem) {
            item.enchant(Enchantments.LOYALTY, 3);
            item.enchant(Enchantments.IMPALING, 3 + random.nextInt(2));
            item.enchant(Enchantments.UNBREAKING, 3);
        }

        return item;
    }
}