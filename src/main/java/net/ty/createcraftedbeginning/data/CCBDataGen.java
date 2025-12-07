package net.ty.createcraftedbeginning.data;

import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.provider.CCBDamageTypeTagsProvider;
import net.ty.createcraftedbeginning.provider.CCBEnchantmentTagsProvider;
import net.ty.createcraftedbeginning.provider.CCBGasTagsProvider;
import net.ty.createcraftedbeginning.provider.CCBDatapackBuiltinEntriesProvider;
import net.ty.createcraftedbeginning.provider.CCBRecipeProvider;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.recipe.CCBSequencedAssemblyWithGasRecipes;
import net.ty.createcraftedbeginning.registry.CCBRegistrateTags;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class CCBDataGen {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate();

    public static void gatherDataHighPriority(@NotNull GatherDataEvent event) {
        if (!event.getMods().contains(CreateCraftedBeginning.MOD_ID)) {
            return;
        }

        addExtraRegistrateData();
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

    public static void gatherData(@NotNull GatherDataEvent event) {
        if (!event.getMods().contains(CreateCraftedBeginning.MOD_ID)) {
            return;
        }

        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), CCBSoundEvents.provider(generator));

        CCBDatapackBuiltinEntriesProvider generatedEntriesProvider = new CCBDatapackBuiltinEntriesProvider(output, lookupProvider);
        lookupProvider = generatedEntriesProvider.getRegistryProvider();

        generator.addProvider(event.includeServer(), generatedEntriesProvider);
        generator.addProvider(event.includeServer(), new CCBAdvancements(output, lookupProvider));
        generator.addProvider(event.includeServer(), new CCBDamageTypeTagsProvider(output, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(), new CCBEnchantmentTagsProvider(output, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(), new CCBGasTagsProvider(output, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(), new CCBSequencedAssemblyWithGasRecipes(output, lookupProvider));
        if (!event.includeServer()) {
            return;
        }

        CCBRecipeProvider.registerAllProcessing(generator, output, lookupProvider);
        CCBRecipeProvider.registerAllProcessingWithGas(generator, output, lookupProvider);
    }
}