package com.example.eldoria.exploration;

import com.example.eldoria.EldoriaMod;
import net.minecraft.network.chat.Component;
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
        int biomesExplored = ExplorationRanking.getPlayerBiomeCount(player);
        LOGGER.info("🎖 Vérification des récompenses... {} biomes explorés.", biomesExplored);

        if (biomesExplored % 5 == 0) {  // ✅ Trésor tous les 5 biomes explorés
            generateTreasure(player);
        }
    }

    public static BlockPos generateTreasure(Player player) {
        ServerLevel world = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();

        int attempts = 0;
        while (attempts < 10) { // 🔄 Essayer jusqu'à 10 fois
            attempts++;

            int distance = 250 + random.nextInt(251);
            double angle = random.nextDouble() * 2 * Math.PI;

            int x = playerPos.getX() + (int) (distance * Math.cos(angle));
            int z = playerPos.getZ() + (int) (distance * Math.sin(angle));
            int y = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);

            BlockPos surfacePos = new BlockPos(x, y, z);

            if (world.getBlockState(surfacePos.below()).isSolid()) { // ✅ Si le bloc est solide, on valide
                world.setBlock(surfacePos, Blocks.CHEST.defaultBlockState(), 3);
                LOGGER.info("📜 Trésor généré aux coordonnées : X={}, Y={}, Z={}", surfacePos.getX(), surfacePos.getY(), surfacePos.getZ());

                world.getServer().execute(() -> {
                    if (!addLootToChest(world, surfacePos)) {
                        LOGGER.error("❌ [ERREUR] Impossible de remplir le coffre au trésor !");
                    }
                });

                giveExplorerBook(player, surfacePos.getX(), surfacePos.getY(), surfacePos.getZ());
                player.displayClientMessage(Component.literal("Un Aventurier Mystérieux a laissé un indice… Consultez votre journal !"), false);

                return surfacePos;
            }

            EldoriaMod.LOGGER.warn("⚠️ [WARNING] Tentative de spawn sur un bloc non solide ! Recalcul...");
        }

        // 🚨 Si après 10 tentatives on ne trouve rien, on abandonne
        EldoriaMod.LOGGER.error("❌ Impossible de générer un trésor sur une surface correcte après 10 tentatives !");
        return playerPos; // Retourne la position du joueur pour éviter un crash
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

    private static void giveExplorerBook(Player player, int x, int y, int z) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = new CompoundTag();

        tag.putString("title", "Journal de l'Explorateur");
        tag.putString("author", "Ancien Voyageur");

        String content = """
        Cher aventurier,

        Votre exploration vous a mené à un précieux trésor !

        Un coffre a été dissimulé à ces coordonnées :

        X: %d | Y: %d | Z: %d

        Bonne chance pour le retrouver !
        """.formatted(x, y, z);

        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(content))));
        tag.put("pages", pages);

        book.setTag(tag);
        player.addItem(book);

        LOGGER.info("📖 Journal de l'explorateur donné à {}", player.getName().getString());
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