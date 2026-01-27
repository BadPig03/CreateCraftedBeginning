package net.ty.createcraftedbeginning.data;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.recipe.generators.PressurizationRecipeGen;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class CCBPressurizationRecipes extends PressurizationRecipeGen {
    public CCBPressurizationRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
        addGasRecipes(registries);
    }

    private void addGasRecipes(@NotNull CompletableFuture<Provider> registriesFuture) {
        registriesFuture.thenAccept(registries -> {
            RegistryLookup<Gas> gasLookup = registries.lookupOrThrow(CCBRegistries.GAS_REGISTRY_KEY);
            gasLookup.listElements().forEach(holder -> {
                Gas gasType = holder.value();
                if (gasType.getPressurizedGasType().isEmpty()) {
                    return;
                }

                String gasName = Objects.requireNonNull(holder.getKey()).location().getPath();
                create(gasName, b -> b.require(holder.value(), 10).output(gasType.getPressurizedGasType(), 1));
            });
        });
    }
}
