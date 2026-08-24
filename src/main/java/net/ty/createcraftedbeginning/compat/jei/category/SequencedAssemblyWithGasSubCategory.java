package net.ty.createcraftedbeginning.compat.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedDeployer;
import com.simibubi.create.compat.jei.category.animations.AnimatedPress;
import com.simibubi.create.compat.jei.category.animations.AnimatedSaw;
import com.simibubi.create.compat.jei.category.animations.AnimatedSpout;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedGasInjectionChamber;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.recipe.SequencedWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.DeployerApplicationWithGasRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

import static net.ty.createcraftedbeginning.compat.jei.category.CCBRecipeCategory.getRenderedSlot;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class SequencedAssemblyWithGasSubCategory {
    protected final int width;

    protected SequencedAssemblyWithGasSubCategory(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public void setRecipe(IRecipeLayoutBuilder builder, SequencedWithGasRecipe<?> recipe, IFocusGroup focuses, int slotX) {
    }

    public abstract void draw(SequencedWithGasRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int stepIndex);

    public static class AssemblyPressing extends SequencedAssemblyWithGasSubCategory {
        private final AnimatedPress press;

        public AssemblyPressing() {
            super(25);
            press = new AnimatedPress(false);
        }

        @Override
        public void draw(SequencedWithGasRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int stepIndex) {
            PoseStack poseStack = graphics.pose();
            press.offset = stepIndex;
            poseStack.pushPose();
            poseStack.translate(-5, 50, 0);
            poseStack.scale(0.6f, 0.6f, 0.6f);
            press.draw(graphics, getWidth() / 2, 0);
            poseStack.popPose();
        }
    }

    public static class AssemblySpouting extends SequencedAssemblyWithGasSubCategory {
        private final AnimatedSpout spout;

        public AssemblySpouting() {
            super(25);
            spout = new AnimatedSpout();
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, SequencedWithGasRecipe<?> recipe, IFocusGroup focuses, int slotX) {
            CreateRecipeCategory.addFluidSlot(builder, slotX + 4, 15, recipe.getRecipe().getFluidIngredients().getFirst());
        }

        @Override
        public void draw(SequencedWithGasRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int stepIndex) {
            PoseStack poseStack = graphics.pose();
            spout.offset = stepIndex;
            poseStack.pushPose();
            poseStack.translate(-7, 50, 0);
            poseStack.scale(0.75f, 0.75f, 0.75f);
            List<FluidStack> fluidStacks = Arrays.asList(recipe.getRecipe().getFluidIngredients().getFirst().getFluids());
            spout.withFluids(fluidStacks).draw(graphics, getWidth() / 2, 0);
            poseStack.popPose();
        }
    }

    public static class AssemblyDeploying extends SequencedAssemblyWithGasSubCategory {
        private final AnimatedDeployer deployer;

        public AssemblyDeploying() {
            super(25);
            deployer = new AnimatedDeployer();
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, SequencedWithGasRecipe<?> recipe, IFocusGroup focuses, int slotX) {
            DeployerApplicationWithGasRecipe deployerRecipe = (DeployerApplicationWithGasRecipe) recipe.getRecipe();
            IRecipeSlotBuilder ingredientSlot = builder.addSlot(RecipeIngredientRole.INPUT, slotX + 4, 15).setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1).addIngredients(deployerRecipe.getRequiredHeldItem());
            if (!deployerRecipe.shouldKeepHeldItem()) {
                return;
            }

            ingredientSlot.addRichTooltipCallback((view, tooltip) -> tooltip.add(CCBLang.translate("recipe.deploying.not_consumed").style(ChatFormatting.GOLD).component()));
        }

        @Override
        public void draw(SequencedWithGasRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int stepIndex) {
            PoseStack poseStack = graphics.pose();
            deployer.offset = stepIndex;
            poseStack.pushPose();
            poseStack.translate(-7, 50, 0);
            poseStack.scale(0.75f, 0.75f, 0.75f);
            deployer.draw(graphics, getWidth() / 2, 0);
            poseStack.popPose();
        }
    }

    public static class AssemblyInjecting extends SequencedAssemblyWithGasSubCategory {
        private final AnimatedGasInjectionChamber chamber;

        public AssemblyInjecting() {
            super(25);
            chamber = new AnimatedGasInjectionChamber(false);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, SequencedWithGasRecipe<?> recipe, IFocusGroup focuses, int slotX) {
            SizedGasIngredient gasIngredient = recipe.getRecipe().getGasIngredients().getFirst();
            List<GasStack> gasStacks = Arrays.stream(gasIngredient.getGases()).map(GasStack::copy).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, slotX + 4, 15).setBackground(getRenderedSlot(), -1, -1).addIngredients(CCBJEIPlugin.GAS_STACK, gasStacks).addRichTooltipCallback((view, tooltip) -> tooltip.add(GasAmounts.precise(gasIngredient.amount()).style(ChatFormatting.GRAY).component()));
        }

        @Override
        public void draw(SequencedWithGasRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int stepIndex) {
            PoseStack poseStack = graphics.pose();
            chamber.offset = stepIndex;
            poseStack.pushPose();
            poseStack.translate(-7, 50, 0);
            poseStack.scale(0.75f, 0.75f, 0.75f);
            chamber.draw(graphics, getWidth() / 2, 0);
            poseStack.popPose();
        }
    }

    public static class AssemblyCutting extends SequencedAssemblyWithGasSubCategory {
        private final AnimatedSaw saw;

        public AssemblyCutting() {
            super(25);
            saw = new AnimatedSaw();
        }

        @Override
        public void draw(SequencedWithGasRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int stepIndex) {
            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            poseStack.translate(0, 51.5, 0);
            poseStack.scale(0.6f, 0.6f, 0.6f);
            saw.draw(graphics, getWidth() / 2, 30);
            poseStack.popPose();
        }
    }
}
