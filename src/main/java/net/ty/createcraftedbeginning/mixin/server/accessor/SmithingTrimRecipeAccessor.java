package net.ty.createcraftedbeginning.mixin.server.accessor;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.ty.createcraftedbeginning.platform.access.SmithingTrimRecipeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SmithingTrimRecipe.class)
public interface SmithingTrimRecipeAccessor extends SmithingTrimRecipeAccess {
    @Override
    @Accessor("template")
    Ingredient getTemplate();

    @Override
    @Accessor("base")
    Ingredient getBase();

    @Override
    @Accessor("addition")
    Ingredient getAddition();
}
