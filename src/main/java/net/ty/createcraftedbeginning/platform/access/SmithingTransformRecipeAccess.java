package net.ty.createcraftedbeginning.platform.access;

import net.minecraft.world.item.crafting.Ingredient;

public interface SmithingTransformRecipeAccess {
    Ingredient getTemplate();

    Ingredient getBase();

    Ingredient getAddition();
}
