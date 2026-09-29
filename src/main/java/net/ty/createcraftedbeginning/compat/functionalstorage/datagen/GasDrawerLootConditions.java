package net.ty.createcraftedbeginning.compat.functionalstorage.datagen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerLootConditions implements DataProvider {
    private final Path lootDirectory;

    public GasDrawerLootConditions(PackOutput output) {
        lootDirectory = output.getOutputFolder(Target.DATA_PACK).resolve(CCBAPI.MOD_ID).resolve("loot_table/blocks");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> saves = new ArrayList<>();
        ModLoadedCondition condition = new ModLoadedCondition(CCBCompatMods.FUNCTIONAL_STORAGE.id());
        for (int size : List.of(1, 2, 4)) {
            Path path = lootDirectory.resolve("gas_drawer_" + size + ".json");
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                JsonObject table = JsonParser.parseReader(reader).getAsJsonObject();
                ICondition.writeConditions(JsonOps.INSTANCE, table, List.of(condition));
                saves.add(DataProvider.saveStable(output, table, path));
            }
            catch (IOException exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }

        return CompletableFuture.allOf(saves.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Create Crafted Beginning: Gas Drawer Loot Conditions";
    }
}
