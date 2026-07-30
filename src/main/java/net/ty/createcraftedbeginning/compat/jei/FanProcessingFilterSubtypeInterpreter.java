package net.ty.createcraftedbeginning.compat.jei;

import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum FanProcessingFilterSubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
    INSTANCE;

    @Override
    public @Nullable Object getSubtypeData(ItemStack ingredient, UidContext context) {
        return GasInjectionChamberUtils.getFanProcessingTypeId(ingredient).orElse(null);
    }

    @Override
    public String getLegacyStringSubtypeInfo(ItemStack ingredient, UidContext context) {
        return GasInjectionChamberUtils.getFanProcessingTypeId(ingredient).map(ResourceLocation::toString).orElse("");
    }
}
