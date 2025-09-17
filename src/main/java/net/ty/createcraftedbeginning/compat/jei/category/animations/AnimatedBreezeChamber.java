package net.ty.createcraftedbeginning.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.minecraft.client.gui.GuiGraphics;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class AnimatedBreezeChamber extends AnimatedKinetics {
    @Override
    public void draw(@NotNull GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(xOffset, yOffset, 192);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));

        int scale = 23;

        blockElement(CCBBlocks.BREEZE_CHAMBER_BLOCK.getDefaultState()).atLocal(0, 1, 0).scale(scale).render(graphics);
        blockElement(CCBPartialModels.BREEZE_CHAMBER_WIND).rotateBlock(0, getCurrentAngle() * 4, 0).atLocal(0, 9 / 8d, 0).scale(scale).render(graphics);
        blockElement(CCBPartialModels.BREEZE_GALE).rotateBlock(0, 180, 0).atLocal(0, 9 / 8d, 0).scale(scale).render(graphics);

        matrixStack.popPose();
    }
}
