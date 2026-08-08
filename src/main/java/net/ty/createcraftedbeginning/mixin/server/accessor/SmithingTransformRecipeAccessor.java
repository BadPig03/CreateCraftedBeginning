package net.ty.createcraftedbeginning.mixin.server.accessor;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.ty.createcraftedbeginning.platform.access.SmithingTransformRecipeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SmithingTransformRecipe.class)
public interface SmithingTransformRecipeAccessor extends SmithingTransformRecipeAccess {
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
