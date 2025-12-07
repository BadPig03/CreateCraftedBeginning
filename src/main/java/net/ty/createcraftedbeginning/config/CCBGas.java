package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CCBGas extends ConfigBase {
    public final ConfigBool canExtractAirFromWorld = b(true, "canExtractAirFromWorld", Comments.canExtractAirFromWorld);
    public final ConfigBool canCoolerGetFromSpawners = b(true, "canCoolerGetFromSpawners", Comments.canCoolerGetFromSpawners);
    public final ConfigInt pressurizationRateMultiplier = i(4, 1, "pressurizationRateMultiplier", Comments.pressurizationRateMultiplier);
    public final ConfigInt airtightPumpRange = i(32, 1, "airtightPumpRange", Comments.blocks, Comments.airtightPumpRange);
    public final ConfigInt airtightTankCapacity = i(80, 1, "airtightTankCapacity", Comments.buckets, Comments.airtightTankCapacity);
    public final ConfigInt canisterCapacity = i(20, 1, "canisterCapacity", Comments.buckets, Comments.canisterCapacity);
    public final ConfigInt maxVortexingAmount = i(1000, 1, "maxVortexingAmount", Comments.milliBuckets, Comments.maxVortexingAmount);

    @Override
    public @NotNull String getName() {
        return "gas";
    }

    private static class Comments {
        static String blocks = "[in Blocks]";
        static String buckets = "[in Buckets]";
        static String milliBuckets = "[in milli-Buckets]";
        static String airtightPumpRange = "The maximum distance an Airtight Pump can push or pull gas on either side.";
        static String airtightTankCapacity = "The amount of Compressed Air a Airtight Tank can hold per block.";
        static String canExtractAirFromWorld = "Allows opened pipes to extract air from the environment.";
        static String canCoolerGetFromSpawners = "Allows using an Empty Breeze Cooler on Spawners and Trial Spawners to obtain a Breeze Cooler with Breeze.";
        static String canisterCapacity = "The amount of Compressed Air a Compressed Air Canister can hold per item.";
        static String maxVortexingAmount = "The amount of Air vortexed by the Breeze Chamber per second.";
        static String pressurizationRateMultiplier = "The pressurization rate multiplier for Air Compressors when pressurizing gases.";
    }
}
