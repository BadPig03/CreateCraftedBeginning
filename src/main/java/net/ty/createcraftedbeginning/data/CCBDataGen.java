package net.ty.createcraftedbeginning.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import java.util.concurrent.CompletableFuture;

public class CCBDataGen {
    public static void gatherDataHighPriority(GatherDataEvent event) {
        if (event.getMods().contains(CreateCraftedBeginning.MOD_ID)) {
            addExtraRegistrateData();
        }
    }

    public static void gatherData(GatherDataEvent event) {
        if (!event.getMods().contains(CreateCraftedBeginning.MOD_ID)) {
            return;
        }

        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CCBGeneratedEntriesProvider generatedEntriesProvider = new CCBGeneratedEntriesProvider(output, lookupProvider);
        lookupProvider = generatedEntriesProvider.getRegistryProvider();
        generator.addProvider(event.includeServer(), generatedEntriesProvider);

        generator.addProvider(event.includeServer(), new CCBDamageTypeTagGen(output, lookupProvider, existingFileHelper));
    }

    private static void addExtraRegistrateData() {
        CCBRegistrateTags.addGenerators();
    }
}