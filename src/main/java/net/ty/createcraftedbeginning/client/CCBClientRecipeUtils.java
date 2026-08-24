package net.ty.createcraftedbeginning.client;

import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
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

    public static RecipeHolder<ReactorKettleRecipe> convertToReactorKettleRecipe(RecipeHolder<?> sourceRecipe) {
        ReactorKettleRecipe.Builder<ReactorKettleRecipe> recipeBuilder = new ReactorKettleRecipe.Builder<>(ReactorKettleRecipe::new, sourceRecipe.id());
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return new RecipeHolder<>(sourceRecipe.id(), recipeBuilder.build());
        }

        ReactorKettleRecipe convertedRecipe = recipeBuilder.withItemIngredients(sourceRecipe.value().getIngredients()).withSingleItemOutput(sourceRecipe.value().getResultItem(level.registryAccess())).build();
        return new RecipeHolder<>(sourceRecipe.id(), convertedRecipe);
    }

    public static void addSequencedAssemblyTooltip(ItemTooltipEvent event) {
        SequencedAssemblyWithGas assemblyData = event.getItemStack().get(CCBRecipeDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS);
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

        int sequenceLength = recipe.sequence.size();
        int currentStep = assemblyData.step();
        int totalSteps = sequenceLength * recipe.loops;
        List<Component> tooltip = event.getToolTip();
        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CreateLang.translateDirect("recipe.sequenced_assembly").withStyle(ChatFormatting.GRAY));
        tooltip.add(CreateLang.translateDirect("recipe.assembly.progress", currentStep, totalSteps).withStyle(ChatFormatting.DARK_GRAY));
        int remainingSteps = totalSteps - currentStep;
        for (int stepOffset = 0; stepOffset < sequenceLength && stepOffset < remainingSteps; stepOffset++) {
            Component stepDescription = recipe.sequence.get((stepOffset + currentStep) % sequenceLength).getAsAssemblyRecipe().getDescriptionForAssembly();
            Component tooltipLine = stepOffset == 0 ? CreateLang.translateDirect("recipe.assembly.next", stepDescription).withStyle(ChatFormatting.AQUA) : Component.literal("-> ").append(stepDescription).withStyle(ChatFormatting.DARK_AQUA);
            tooltip.add(tooltipLine);
        }
    }
}
