package net.ty.createcraftedbeginning.api.gas.recipes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedDeployer;
import com.simibubi.create.compat.jei.category.animations.AnimatedPress;
import com.simibubi.create.compat.jei.category.animations.AnimatedSaw;
import com.simibubi.create.compat.jei.category.animations.AnimatedSpout;
import com.simibubi.create.foundation.utility.CreateLang;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.SizedGasIngredient;
import net.ty.createcraftedbeginning.compat.jei.JEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedGasInjectionChamber;
import net.ty.createcraftedbeginning.recipe.SequencedWithGasRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.ty.createcraftedbeginning.compat.jei.category.CCBRecipeCategory.getRenderedSlot;

public abstract class SequencedAssemblyWithGasSubCategory {
    private final int width;

    public SequencedAssemblyWithGasSubCategory(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public void setRecipe(IRecipeLayoutBuilder builder, SequencedWithGasRecipe<?> recipe, IFocusGroup focuses, int x) {
    }

    public abstract void draw(SequencedWithGasRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index);

    public static class AssemblyPressing extends SequencedAssemblyWithGasSubCategory {
        private final AnimatedPress press;

        public AssemblyPressing() {
            super(25);
            press = new AnimatedPress(false);
        }

        @Override
        public void draw(SequencedWithGasRecipe<?> recipe, @NotNull GuiGraphics graphics, double mouseX, double mouseY, int index) {
            PoseStack ms = graphics.pose();
            press.offset = index;
            ms.pushPose();
            ms.translate(-5, 50, 0);
            ms.scale(0.6f, 0.6f, 0.6f);
            press.draw(graphics, getWidth() / 2, 0);
            ms.popPose();
        }
    }

    public static class AssemblySpouting extends SequencedAssemblyWithGasSubCategory {
        private final AnimatedSpout spout;

        public AssemblySpouting() {
            super(25);
            spout = new AnimatedSpout();
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, @NotNull SequencedWithGasRecipe<?> recipe, IFocusGroup focuses, int x) {
            SizedFluidIngredient fluidIngredient = recipe.getRecipe().getFluidIngredients().getFirst();
            CreateRecipeCategory.addFluidSlot(builder, x + 4, 15, fluidIngredient);
        }

        @Override
        public void draw(@NotNull SequencedWithGasRecipe<?> recipe, @NotNull GuiGraphics graphics, double mouseX, double mouseY, int index) {
            PoseStack ms = graphics.pose();
            spout.offset = index;
            ms.pushPose();
            ms.translate(-7, 50, 0);
            ms.scale(0.75f, 0.75f, 0.75f);
            spout.withFluids(Arrays.asList(recipe.getRecipe().getFluidIngredients().getFirst().getFluids())).draw(graphics, getWidth() / 2, 0);
            ms.popPose();
        }
    }

    public static class AssemblyDeploying extends SequencedAssemblyWithGasSubCategory {
        private final AnimatedDeployer deployer;

        public AssemblyDeploying() {
            super(25);
            deployer = new AnimatedDeployer();
        }

        @Override
        public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull SequencedWithGasRecipe<?> recipe, IFocusGroup focuses, int x) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, x + 4, 15).setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1).addIngredients(recipe.getRecipe().getIngredients().get(1));
            if (!(recipe.getAsAssemblyRecipe() instanceof DeployerApplicationWithGasRecipe deployerRecipe) || !deployerRecipe.shouldKeepHeldItem()) {
                return;
            }

            slot.addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(CreateLang.translateDirect("recipe.deploying.not_consumed").withStyle(ChatFormatting.GOLD)));
        }

        @Override
        public void draw(SequencedWithGasRecipe<?> recipe, @NotNull GuiGraphics graphics, double mouseX, double mouseY, int index) {
            PoseStack ms = graphics.pose();
            deployer.offset = index;
            ms.pushPose();
            ms.translate(-7, 50, 0);
            ms.scale(0.75f, 0.75f, 0.75f);
            deployer.draw(graphics, getWidth() / 2, 0);
            ms.popPose();
        }
    }

    public static class AssemblyInjecting extends SequencedAssemblyWithGasSubCategory {
        private final AnimatedGasInjectionChamber chamber;

        public AssemblyInjecting() {
            super(25);
            chamber = new AnimatedGasInjectionChamber();
        }

        @Override
        public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull SequencedWithGasRecipe<?> recipe, IFocusGroup focuses, int x) {
            SizedGasIngredient gasIngredient = recipe.getRecipe().getGasIngredients().getFirst();
            List<GasStack> gasStackList = Arrays.asList(gasIngredient.getGases());
            List<GasStack> fullStacks = new ArrayList<>();
            for (GasStack stack : gasStackList) {
                fullStacks.add(stack.copyWithAmount(FluidType.BUCKET_VOLUME));
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, x + 4, 15).setBackground(getRenderedSlot(), -1, -1).addIngredients(JEIPlugin.GAS_STACK, fullStacks).setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16).addRichTooltipCallback((recipeSlotView, tooltip) -> gasStackList.stream().map(stack -> Component.translatable("jei.tooltip.gas.amount", stack.getAmount()).withStyle(ChatFormatting.GRAY)).forEach(tooltip::add));
        }

        @Override
        public void draw(SequencedWithGasRecipe<?> recipe, @NotNull GuiGraphics graphics, double mouseX, double mouseY, int index) {
            PoseStack ms = graphics.pose();
            chamber.offset = index;
            ms.pushPose();
            ms.translate(-7, 50, 0);
            ms.scale(0.75f, 0.75f, 0.75f);
            chamber.draw(graphics, getWidth() / 2, 0);
            ms.popPose();
        }
    }

    public static class AssemblyCutting extends SequencedAssemblyWithGasSubCategory {
        private final AnimatedSaw saw;

        public AssemblyCutting() {
            super(25);
            saw = new AnimatedSaw();
        }

        @Override
        public void draw(SequencedWithGasRecipe<?> recipe, @NotNull GuiGraphics graphics, double mouseX, double mouseY, int index) {
            PoseStack ms = graphics.pose();
            ms.pushPose();
            ms.translate(0, 51.5f, 0);
            ms.scale(0.6f, 0.6f, 0.6f);
            saw.draw(graphics, getWidth() / 2, 30);
            ms.popPose();
        }
    }
}
