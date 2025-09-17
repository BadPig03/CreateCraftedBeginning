package net.ty.createcraftedbeginning.data;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasBuilder;

public class CCBGasRegistry {
    public static final ResourceKey<Registry<Gas>> GAS_REGISTRY_NAME = ResourceKey.createRegistryKey(CreateCraftedBeginning.asResource("gas"));
    public static final ResourceKey<Gas> EMPTY_GAS_KEY = ResourceKey.create(GAS_REGISTRY_NAME, CreateCraftedBeginning.asResource("empty"));

    @SuppressWarnings("deprecation")
    public static final DefaultedRegistry<Gas> GAS_REGISTRY = (DefaultedRegistry<Gas>) new RegistryBuilder<>(GAS_REGISTRY_NAME).defaultKey(EMPTY_GAS_KEY).sync(true).withIntrusiveHolders().create();
    public static final Gas EMPTY_GAS = new Gas(GasBuilder.builder());
}