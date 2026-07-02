package net.ty.createcraftedbeginning.provider;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBDamageTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBDamageTypeTagsProvider extends TagsProvider<DamageType> {
    public CCBDamageTypeTagsProvider(PackOutput output, CompletableFuture<Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Registries.DAMAGE_TYPE, lookupProvider, CreateCraftedBeginning.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Create: Crafted Beginning's Damage Type Tags";
    }

    @Override
    protected void addTags(Provider provider) {
        tag(DamageTypeTags.IS_FIRE).add(CCBDamageTypes.BRIMSTONE).add(CCBDamageTypes.BRIMSTONE_FIRE);
    }
}
