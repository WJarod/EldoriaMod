package com.example.eldoria.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec CLIENT_SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Eldoria Client Configurations")
                .push("eldoria_settings");

        // Exemple d'option de configuration (modifiable selon les besoins)
        BUILDER.pop();

        CLIENT_SPEC = BUILDER.build();
    }
}