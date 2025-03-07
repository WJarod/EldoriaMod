package com.example.eldoria.exploration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import java.util.Random;

public class AncientTreasure {
    private static final Random random = new Random();

    public static ItemStack createTreasureNote() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = new CompoundTag();

        int x = random.nextInt(500) - 250;
        int y = 70;
        int z = random.nextInt(500) - 250;

        tag.putString("title", "Note Mystérieuse");
        tag.putString("author", "Explorateur Disparu");

        String content = """
        Un explorateur disparu a laissé cette note...
        
        Il parle d’un mystérieux trésor enfoui quelque part...
        
        Suis ces coordonnées :
        
        X: %d | Y: %d | Z: %d
        """.formatted(x, y, z);

        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(content))));
        tag.put("pages", pages);

        book.setTag(tag);
        return book;
    }
}