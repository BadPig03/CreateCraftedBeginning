package net.ty.createcraftedbeginning.compat.jade.gas;

import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

public class GasConstants {
    public static String STORAGE_KEY = "JadeGasStorage";
    public static String STORAGE_UID_KEY = "JadeGasStorageUid";
    public static String STORAGE_GAS_KEY = "gas";
    public static String STORAGE_CAPACITY_KEY = "capacity";
    public static String STORAGE_CREATIVE_KEY = "creative";

    public static ResourceLocation GAS = CreateCraftedBeginning.asResource("gas");
    public static ResourceLocation TOOLTIP_RENDERER = CreateCraftedBeginning.asResource("tooltip_renderer");
    public static ResourceLocation DATA_PROVIDER = CreateCraftedBeginning.asResource("data_provider");
    public static ResourceLocation CONTRAPTION_DATA_PROVIDER = CreateCraftedBeginning.asResource("contraption_data_provider");
    public static ResourceLocation CONTRAPTION_TOOLTIP_RENDERER = CreateCraftedBeginning.asResource("contraption_tooltip_renderer");
}
