package net.ty.createcraftedbeginning.compat.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.CCBJEITextures;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedAirtightFractionationTower;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.recipe.FractionationTowerOutput;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static com.simibubi.create.compat.jei.category.CreateRecipeCategory.addFluidSlot;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FractionationTowerCategory extends CCBRecipeCategory<FractionationTowerRecipe> {
    private static final int VISIBLE_ROWS = 4;
    private static final int SLOT_SIZE = 18;

    private final AnimatedAirtightFractionationTower tower = new AnimatedAirtightFractionationTower();

    public FractionationTowerCategory(Info<FractionationTowerRecipe> info) {
        super(info);
    }

    @Override
    protected List<Component> getTooltipStrings(FractionationTowerRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        int temperatureY = getHeight() - 22;
        if (mouseX < 4 || mouseX >= 129 || mouseY < temperatureY || mouseY >= temperatureY + 19) {
            return List.of();
        }

        TemperatureMatching matching = recipe.getTemperatureMatching();
        String description = matching == TemperatureMatching.EXACT ? "recipe.temperature_matching.exact.description" : "recipe.temperature_matching.compatible.description";
        return List.of(CCBLang.translateDirect("recipe.temperature_matching." + matching.getSerializedName()), CCBLang.translateDirect(description).withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void draw(FractionationTowerRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        int height = getHeight();
        int temperatureY = height - 22;
        TemperatureCondition condition = recipe.getTemperatureCondition();
        int color = condition.getColor();
        CCBJEITextures.JEI_HEAT_BAR.render(graphics, 4, temperatureY, new Color(color));
        Font font = Minecraft.getInstance().font;
        Component temperature = CCBLang.translateDirect(condition.getTranslationKey());
        if (recipe.getTemperatureMatching() == TemperatureMatching.COMPATIBLE) {
            temperature = CCBLang.translateDirect("recipe.temperature_matching.compatible." + condition.getSerializedName());
        }
        graphics.drawString(font, temperature, 9, temperatureY + 6, color, false);
        int towerX = getWidth() / 2;
        int towerY = height - 25;
        tower.draw(graphics, towerX, towerY, recipe.getRequiredHeight(), towerY - 1);
    }

    @Override
    protected void setRecipe(IRecipeLayoutBuilder builder, FractionationTowerRecipe recipe, IFocusGroup focuses) {
        int temperatureY = getHeight() - 22;
        int outputX = getOutputX(recipe);
        List<Ingredient> itemIngredients = recipe.getIngredients();
        List<SizedFluidIngredient> fluidIngredients = recipe.getFluidIngredients();
        List<GasRecipeRequirement> gasRequirements = recipe.getGasRequirements();
        int inputCount = itemIngredients.size() + fluidIngredients.size() + gasRequirements.size();
        int inputRows = (inputCount + 1) / 2;
        int inputX = inputCount == 1 ? 5 + SLOT_SIZE : 5;
        int inputY = getColumnY(recipe, inputRows);
        Component inputLayer = CCBLang.translateDirect("jei.fractionation_tower.input_layer").withStyle(ChatFormatting.GRAY);
        int inputIndex = 0;
        for (Ingredient ingredient : itemIngredients) {
            int x = inputX + inputIndex % 2 * SLOT_SIZE;
            int y = inputY + inputIndex / 2 * SLOT_SIZE;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).setBackground(getRenderedSlot(), -1, -1).addIngredients(ingredient).addRichTooltipCallback((view, tooltip) -> tooltip.add(inputLayer));
            inputIndex++;
        }
        for (SizedFluidIngredient ingredient : fluidIngredients) {
            int x = inputX + inputIndex % 2 * SLOT_SIZE;
            int y = inputY + inputIndex / 2 * SLOT_SIZE;
            addFluidSlot(builder, x, y, ingredient).addRichTooltipCallback((view, tooltip) -> tooltip.add(inputLayer));
            inputIndex++;
        }
        for (GasRecipeRequirement requirement : gasRequirements) {
            int x = inputX + inputIndex % 2 * SLOT_SIZE;
            int y = inputY + inputIndex / 2 * SLOT_SIZE;
            List<GasStack> gases = Arrays.stream(requirement.getGases()).map(GasStack::copy).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).setBackground(getRenderedSlot(), -1, -1).addIngredients(CCBJEIPlugin.GAS_STACK, gases).addRichTooltipCallback((view, tooltip) -> {
                addGasRequirementTooltip(tooltip, requirement);
                tooltip.add(inputLayer);
            });
            inputIndex++;
        }

        Comparator<FractionationTowerOutput> order = Comparator.comparingInt(FractionationTowerOutput::layer);
        if (!recipe.isCondensation()) {
            order = order.reversed();
        }
        List<FractionationTowerOutput> outputs = recipe.getLayerOutputs().stream().sorted(order).toList();
        int outputY = getColumnY(recipe, outputs.size());
        for (int index = 0; index < outputs.size(); index++) {
            FractionationTowerOutput output = outputs.get(index);
            int y = outputY + index * SLOT_SIZE;
            ItemStack item = output.item();
            FluidStack fluid = output.fluid();
            GasStack gas = output.gas();
            IRecipeSlotBuilder slot;
            if (!item.isEmpty()) {
                slot = builder.addSlot(RecipeIngredientRole.OUTPUT, outputX, y).setBackground(getRenderedSlot(), -1, -1).addItemStack(item);
            }
            else if (!fluid.isEmpty()) {
                slot = addFluidSlot(builder, outputX, y, fluid);
            }
            else {
                slot = builder.addSlot(RecipeIngredientRole.OUTPUT, outputX, y).setBackground(getRenderedSlot(), -1, -1).addIngredient(CCBJEIPlugin.GAS_STACK, gas).addRichTooltipCallback((view, tooltip) -> tooltip.add(GasUnitFormat.amount(gas.getAmount()).style(ChatFormatting.GRAY).component()));
            }
            int outputLayer = output.layer();
            slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(CCBLang.translateDirect("jei.fractionation_tower.output_layer", outputLayer).withStyle(ChatFormatting.GRAY)));
        }

        switch (recipe.getTemperatureCondition()) {
            case CHILLED -> builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, temperatureY + 1).addItemStack(new ItemStack(CCBBlocks.BREEZE_COOLER_BLOCK));
            case SUPERCHILLED -> builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, temperatureY + 1).addItemStack(new ItemStack(CCBBlocks.BREEZE_COOLER_BLOCK, 3));
            case HEATED -> builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, temperatureY + 1).addItemStack(new ItemStack(AllBlocks.BLAZE_BURNER));
            case SUPERHEATED -> {
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, temperatureY + 1).addItemStack(new ItemStack(AllBlocks.BLAZE_BURNER));
                builder.addSlot(RecipeIngredientRole.CATALYST, 153, temperatureY + 1).addItemStack(new ItemStack(AllItems.BLAZE_CAKE.asItem()));
            }
            case NONE -> {
            }
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<FractionationTowerRecipe> holder, IFocusGroup focuses) {
        FractionationTowerRecipe recipe = holder.value();
        boolean condensation = recipe.isCondensation();
        IRecipeSlotDrawablesView recipeSlots = builder.getRecipeSlots();
        List<IRecipeSlotDrawable> inputs = recipeSlots.getSlots(RecipeIngredientRole.INPUT);
        if (inputs.size() > 2 * VISIBLE_ROWS) {
            int inputY = getColumnY(recipe, VISIBLE_ROWS);
            builder.addScrollGridWidget(inputs, 2, VISIBLE_ROWS).setPosition(4, inputY - 1);
        }

        List<IRecipeSlotDrawable> outputs = recipeSlots.getSlots(RecipeIngredientRole.OUTPUT);
        int outputRows = Math.min(outputs.size(), VISIBLE_ROWS);
        int outputX = getOutputX(recipe);
        int outputY = getColumnY(recipe, outputRows);
        if (outputs.size() > VISIBLE_ROWS) {
            builder.addScrollGridWidget(outputs, 1, VISIBLE_ROWS).setPosition(outputX - 1, outputY - 1);
        }

        CCBJEITextures arrowTexture = condensation ? CCBJEITextures.JEI_CONDENSATION_ARROW : CCBJEITextures.JEI_FRACTIONATION_ARROW;
        CCBJEITextures modeIcon = condensation ? CCBJEITextures.JEI_CONDENSATION : CCBJEITextures.JEI_FRACTIONATION;
        OutputArrow arrow = new OutputArrow(outputRows * SLOT_SIZE, arrowTexture, modeIcon, !condensation);
        builder.addDrawable(arrow).setPosition(outputX - arrow.getWidth() - 5, outputY - 1);
    }

    private int getOutputX(FractionationTowerRecipe recipe) {
        if (recipe.getLayerOutputs().size() > VISIBLE_ROWS) {
            return getWidth() - 37;
        }

        return getWidth() - 21;
    }

    private int getColumnY(FractionationTowerRecipe recipe, int count) {
        int bottomY = getHeight() - 42;
        if (!recipe.isCondensation()) {
            return bottomY - Math.clamp(count, 1, VISIBLE_ROWS) * SLOT_SIZE + SLOT_SIZE;
        }

        return bottomY - VISIBLE_ROWS * SLOT_SIZE + SLOT_SIZE;
    }

    private record OutputArrow(int height, CCBJEITextures texture, CCBJEITextures modeIcon, boolean upward) implements IDrawable {
        @Override
        public int getWidth() {
            return texture.getWidth() + 4;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
            ResourceLocation location = texture.getLocation();
            int width = texture.getWidth();
            int sourceX = texture.getStartX();
            int sourceY = texture.getStartY();
            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            try {
                poseStack.translate(xOffset, yOffset, 0);
                graphics.blit(location, 4, 0, sourceX, sourceY, width, 7);

                poseStack.pushPose();
                try {
                    poseStack.translate(0, 7, 0);
                    poseStack.scale(1, height - 14, 1);
                    graphics.blit(location, 4, 0, sourceX, sourceY + 7, width, 1);
                }
                finally {
                    poseStack.popPose();
                }

                graphics.blit(location, 4, height - 7, sourceX, sourceY + 8, width, 7);
                float iconScale = Math.min(1, (float) (height - 7) / modeIcon.getHeight());
                poseStack.translate(width - 5 - modeIcon.getWidth() * iconScale, upward ? height - 7 - modeIcon.getHeight() * iconScale : 7, 0);
                poseStack.scale(iconScale, iconScale, 1);
                modeIcon.render(graphics, 0, 0);
            }
            finally {
                poseStack.popPose();
            }
        }
    }
}
