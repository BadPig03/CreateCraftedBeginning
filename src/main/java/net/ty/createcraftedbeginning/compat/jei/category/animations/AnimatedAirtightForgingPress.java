package net.ty.createcraftedbeginning.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.client.CCBPartialModels;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralPosition;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AnimatedAirtightForgingPress extends AnimatedKinetics {
    private static final int SCALE = 12;

    private static float getSqueeze(float cycle) {
        float ticks = Mth.clamp(cycle, 0, 30);
        float distance;
        if (ticks < 20) {
            float progress = ticks / 15;
            distance = Mth.clamp(Mth.square(progress) * progress, 0, 1);
        }
        else {
            distance = Mth.clamp((30 - ticks) / 10, 0, 1);
        }
        return distance * 8;
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        poseStack.translate(xOffset, yOffset, 192);
        poseStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        poseStack.mulPose(Axis.YP.rotationDegrees(22.5f));
        poseStack.pushPose();
        float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 45;
        poseStack.translate(0, getSqueeze(cycle), 0);
        blockElement(CCBPartialModels.AIRTIGHT_FORGING_PRESS_PRESS_HEAD).atLocal(0, -0.5, 0).scale(SCALE).render(graphics);
        poseStack.popPose();

        for (AirtightForgingPressStructuralPosition structuralPosition : AirtightForgingPressStructuralPosition.all()) {
            var structureOffset = structuralPosition.getStructureOffset();
            int x = structureOffset.getX();
            int y = structureOffset.getY();
            int z = structureOffset.getZ();
            if (!structuralPosition.isShaft()) {
                blockElement(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_BLOCK.getDefaultState().setValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION, structuralPosition)).atLocal(x, -y, z).scale(SCALE).render(graphics);
                continue;
            }

            blockElement(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT_BLOCK.getDefaultState().setValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION, structuralPosition)).atLocal(x, -y, z).scale(SCALE).render(graphics);
            if (structuralPosition == AirtightForgingPressStructuralPosition.TOP_LEFT_MID) {
                blockElement(CCBPartialModels.SHAFT_HALF_UP).rotateBlock(0, -getCurrentAngle() * 2, 90).atLocal(x, -y, z).scale(SCALE).render(graphics);
            }
            else if (structuralPosition == AirtightForgingPressStructuralPosition.TOP_MID_DOWN) {
                blockElement(CCBPartialModels.SHAFT_HALF_UP).rotateBlock(90, getCurrentAngle() * 2, 0).atLocal(x, -y, z).scale(SCALE).render(graphics);
            }
        }

        poseStack.popPose();
    }
}
