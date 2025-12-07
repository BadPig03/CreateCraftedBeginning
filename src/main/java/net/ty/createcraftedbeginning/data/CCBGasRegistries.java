package net.ty.createcraftedbeginning.data;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.registry.CCBRegistries;

public class CCBGasRegistries {
    private static final ResourceLocation EMPTY = CreateCraftedBeginning.asResource("empty");

    public static final ResourceKey<Gas> EMPTY_GAS_KEY = ResourceKey.create(CCBRegistries.GAS_REGISTRY_KEY, EMPTY);

    @SuppressWarnings("deprecation")
    public static final DefaultedRegistry<Gas> GAS_REGISTRY = (DefaultedRegistry<Gas>) new RegistryBuilder<>(CCBRegistries.GAS_REGISTRY_KEY).defaultKey(EMPTY_GAS_KEY).sync(true).withIntrusiveHolders().create();
}