package net.ty.createcraftedbeginning.recipe;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBEnergizationRecipes extends EnergizationRecipeGen {
    public CCBEnergizationRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
        addGasRecipes(registries);
    }

    private void addGasRecipes(@NotNull CompletableFuture<Provider> registriesFuture) {
        registriesFuture.thenAccept(registries -> {
            RegistryLookup<Gas> gasLookup = registries.lookupOrThrow(CCBRegistries.GAS_REGISTRY_KEY);
            gasLookup.listElements().forEach(holder -> {
                Gas gas = holder.value();
                if (gas.getEnergizedGas().isEmpty()) {
                    return;
                }

                String gasName = Objects.requireNonNull(holder.getKey()).location().getPath();
                create(gasName, b -> b.require(holder.value(), 1).output(gas.getEnergizedGas(), 1));
            });
        });
    }
}
