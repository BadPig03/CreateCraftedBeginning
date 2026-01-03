package net.ty.createcraftedbeginning.data;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasBuilder;
import net.ty.createcraftedbeginning.api.gas.gases.GasHolder;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class CCBGasDeferredRegister extends DeferredRegister<Gas> {
    private final Function<ResourceKey<Gas>, GasHolder<Gas, Gas>> holderCreator = GasHolder::new;

    public CCBGasDeferredRegister(String modId) {
        super(CCBRegistries.GAS_REGISTRY_KEY, modId);
    }

    public GasHolder<Gas, Gas> register(String name, GasBuilder builder) {
        return register(name, () -> new Gas(builder));
    }

    @Override
    public <I extends Gas> @NotNull GasHolder<Gas, I> register(@NotNull String name, @NotNull Supplier<? extends I> supplier) {
        return (GasHolder<Gas, I>) super.register(name, supplier);
    }

    @Override
    public <I extends Gas> @NotNull GasHolder<Gas, I> register(@NotNull String name, @NotNull Function<ResourceLocation, ? extends I> func) {
        return (GasHolder<Gas, I>) super.register(name, func);
    }

    @Override
    protected <I extends Gas> @NotNull GasHolder<Gas, I> createHolder(@NotNull ResourceKey<? extends Registry<Gas>> registryKey, @NotNull ResourceLocation key) {
        return (GasHolder<Gas, I>) holderCreator.apply(ResourceKey.create(registryKey, key));
    }
}
