package net.ty.createcraftedbeginning.provider;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBEnchantments;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBEnchantmentTagsProvider extends EnchantmentTagsProvider {
    public CCBEnchantmentTagsProvider(PackOutput output, CompletableFuture<Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, CreateCraftedBeginning.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Create: Crafted Beginning's Enchantments Tags";
    }

    @Override
    protected void addTags(Provider provider) {
        tag(EnchantmentTags.NON_TREASURE).add(CCBEnchantments.ECONOMIZE);
        tag(EnchantmentTags.IN_ENCHANTING_TABLE).add(CCBEnchantments.ECONOMIZE);
    }
}
