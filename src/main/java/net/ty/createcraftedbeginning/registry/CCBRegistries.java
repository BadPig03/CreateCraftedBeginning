package net.ty.createcraftedbeginning.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasIngredientType;
import net.ty.createcraftedbeginning.api.gas.gases.MountedGasStorageType;
import org.jetbrains.annotations.NotNull;

public class CCBRegistries {
    public static final ResourceKey<Registry<MountedGasStorageType<?>>> MOUNTED_GAS_STORAGE_TYPE = key("mounted_gas_storage_type");
    public static final ResourceKey<Registry<Gas>> GAS_REGISTRY_KEY = key("gas");
    public static final ResourceKey<Registry<GasIngredientType<?>>> GAS_INGREDIENT_TYPES = key("gas_ingredient_type");

    @SuppressWarnings("SameParameterValue")
    private static <T> @NotNull ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(CreateCraftedBeginning.asResource(name));
    }
}
