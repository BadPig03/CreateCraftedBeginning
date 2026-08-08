package net.ty.createcraftedbeginning.platform.access;

import net.minecraft.world.item.crafting.Ingredient;

public interface SmithingTrimRecipeAccess {
    Ingredient getTemplate();

    Ingredient getBase();

    Ingredient getAddition();
}
