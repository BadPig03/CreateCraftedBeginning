package net.ty.createcraftedbeginning.compat.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.CreateLang;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.SequencedAssemblyWithGasSubCategory.AssemblyCutting;
import net.ty.createcraftedbeginning.compat.jei.category.SequencedAssemblyWithGasSubCategory.AssemblyDeploying;
import net.ty.createcraftedbeginning.compat.jei.category.SequencedAssemblyWithGasSubCategory.AssemblyInjecting;
import net.ty.createcraftedbeginning.compat.jei.category.SequencedAssemblyWithGasSubCategory.AssemblyPressing;
import net.ty.createcraftedbeginning.compat.jei.category.SequencedAssemblyWithGasSubCategory.AssemblySpouting;
import net.ty.createcraftedbeginning.foundation.client.CCBGUITextures;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.CuttingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.DeployerApplicationWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.FillingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.PressingWithGasRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SequencedAssemblyWithGasCategory extends CCBRecipeCategory<SequencedAssemblyWithGasRecipe> {
    private static final String[] ROMANS = {"I", "II", "III", "IV", "V", "VI", "-"};
    private static final int STEP_MARGIN = 3;
    private static final int RANDOM_OUTPUT_OFFSET = -7;
    public Map<ResourceLocation, SequencedAssemblyWithGasSubCategory> subCategories = new HashMap<>();

    public SequencedAssemblyWithGasCategory(Info<SequencedAssemblyWithGasRecipe> info) {
        super(info);
    }

    private static SequencedAssemblyWithGasSubCategory createSubCategory(SequencedWithGasRecipe<?> step) {
        var recipe = step.getAsAssemblyRecipe();
        return switch (recipe) {
            case PressingWithGasRecipe ignored -> new AssemblyPressing();
            case FillingWithGasRecipe ignored -> new AssemblySpouting();
            case DeployerApplicationWithGasRecipe ignored -> new AssemblyDeploying();
            case GasInjectionRecipe ignored -> new AssemblyInjecting();
            case CuttingWithGasRecipe ignored -> new AssemblyCutting();
            default -> throw new IllegalArgumentException("Unsupported sequenced assembly recipe: " + recipe.getClass().getName());
        };
    }

    private static void addInvisibleInputs(IRecipeLayoutBuilder builder, SequencedWithGasRecipe<?> step) {
        var stepRecipe = step.getRecipe();
        NonNullList<Ingredient> ingredients = stepRecipe.getIngredients();
        ingredients.subList(1, ingredients.size()).forEach(ingredient -> builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addIngredients(ingredient));
        stepRecipe.getFluidIngredients().forEach(fluidIngredient -> builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addIngredients(NeoForgeTypes.FLUID_STACK, Arrays.asList(fluidIngredient.getFluids())));
        stepRecipe.getGasIngredients().forEach(gasIngredient -> builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addIngredients(CCBJEIPlugin.GAS_STACK, Arrays.asList(gasIngredient.getGases())));
    }

    @Override
    public List<Component> getTooltipStrings(SequencedAssemblyWithGasRecipe recipe, IRecipeSlotsView recipeSlots, double mouseX, double mouseY) {
        List<Component> tooltip = new ArrayList<>();
        boolean singleOutput = recipe.getOutputChance() == 1;
        boolean willRepeat = recipe.getLoops() > 1;
        int xOffset = RANDOM_OUTPUT_OFFSET;
        int minX = 150 + xOffset;
        int maxX = minX + 18;
        int minY = 90;
        int maxY = minY + 18;
        if (!singleOutput && mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY) {
            float chance = recipe.getOutputChance();
            tooltip.add(CreateLang.translateDirect("recipe.assembly.junk"));
            tooltip.add(chanceComponent(1 - chance));
            return tooltip;
        }

        minX = 55 + xOffset;
        maxX = minX + 65;
        minY = 92;
        maxY = minY + 24;
        if (willRepeat && mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY) {
            tooltip.add(CreateLang.translateDirect("recipe.assembly.repeat", recipe.getLoops()));
            return tooltip;
        }

        if (mouseY <= 5 || mouseY >= 84) {
            return tooltip;
        }

        int width = getSequenceWidth(recipe);
        xOffset = width / 2 + getBackground().getWidth() / -2;
        double relativeX = mouseX + xOffset;
        List<SequencedWithGasRecipe<?>> sequence = recipe.getSequence();
        for (int i = 0; i < sequence.size(); i++) {
            SequencedWithGasRecipe<?> step = sequence.get(i);
            SequencedAssemblyWithGasSubCategory subCategory = getSubCategory(step);
            if (relativeX >= 0 && relativeX < subCategory.getWidth()) {
                tooltip.add(CreateLang.translateDirect("recipe.assembly.step", i + 1));
                tooltip.add(step.getAsAssemblyRecipe().getDescriptionForAssembly().plainCopy().withStyle(ChatFormatting.DARK_GREEN));
                return tooltip;
            }

            relativeX -= subCategory.getWidth() + STEP_MARGIN;
        }

        return tooltip;
    }

    @Override
    public void draw(SequencedAssemblyWithGasRecipe recipe, IRecipeSlotsView recipeSlots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.pushPose();
        poseStack.translate(0, 15, 0);
        boolean singleOutput = recipe.getOutputChance() == 1;
        int xOffset = singleOutput ? 0 : RANDOM_OUTPUT_OFFSET;
        CCBGUITextures.JEI_LONG_ARROW.render(graphics, 52 + xOffset, 79);
        if (!singleOutput) {
            AllGuiTextures.JEI_CHANCE_SLOT.render(graphics, 150 + xOffset, 75);
            Component component = Component.literal("?").withStyle(ChatFormatting.BOLD);
            graphics.drawString(font, component, font.width(component) / -2 + 8 + 150 + xOffset, 80, 0xEFEFEF);
        }
        if (recipe.getLoops() > 1) {
            poseStack.pushPose();
            poseStack.translate(15, 9, 0);
            AllIcons.I_SEQ_REPEAT.render(graphics, 50 + xOffset, 75);
            graphics.drawString(font, Component.literal("x" + recipe.getLoops()), 66 + xOffset, 80, 0x888888, false);
            poseStack.popPose();
        }
        poseStack.popPose();
        int width = getSequenceWidth(recipe);
        poseStack.translate((float) width / -2 + (float) getBackground().getWidth() / 2, 0, 0);
        poseStack.pushPose();
        List<SequencedWithGasRecipe<?>> sequence = recipe.getSequence();
        for (int i = 0; i < sequence.size(); i++) {
            SequencedWithGasRecipe<?> sequencedRecipe = sequence.get(i);
            SequencedAssemblyWithGasSubCategory subCategory = getSubCategory(sequencedRecipe);
            int subWidth = subCategory.getWidth();
            MutableComponent component = Component.literal(ROMANS[Math.min(i, 6)]);
            graphics.drawString(font, component, font.width(component) / -2 + subWidth / 2, 2, 0x888888, false);
            subCategory.draw(sequencedRecipe, graphics, mouseX, mouseY, i);
            poseStack.translate(subWidth + STEP_MARGIN, 0, 0);
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SequencedAssemblyWithGasRecipe recipe, IFocusGroup focuses) {
        boolean noRandomOutput = recipe.getOutputChance() == 1;
        int xOffset = noRandomOutput ? 0 : RANDOM_OUTPUT_OFFSET;
        builder.addSlot(RecipeIngredientRole.INPUT, 27 + xOffset, 91).setBackground(getRenderedSlot(), -1, -1).addItemStacks(List.of(recipe.getIngredient().getItems()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 132 + xOffset, 91).setBackground(getRenderedSlot(recipe.getOutputChance()), -1, -1).addItemStack(getResultItem(recipe)).addRichTooltipCallback((view, tooltip) -> {
            if (noRandomOutput) {
                return;
            }

            tooltip.add(chanceComponent(recipe.getOutputChance()));
        });

        int width = getSequenceWidth(recipe);
        int x = width / -2 + getBackground().getWidth() / 2;
        for (SequencedWithGasRecipe<?> step : recipe.getSequence()) {
            SequencedAssemblyWithGasSubCategory subCategory = getSubCategory(step);
            subCategory.setRecipe(builder, step, focuses, x);
            x += subCategory.getWidth() + STEP_MARGIN;
        }
        for (int repeat = 1; repeat < recipe.getLoops(); repeat++) {
            for (SequencedWithGasRecipe<?> step : recipe.getSequence()) {
                addInvisibleInputs(builder, step);
            }
        }
    }

    private int getSequenceWidth(SequencedAssemblyWithGasRecipe recipe) {
        int width = 0;
        for (SequencedWithGasRecipe<?> step : recipe.getSequence()) {
            width += getSubCategory(step).getWidth() + STEP_MARGIN;
        }
        return width - STEP_MARGIN;
    }

    private SequencedAssemblyWithGasSubCategory getSubCategory(SequencedWithGasRecipe<?> step) {
        ResourceLocation serializerId = RegisteredObjectsHelper.getKeyOrThrow(step.getRecipe().getSerializer());
        return subCategories.computeIfAbsent(serializerId, ignored -> createSubCategory(step));
    }

    protected MutableComponent chanceComponent(float chance) {
        String number;
        if (chance < 0.01) {
            number = "<1";
        }
        else if (chance > 0.99) {
            number = ">99";
        }
        else {
            number = String.valueOf(Math.round(chance * 100));
        }
        return CCBLang.translate("recipe.processing.chance", number).style(ChatFormatting.GOLD).component();
    }
}
