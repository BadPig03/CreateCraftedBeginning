package net.ty.createcraftedbeginning.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.content.aircompressor.AirCompressorBlock;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

public class AnimatedAirCompressor extends AnimatedKinetics {
    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(xOffset, yOffset, 192);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));

        int scale = 23;
        blockElement(CCBBlocks.AIR_COMPRESSOR_BLOCK.getDefaultState().setValue(AirCompressorBlock.HORIZONTAL_FACING, Direction.SOUTH)).scale(scale).render(graphics);
        blockElement(CCBBlocks.BREEZE_CHAMBER_BLOCK.getDefaultState().setValue(BreezeChamberBlock.COOLER, true)).atLocal(0, 1, 0).scale(scale).render(graphics);
        blockElement(CCBPartialModels.BREEZE_CHILLED_ACTIVE).rotateBlock(0, 180, 0).atLocal(0, 1, 0).scale(scale).render(graphics);
        blockElement(CCBPartialModels.BREEZE_WIND).rotateBlock(0, getCurrentAngle() * 4, 0).atLocal(0, 1, 0).scale(scale).render(graphics);
        blockElement(CCBPartialModels.AIR_COMPRESSOR_SHAFT).rotateBlock(0, getCurrentAngle() * 2, 0).atLocal(0, 0, 0).scale(scale).render(graphics);

        matrixStack.popPose();
    }
}
