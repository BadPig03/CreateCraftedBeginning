package net.ty.createcraftedbeginning.mixin.common.accessor;

import com.simibubi.create.content.fluids.potion.PotionMixingRecipes;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = PotionMixingRecipes.class, remap = false)
public interface PotionMixingRecipesAccessor {
    @Invoker("createRecipesImpl")
    static List<RecipeHolder<MixingRecipe>> ccb$createRecipes(Level level) {
        throw new UnsupportedOperationException("The potion mixing recipe invoker must be transformed by Mixin.");
    }
}
