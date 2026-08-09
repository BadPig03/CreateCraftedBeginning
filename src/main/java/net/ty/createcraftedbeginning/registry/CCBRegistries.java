package net.ty.createcraftedbeginning.registry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasRegistries;
import net.ty.createcraftedbeginning.content.airtights.gas.mounted.MountedGasStorageType;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBRegistries {
    public static final ResourceKey<Registry<MountedGasStorageType<?>>> MOUNTED_GAS_STORAGE_TYPE = ResourceKey.createRegistryKey(CCBAPI.asResource("mounted_gas_storage_type"));
    public static final ResourceKey<Registry<Gas>> GAS_REGISTRY_KEY = GasRegistries.GAS_REGISTRY_KEY;
}
