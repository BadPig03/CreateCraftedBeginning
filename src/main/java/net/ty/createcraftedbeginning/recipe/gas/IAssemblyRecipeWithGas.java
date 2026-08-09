package net.ty.createcraftedbeginning.recipe.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IAssemblyRecipeWithGas {
    @OnlyIn(Dist.CLIENT)
    Component getDescriptionForAssembly();

    default boolean supportsAssembly() {
        return true;
    }
}
