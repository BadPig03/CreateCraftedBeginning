package net.ty.createcraftedbeginning.mixin.server.accessor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.ty.createcraftedbeginning.platform.access.SmithingRecipeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(SmithingTrimRecipe.class)
public interface SmithingTrimRecipeAccessor extends SmithingRecipeAccess {
    @Override
    @Accessor("template")
    Ingredient ccb$getTemplate();

    @Override
    @Accessor("base")
    Ingredient ccb$getBase();

    @Override
    @Accessor("addition")
    Ingredient ccb$getAddition();
}
