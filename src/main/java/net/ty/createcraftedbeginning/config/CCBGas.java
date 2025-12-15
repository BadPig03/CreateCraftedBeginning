package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CCBGas extends ConfigBase {
    public final ConfigBool canExtractAirFromWorld = b(true, "canExtractAirFromWorld", Comments.canExtractAirFromWorld);
    public final ConfigBool canCoolerGetFromSpawners = b(true, "canCoolerGetFromSpawners", Comments.canCoolerGetFromSpawners);
    public final ConfigInt canisterCapacity = i(20, 1, "canisterCapacity", Comments.buckets, Comments.canisterCapacity);
    public final ConfigInt maxVortexingAmount = i(1000, 1, "maxVortexingAmount", Comments.maxVortexingAmount);

    @Override
    public @NotNull String getName() {
        return "gas";
    }

    private static class Comments {
        static String buckets = "[in Buckets]";
        static String canExtractAirFromWorld = "Allows opened pipes to extract air from the environment.";
        static String canCoolerGetFromSpawners = "Allows using an Empty Breeze Cooler on Spawners and Trial Spawners to obtain a Breeze Cooler with Breeze.";
        static String canisterCapacity = "The amount of Compressed Air a Compressed Air Canister can hold per item.";
        static String maxVortexingAmount = "The amount of Air vortexed by the Breeze Chamber per second.";
    }
}
