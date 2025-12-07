package net.ty.createcraftedbeginning.provider;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.data.CCBGases;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class CCBGasTagsProvider extends TagsProvider<Gas> {
    public CCBGasTagsProvider(PackOutput output, CompletableFuture<Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, CCBRegistries.GAS_REGISTRY_KEY, lookupProvider, CreateCraftedBeginning.MOD_ID, existingFileHelper);
    }

    @Override
    public @NotNull String getName() {
        return "Create: Crafted Beginning's Gas Tags";
    }

    @Override
    protected void addTags(@NotNull Provider provider) {
        CCBGases.GAS_REGISTER.getEntries().forEach(holder -> holder.get().getTags().forEach(tag -> tag(tag).add(holder.getKey())));
    }
}
