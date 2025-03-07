package com.example.eldoria.treasures;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public class TreasureHintHandler {

    public static void placeHintChest(ServerLevel world, BlockPos pos) {
        world.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
    }
}