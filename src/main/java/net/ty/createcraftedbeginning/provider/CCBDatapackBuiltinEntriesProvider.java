package net.ty.createcraftedbeginning.provider;

import com.simibubi.create.api.registry.CreateRegistries;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.init.CCBPotatoProjectileTypes;
import net.ty.createcraftedbeginning.registry.CCBDamageTypes;
import net.ty.createcraftedbeginning.registry.CCBEnchantments;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CCBDatapackBuiltinEntriesProvider extends DatapackBuiltinEntriesProvider {
    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder().add(Registries.DAMAGE_TYPE, CCBDamageTypes::bootstrap).add(CreateRegistries.POTATO_PROJECTILE_TYPE, CCBPotatoProjectileTypes::bootstrap).add(Registries.ENCHANTMENT, CCBEnchantments::bootstrap);

    public CCBDatapackBuiltinEntriesProvider(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, BUILDER, Set.of(CreateCraftedBeginning.MOD_ID));
    }

    @Override
    public @NotNull String getName() {
        return "Create: Crafted Beginning's Generated Entries";
    }
}
