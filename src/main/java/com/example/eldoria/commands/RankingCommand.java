package com.example.eldoria.commands;

import com.example.eldoria.exploration.ExplorationRanking;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class RankingCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("classement")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        ExplorationRanking.displayRanking(player);
                    }
                    return Command.SINGLE_SUCCESS;
                }));
    }
}