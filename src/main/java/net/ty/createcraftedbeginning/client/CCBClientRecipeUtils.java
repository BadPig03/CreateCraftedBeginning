package net.ty.createcraftedbeginning.client;

import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.ty.createcraftedbeginning.recipe.CCBRecipeDataComponents;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe.SequencedAssemblyWithGas;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class CCBClientRecipeUtils {
    private CCBClientRecipeUtils() {
    }

    public static RecipeHolder<ReactorKettleRecipe> convertToReactorKettleRecipe(RecipeHolder<?> holder) {
        ReactorKettleRecipe.Builder<ReactorKettleRecipe> builder = new ReactorKettleRecipe.Builder<>(ReactorKettleRecipe::new, holder.id());
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return new RecipeHolder<>(holder.id(), builder.build());
        }

        ReactorKettleRecipe recipe = builder.withItemIngredients(holder.value().getIngredients()).withSingleItemOutput(holder.value().getResultItem(level.registryAccess())).build();
        return new RecipeHolder<>(holder.id(), recipe);
    }

    public static void addSequencedAssemblyTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        SequencedAssemblyWithGas assemblyData = stack.get(CCBRecipeDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS);
        if (assemblyData == null) {
            return;
        }

        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        Optional<RecipeHolder<?>> recipeHolder = level.getRecipeManager().byKey(assemblyData.id());
        if (recipeHolder.isEmpty() || !(recipeHolder.get().value() instanceof SequencedAssemblyWithGasRecipe recipe)) {
            return;
        }

        int length = recipe.sequence.size();
        int step = assemblyData.step();
        int total = length * recipe.loops;
        List<Component> tooltip = event.getToolTip();
        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CreateLang.translateDirect("recipe.sequenced_assembly").withStyle(ChatFormatting.GRAY));
        tooltip.add(CreateLang.translateDirect("recipe.assembly.progress", step, total).withStyle(ChatFormatting.DARK_GRAY));
        int remaining = total - step;
        for (int i = 0; i < length && i < remaining; i++) {
            Component description = recipe.sequence.get((i + step) % length).getAsAssemblyRecipe().getDescriptionForAssembly();
            Component line = i == 0 ? CreateLang.translateDirect("recipe.assembly.next", description).withStyle(ChatFormatting.AQUA) : Component.literal("-> ").append(description).withStyle(ChatFormatting.DARK_AQUA);
            tooltip.add(line);
        }
    }
}
