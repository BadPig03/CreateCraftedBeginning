package net.ty.createcraftedbeginning.platform.access;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface SmithingRecipeAccess {
    Ingredient ccb$getTemplate();

    Ingredient ccb$getBase();

    Ingredient ccb$getAddition();
}
