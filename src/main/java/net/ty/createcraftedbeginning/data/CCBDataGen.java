package net.ty.createcraftedbeginning.data;

import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.provider.CCBDamageTypeTagsProvider;
import net.ty.createcraftedbeginning.provider.CCBDatapackBuiltinEntriesProvider;
import net.ty.createcraftedbeginning.provider.CCBEnchantmentTagsProvider;
import net.ty.createcraftedbeginning.provider.CCBGasTagsProvider;
import net.ty.createcraftedbeginning.provider.CCBRecipeProvider;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBRegistrateTags;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBDataGen {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate();

    public static void gatherDataHighPriority(GatherDataEvent event) {
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

    public static void gatherData(GatherDataEvent event) {
        if (!event.getMods().contains(CreateCraftedBeginning.MOD_ID)) {
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
        generator.addProvider(event.includeServer(), new CCBSequencedAssemblyWithGasRecipes(output, registries));
        if (!event.includeServer()) {
            return;
        }

        CCBRecipeProvider.registerAllProcessing(generator, output, registries);
        CCBRecipeProvider.registerAllProcessingWithGas(generator, output, registries);
    }
}