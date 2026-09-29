package net.ty.createcraftedbeginning.compat.jei.category;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.CCBJEITextures;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedAirtightReactorKettle;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.simibubi.create.compat.jei.category.CreateRecipeCategory.addFluidSlot;
import static com.simibubi.create.compat.jei.category.CreateRecipeCategory.addStochasticTooltip;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorKettleCategory extends CCBRecipeCategory<ReactorKettleRecipe> {
    protected final AnimatedAirtightReactorKettle reactorKettleOpened = new AnimatedAirtightReactorKettle(false);
    protected final AnimatedAirtightReactorKettle reactorKettleClosed = new AnimatedAirtightReactorKettle(true);

    public ReactorKettleCategory(Info<ReactorKettleRecipe> info) {
        super(info);
    }

    private static int getInputX(int inputIndex, int xOffset) {
        return 14 + xOffset + inputIndex % 3 * 19;
    }

    private static int getInputY(int inputIndex) {
        return 59 - inputIndex / 3 * 19;
    }

    private static int getOutputX(int outputIndex, int outputCount) {
        if (!Mth.isMultipleOf(outputCount, 2) && outputIndex == outputCount - 1) {
            return 142;
        }

        if (!Mth.isMultipleOf(outputIndex, 2)) {
            return 151;
        }

        return 132;
    }

    private static int getOutputY(int outputIndex) {
        return -19 * (outputIndex / 2) + 59;
    }

    private static Component getTemperatureDisplay(ReactorKettleRecipe recipe) {
        TemperatureCondition condition = recipe.getTemperatureCondition();
        if (recipe.getTemperatureMatching() != TemperatureMatching.COMPATIBLE) {
            return CCBLang.translateDirect(condition.getTranslationKey());
        }

        return switch (condition) {
            case NONE -> CCBLang.translateDirect("recipe.temperature_matching.compatible.none");
            case HEATED -> CCBLang.translateDirect("recipe.temperature_matching.compatible.heated");
            case CHILLED -> CCBLang.translateDirect("recipe.temperature_matching.compatible.chilled");
            default -> CCBLang.translateDirect(condition.getTranslationKey());
        };
    }

    private static @Unmodifiable List<Pair<Ingredient, Integer>> collectCondensedIngredients(NonNullList<Ingredient> recipeIngredients) {
        Map<Ingredient, Integer> ingredientCounts = new LinkedHashMap<>();
        for (Ingredient ingredient : recipeIngredients) {
            if (ingredient.isEmpty()) {
                continue;
            }

            boolean foundMatch = false;
            for (Entry<Ingredient, Integer> countEntry : ingredientCounts.entrySet()) {
                Ingredient existingIngredient = countEntry.getKey();
                if (!existingIngredient.equals(ingredient)) {
                    continue;
                }

                ingredientCounts.put(existingIngredient, countEntry.getValue() + 1);
                foundMatch = true;
                break;
            }

            if (foundMatch) {
                continue;
            }

            ingredientCounts.put(ingredient, 1);
        }
        return ingredientCounts.entrySet().stream().map(countEntry -> Pair.of(countEntry.getKey(), countEntry.getValue())).toList();
    }

    @Override
    protected List<Component> getTooltipStrings(ReactorKettleRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX < 4 || mouseX >= 129 || mouseY < 80 || mouseY >= 99) {
            return List.of();
        }

        TemperatureMatching matching = recipe.getTemperatureMatching();
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(CCBLang.translateDirect("recipe.temperature_matching." + matching.getSerializedName()));
        if (matching == TemperatureMatching.EXACT) {
            tooltip.add(CCBLang.translateDirect("recipe.temperature_matching.exact.description").withStyle(ChatFormatting.GRAY));
            return tooltip;
        }

        TemperatureCondition condition = recipe.getTemperatureCondition();
        if (!condition.supportsCompatibleMatching()) {
            return tooltip;
        }

        if (condition == TemperatureCondition.NONE) {
            tooltip.add(CCBLang.translateDirect("recipe.temperature_matching.compatible.none.description").withStyle(ChatFormatting.GRAY));
        }
        else {
            tooltip.add(CCBLang.translateDirect("recipe.temperature_matching.compatible.description").withStyle(ChatFormatting.GRAY));
        }
        return tooltip;
    }

    @Override
    protected void draw(ReactorKettleRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        int outputCount = recipe.getFluidResults().size() + recipe.getGasResults().size() + recipe.getRollableResults().size();
        if (outputCount <= 3) {
            CCBJEITextures.JEI_DOWN_ARROW.render(graphics, 136, -19 * ((1 + outputCount) / 2 - 1) + 32);
        }
        CCBJEITextures.JEI_SHADOW.render(graphics, 76, 66);

        TemperatureCondition condition = recipe.getTemperatureCondition();
        int color = condition.getColor();
        CCBJEITextures.JEI_HEAT_BAR.render(graphics, 4, 80, new Color(color));
        graphics.drawString(Minecraft.getInstance().font, getTemperatureDisplay(recipe), 9, 86, color, false);
        if (recipe.getGasRequirements().isEmpty() && recipe.getGasResults().isEmpty()) {
            reactorKettleOpened.draw(graphics, background.getWidth() / 2 + 6, 58);
            return;
        }

        reactorKettleClosed.draw(graphics, background.getWidth() / 2 + 6, 58);
    }

    @Override
    protected void setRecipe(IRecipeLayoutBuilder builder, ReactorKettleRecipe recipe, IFocusGroup focuses) {
        List<Pair<Ingredient, Integer>> condensedIngredients = collectCondensedIngredients(recipe.getIngredients());
        NonNullList<SizedFluidIngredient> fluidIngredients = recipe.getFluidIngredients();
        List<GasRecipeRequirement> gasRequirements = recipe.getGasRequirements();
        List<ProcessingOutput> results = recipe.getRollableResults();
        NonNullList<FluidStack> fluidResults = recipe.getFluidResults();
        NonNullList<GasStack> gasResults = recipe.getGasResults();
        int inputCount = condensedIngredients.size() + fluidIngredients.size() + gasRequirements.size();
        int xOffset = inputCount < 3 ? (3 - inputCount) * 19 / 2 : 0;
        int inputIndex = 0;
        for (Pair<Ingredient, Integer> pair : condensedIngredients) {
            List<ItemStack> itemStacks = new ArrayList<>();
            for (ItemStack itemStack : pair.getFirst().getItems()) {
                itemStacks.add(itemStack.copyWithCount(pair.getSecond()));
            }
            int slotX = getInputX(inputIndex, xOffset);
            int slotY = getInputY(inputIndex);
            builder.addSlot(RecipeIngredientRole.INPUT, slotX, slotY).setBackground(getRenderedSlot(), -1, -1).addItemStacks(itemStacks);
            inputIndex++;
        }
        for (SizedFluidIngredient fluidIngredient : fluidIngredients) {
            int slotX = getInputX(inputIndex, xOffset);
            int slotY = getInputY(inputIndex);
            addFluidSlot(builder, slotX, slotY, fluidIngredient);
            inputIndex++;
        }
        for (GasRecipeRequirement gasRequirement : gasRequirements) {
            int slotX = getInputX(inputIndex, xOffset);
            int slotY = getInputY(inputIndex);
            List<GasStack> gasStacks = Arrays.stream(gasRequirement.getGases()).map(GasStack::copy).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, slotX, slotY).setBackground(getRenderedSlot(), -1, -1).addIngredients(CCBJEIPlugin.GAS_STACK, gasStacks).addRichTooltipCallback((view, tooltip) -> addGasRequirementTooltip(tooltip, gasRequirement));
            inputIndex++;
        }

        int outputCount = results.size() + fluidResults.size() + gasResults.size();
        int outputIndex = 0;
        for (ProcessingOutput itemResult : results) {
            int slotX = getOutputX(outputIndex, outputCount);
            int slotY = getOutputY(outputIndex);
            builder.addSlot(RecipeIngredientRole.OUTPUT, slotX, slotY).setBackground(getRenderedSlot(itemResult), -1, -1).addItemStack(itemResult.getStack()).addRichTooltipCallback(addStochasticTooltip(itemResult));
            outputIndex++;
        }
        for (FluidStack fluidResult : fluidResults) {
            int slotX = getOutputX(outputIndex, outputCount);
            int slotY = getOutputY(outputIndex);
            addFluidSlot(builder, slotX, slotY, fluidResult);
            outputIndex++;
        }
        for (GasStack gasResult : gasResults) {
            int slotX = getOutputX(outputIndex, outputCount);
            int slotY = getOutputY(outputIndex);
            builder.addSlot(RecipeIngredientRole.OUTPUT, slotX, slotY).setBackground(getRenderedSlot(), -1, -1).addIngredient(CCBJEIPlugin.GAS_STACK, gasResult.copy()).addRichTooltipCallback((view, tooltip) -> tooltip.add(GasUnitFormat.amount(gasResult.getAmount()).style(ChatFormatting.GRAY).component()));
            outputIndex++;
        }

        TemperatureCondition condition = recipe.getTemperatureCondition();
        if (condition == TemperatureCondition.NONE) {
            return;
        }

        switch (condition) {
            case CHILLED -> builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81).addItemStack(new ItemStack(CCBBlocks.BREEZE_COOLER_BLOCK));
            case SUPERCHILLED -> builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81).addItemStack(new ItemStack(CCBBlocks.BREEZE_COOLER_BLOCK, 3));
            case HEATED -> builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81).addItemStack(new ItemStack(AllBlocks.BLAZE_BURNER));
            case SUPERHEATED -> {
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81).addItemStack(new ItemStack(AllBlocks.BLAZE_BURNER));
                builder.addSlot(RecipeIngredientRole.CATALYST, 153, 81).addItemStack(new ItemStack(AllItems.BLAZE_CAKE.asItem()));
            }
        }
    }
}
