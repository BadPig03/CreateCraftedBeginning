package net.ty.createcraftedbeginning.compat.jei.category;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedAirtightForgingPress;
import net.ty.createcraftedbeginning.client.CCBGUITextures;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.simibubi.create.compat.jei.category.CreateRecipeCategory.addFluidSlot;
import static com.simibubi.create.compat.jei.category.CreateRecipeCategory.addStochasticTooltip;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ForgingPressCategory extends CCBRecipeCategory<ForgingPressRecipe> {
    private static final String SLOT_BASE = "base";
    private static final String SLOT_TEMPLATE = "template";
    private static final String SLOT_ADDITION = "addition";
    private static final String SLOT_OUTPUT = "output";

    private final AnimatedAirtightForgingPress forgingPress = new AnimatedAirtightForgingPress();

    public ForgingPressCategory(Info<ForgingPressRecipe> info) {
        super(info);
    }

    private static int getInputX(int index) {
        return 42 - index * 19;
    }

    private static int getOutputX(int size) {
        return 144 - (size == 1 ? 0 : 10);
    }

    private static Optional<IRecipeSlotDrawable> findSlot(List<IRecipeSlotDrawable> slots, String name) {
        return slots.stream().filter(slot -> slot.getSlotName().filter(name::equals).isPresent()).findFirst();
    }

    @Override
    protected void onDisplayedIngredientsUpdate(ForgingPressRecipe recipe, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
        SmithingRecipe smithingRecipe = recipe.getSmithingRecipe();
        if (smithingRecipe == null) {
            return;
        }

        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        Optional<IRecipeSlotDrawable> templateSlot = findSlot(recipeSlots, SLOT_TEMPLATE);
        Optional<IRecipeSlotDrawable> baseSlot = findSlot(recipeSlots, SLOT_BASE);
        Optional<IRecipeSlotDrawable> additionSlot = findSlot(recipeSlots, SLOT_ADDITION);
        Optional<IRecipeSlotDrawable> outputSlot = findSlot(recipeSlots, SLOT_OUTPUT);
        if (templateSlot.isEmpty() || baseSlot.isEmpty() || additionSlot.isEmpty() || outputSlot.isEmpty()) {
            return;
        }

        Optional<ItemStack> template = templateSlot.get().getDisplayedItemStack();
        Optional<ItemStack> base = baseSlot.get().getDisplayedItemStack();
        Optional<ItemStack> addition = additionSlot.get().getDisplayedItemStack();
        if (template.isEmpty() || base.isEmpty() || addition.isEmpty()) {
            return;
        }

        SmithingRecipeInput input = new SmithingRecipeInput(template.get().copyWithCount(1), base.get().copyWithCount(1), addition.get().copyWithCount(1));
        if (!smithingRecipe.matches(input, level)) {
            return;
        }

        ItemStack result = smithingRecipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) {
            return;
        }

        outputSlot.get().createDisplayOverrides().addItemStack(result.copyWithCount(1));
    }

    @Override
    protected void draw(ForgingPressRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        NonNullList<SizedFluidIngredient> fluidIngredients = recipe.getFluidIngredients();
        NonNullList<SizedGasIngredient> gasIngredients = recipe.getGasIngredients();
        int inputCount = recipe.getIngredients().size() + fluidIngredients.size() + gasIngredients.size();
        if (inputCount > 1) {
            CCBGUITextures.JEI_PRESS_HEAD_TOOL.render(graphics, 24, 43);
        }
        if (inputCount > 2) {
            CCBGUITextures.JEI_DOWN_ARROW.render(graphics, 73, 10);
        }
        CCBGUITextures.JEI_SHADOW.render(graphics, 66, 66);
        CCBGUITextures.JEI_LONG_ARROW.render(graphics, getBackground().getWidth() / 2 - 35, 86);
        forgingPress.draw(graphics, getBackground().getWidth() / 2 - 8, 58);
    }

    @Override
    protected void setRecipe(IRecipeLayoutBuilder builder, ForgingPressRecipe recipe, IFocusGroup focuses) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        NonNullList<SizedFluidIngredient> fluidIngredients = recipe.getFluidIngredients();
        NonNullList<SizedGasIngredient> gasIngredients = recipe.getGasIngredients();
        List<ProcessingOutput> results = recipe.getRollableResults();
        if (ingredients.isEmpty()) {
            return;
        }

        int itemCount = ingredients.size();
        int inputIndex = 0;
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 82).setSlotName(SLOT_BASE).setBackground(getRenderedSlot(), -1, -1).addItemStacks(List.of(ingredients.getFirst().getItems()));
        if (itemCount > 1) {
            builder.addSlot(RecipeIngredientRole.INPUT, 42, 45).setSlotName(SLOT_TEMPLATE).setBackground(getRenderedSlot(), -1, -1).addItemStacks(List.of(ingredients.get(1).getItems()));
        }
        if (itemCount > 2) {
            builder.addSlot(RecipeIngredientRole.INPUT, getInputX(inputIndex), 6).setSlotName(SLOT_ADDITION).setBackground(getRenderedSlot(), -1, -1).addItemStacks(List.of(ingredients.get(2).getItems()));
            inputIndex++;
        }
        if (!fluidIngredients.isEmpty()) {
            addFluidSlot(builder, getInputX(inputIndex), 6, fluidIngredients.getFirst());
            inputIndex++;
        }
        if (!gasIngredients.isEmpty()) {
            SizedGasIngredient gasIngredient = gasIngredients.getFirst();
            List<GasStack> gases = Arrays.stream(gasIngredient.getGases()).map(GasStack::copy).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, getInputX(inputIndex), 6).setBackground(getRenderedSlot(), -1, -1).addIngredients(CCBJEIPlugin.GAS_STACK, gases).addRichTooltipCallback((view, tooltip) -> tooltip.add(GasAmountUtils.precise(gasIngredient.amount()).style(ChatFormatting.GRAY).component()));
        }
        if (recipe.getSmithingRecipe() != null) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, getOutputX(1), 82).setSlotName(SLOT_OUTPUT).setBackground(BASIC_SLOT, -1, -1).addItemStack(ItemStack.EMPTY);
            return;
        }

        for (ProcessingOutput result : results) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, getOutputX(results.size()), 82).setBackground(getRenderedSlot(result), -1, -1).addItemStack(result.getStack()).addRichTooltipCallback(addStochasticTooltip(result));
        }
    }
}
