package net.ty.createcraftedbeginning.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class AnimatedAirCompressor extends AnimatedKinetics {
    @Override
    public void draw(@NotNull GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(xOffset, yOffset, 192);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));

        int scale = 23;
        blockElement(CCBBlocks.AIR_COMPRESSOR_BLOCK.getDefaultState().setValue(AirCompressorBlock.HORIZONTAL_FACING, Direction.SOUTH)).scale(scale).render(graphics);
        blockElement(CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState().setValue(BreezeCoolerBlock.COOLER, true)).atLocal(0, 1, 0).scale(scale).render(graphics);
        blockElement(CCBPartialModels.BREEZE_CHILLED_ACTIVE).rotateBlock(0, 180, 0).atLocal(0, 1, 0).scale(scale).render(graphics);
        blockElement(CCBPartialModels.BREEZE_COOLER_WIND).rotateBlock(0, getCurrentAngle() * 4, 0).atLocal(0, 1, 0).scale(scale).render(graphics);
        blockElement(CCBPartialModels.AIR_COMPRESSOR_SHAFT).rotateBlock(0, getCurrentAngle() * 2, 0).atLocal(0, 0, 0).scale(scale).render(graphics);

        matrixStack.popPose();
    }
}
