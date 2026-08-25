package net.ty.createcraftedbeginning.compat.jei.category;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedAirtightForgingPress;
import net.ty.createcraftedbeginning.compat.jei.CCBJEITextures;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
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
    private static final int BASE_INDEX = 0;
    private static final int TEMPLATE_INDEX = 1;
    private static final int ADDITION_INDEX = 2;

    protected final AnimatedAirtightForgingPress forgingPress = new AnimatedAirtightForgingPress();

    public ForgingPressCategory(Info<ForgingPressRecipe> info) {
        super(info);
    }

    private static int getInputX(int inputIndex) {
        return 42 - inputIndex * 19;
    }

    private static int getOutputX(int size) {
        return 144 - (size == 1 ? 0 : 10);
    }

    private static Optional<IRecipeSlotDrawable> findSlot(List<IRecipeSlotDrawable> slots, String name) {
        return slots.stream().filter(slot -> slot.getSlotName().filter(name::equals).isPresent()).findFirst();
    }

    private static List<ItemStack> getSmithingLookupOutputs(ForgingPressRecipe recipe, SmithingRecipe smithingRecipe, Level level) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.size() <= ADDITION_INDEX) {
            return List.of();
        }

        List<ItemStack> templates = Arrays.asList(ingredients.get(TEMPLATE_INDEX).getItems());
        if (templates.isEmpty()) {
            templates = List.of(ItemStack.EMPTY);
        }

        List<ItemStack> bases = Arrays.asList(ingredients.get(BASE_INDEX).getItems());
        if (bases.isEmpty()) {
            bases = List.of(ItemStack.EMPTY);
        }

        ItemStack[] additions = ingredients.get(ADDITION_INDEX).getItems();
        ItemStack addition = additions.length == 0 ? ItemStack.EMPTY : additions[0];
        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack template : templates) {
            for (ItemStack base : bases) {
                SmithingRecipeInput smithingInput = new SmithingRecipeInput(template.copyWithCount(1), base.copyWithCount(1), addition.copyWithCount(1));
                ItemStack smithingResult = smithingRecipe.assemble(smithingInput, level.registryAccess());
                if (smithingResult.isEmpty()) {
                    continue;
                }

                ItemStack lookupResult = smithingResult.copyWithCount(1);
                if (outputs.stream().noneMatch(existing -> ItemStack.isSameItemSameComponents(existing, lookupResult))) {
                    outputs.add(lookupResult);
                }
            }
        }
        return outputs;
    }

    @Override
    protected void onDisplayedIngredientsUpdate(ForgingPressRecipe recipe, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
        SmithingRecipe smithingRecipe = recipe.getSmithingRecipe();
        if (smithingRecipe == null) {
            return;
        }

        Level clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
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
        Optional<ItemStack> addition = additionSlot.get().getDisplayedItemStack();
        if (template.isEmpty() || addition.isEmpty()) {
            return;
        }

        ItemStack base;
        boolean hasOutputFocus = focuses.getFocuses(RecipeIngredientRole.OUTPUT).findAny().isPresent();
        if (smithingRecipe instanceof SmithingTrimRecipe && hasOutputFocus) {
            Optional<ItemStack> displayedOutput = outputSlot.get().getDisplayedItemStack();
            if (displayedOutput.isEmpty()) {
                return;
            }

            base = new ItemStack(displayedOutput.get().getItem());
            baseSlot.get().createDisplayOverrides().addItemStack(base.copy());
        }
        else {
            Optional<ItemStack> displayedBase = baseSlot.get().getDisplayedItemStack();
            if (displayedBase.isEmpty()) {
                return;
            }
            base = displayedBase.get().copyWithCount(1);
        }

        SmithingRecipeInput smithingInput = new SmithingRecipeInput(template.get().copyWithCount(1), base, addition.get().copyWithCount(1));
        if (!smithingRecipe.matches(smithingInput, clientLevel)) {
            return;
        }

        ItemStack smithingResult = smithingRecipe.assemble(smithingInput, clientLevel.registryAccess());
        if (smithingResult.isEmpty()) {
            return;
        }

        outputSlot.get().createDisplayOverrides().addItemStack(smithingResult.copyWithCount(1));
    }

    @Override
    protected void draw(ForgingPressRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        NonNullList<SizedFluidIngredient> fluidIngredients = recipe.getFluidIngredients();
        NonNullList<SizedGasIngredient> gasIngredients = recipe.getGasIngredients();
        int inputCount = recipe.getIngredients().size() + fluidIngredients.size() + gasIngredients.size();
        if (inputCount > 1) {
            CCBJEITextures.JEI_PRESS_HEAD_TOOL.render(graphics, 24, 43);
        }
        if (inputCount > 2) {
            CCBJEITextures.JEI_DOWN_ARROW.render(graphics, 73, 10);
        }
        CCBJEITextures.JEI_SHADOW.render(graphics, 66, 66);
        CCBJEITextures.JEI_LONG_ARROW.render(graphics, background.getWidth() / 2 - 35, 86);
        forgingPress.draw(graphics, background.getWidth() / 2 - 8, 58);
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
            List<GasStack> gasStacks = Arrays.stream(gasIngredient.getGases()).map(GasStack::copy).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, getInputX(inputIndex), 6).setBackground(getRenderedSlot(), -1, -1).addIngredients(CCBJEIPlugin.GAS_STACK, gasStacks).addRichTooltipCallback((view, tooltip) -> tooltip.add(GasAmounts.precise(gasIngredient.amount()).style(ChatFormatting.GRAY).component()));
        }
        SmithingRecipe smithingRecipe = recipe.getSmithingRecipe();
        if (smithingRecipe != null) {
            IRecipeSlotBuilder outputSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, getOutputX(1), 82).setSlotName(SLOT_OUTPUT).setBackground(BASIC_SLOT, -1, -1);
            Level level = Minecraft.getInstance().level;
            if (level != null) {
                List<ItemStack> lookupOutputs = getSmithingLookupOutputs(recipe, smithingRecipe, level);
                if (!lookupOutputs.isEmpty()) {
                    outputSlot.addItemStacks(lookupOutputs);
                }
            }
            return;
        }

        for (ProcessingOutput output : results) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, getOutputX(results.size()), 82).setBackground(getRenderedSlot(output), -1, -1).addItemStack(output.getStack()).addRichTooltipCallback(addStochasticTooltip(output));
        }
    }
}
