package com.example.eldoria.client.gui;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.IContainerFactory;

public class QuestGiverMenu extends AbstractContainerMenu {

    public QuestGiverMenu(int windowId, Inventory playerInventory) {
        super(MenuType.GENERIC_9x3, windowId);
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY; // ✅ Obligatoire pour éviter l'erreur de compilation
    }

    public static class Factory implements IContainerFactory<QuestGiverMenu> {
        @Override
        public QuestGiverMenu create(int windowId, Inventory inv, FriendlyByteBuf data) {
            return new QuestGiverMenu(windowId, inv);
        }
    }
}