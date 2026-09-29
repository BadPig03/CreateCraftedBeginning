package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRegistries {
    public static final ResourceKey<Registry<Gas>> GAS_REGISTRY_KEY = ResourceKey.createRegistryKey(CCBAPI.asResource("gas"));
    public static final ResourceKey<Gas> EMPTY_GAS_KEY = ResourceKey.create(GAS_REGISTRY_KEY, CCBAPI.asResource("empty"));

    @SuppressWarnings("deprecation")
    public static final DefaultedRegistry<Gas> GAS_REGISTRY = (DefaultedRegistry<Gas>) new RegistryBuilder<>(GAS_REGISTRY_KEY).defaultKey(EMPTY_GAS_KEY).sync(true).withIntrusiveHolders().create();

    private GasRegistries() {
    }
}
