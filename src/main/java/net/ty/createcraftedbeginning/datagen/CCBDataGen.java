package net.ty.createcraftedbeginning.datagen;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.compat.functionalstorage.datagen.GasDrawerLootConditions;
import net.ty.createcraftedbeginning.datagen.provider.CCBDamageTypeTagsProvider;
import net.ty.createcraftedbeginning.datagen.provider.CCBDatapackBuiltinEntriesProvider;
import net.ty.createcraftedbeginning.datagen.provider.CCBEnchantmentTagsProvider;
import net.ty.createcraftedbeginning.datagen.provider.CCBGasTagsProvider;
import net.ty.createcraftedbeginning.datagen.provider.CCBRecipeProvider;
import net.ty.createcraftedbeginning.datagen.recipe.CCBSequencedAssemblyRecipes;
import net.ty.createcraftedbeginning.datagen.tag.CCBRegistrateTags;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrateProvider;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBDataGen {
    private static final CreateRegistrate CCB_REGISTRATE = CCBRegistrateProvider.get();

    public static void gatherDataHighPriority(GatherDataEvent event) {
        if (!event.getMods().contains(CCBAPI.MOD_ID)) {
            return;
        }

        addExtraRegistrateData();
    }

    public static void gatherData(GatherDataEvent event) {
        if (!event.getMods().contains(CCBAPI.MOD_ID)) {
            return;
        }

        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<Provider> registries = event.getLookupProvider();
        ExistingFileHelper existingFiles = event.getExistingFileHelper();

        CCBDatapackBuiltinEntriesProvider entriesProvider = new CCBDatapackBuiltinEntriesProvider(output, registries);
        registries = entriesProvider.getRegistryProvider();

        generator.addProvider(event.includeClient(), CCBSoundEvents.provider(generator));
        generator.addProvider(event.includeServer(), entriesProvider);
        generator.addProvider(event.includeServer(), new CCBAdvancements(output, registries));
        generator.addProvider(event.includeServer(), new CCBDamageTypeTagsProvider(output, registries, existingFiles));
        generator.addProvider(event.includeServer(), new CCBEnchantmentTagsProvider(output, registries, existingFiles));
        generator.addProvider(event.includeServer(), new CCBGasTagsProvider(output, registries, existingFiles));
        generator.addProvider(event.includeServer(), new CCBSequencedAssemblyRecipes(output, registries));
        if (!event.includeServer()) {
            return;
        }

        CCBRecipeProvider.registerAllProcessing(generator, output, registries);
        CCBRecipeProvider.registerAllGasProcessing(generator, output, registries);
        if (!CCBCompatMods.FUNCTIONAL_STORAGE.isLoaded()) {
            return;
        }

        generator.addProvider(true, new GasDrawerLootConditions(output));
    }

    private static void addExtraRegistrateData() {
        CCBRegistrateTags.addGenerators();
        CCB_REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
            BiConsumer<String, String> langConsumer = provider::add;
            CCBAdvancements.provideLang(langConsumer);
            CCBGases.provideLang(langConsumer);
            CCBSoundEvents.provideLang(langConsumer);
        });
    }
}
