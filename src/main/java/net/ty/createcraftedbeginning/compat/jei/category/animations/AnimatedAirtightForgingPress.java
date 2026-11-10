package net.ty.createcraftedbeginning.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralPosition;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AnimatedAirtightForgingPress extends AnimatedKinetics {
    private static final int SCALE = 12;
    private static final int RETRACTION_START_TICKS = 20;
    private static final int RETRACTION_END_TICKS = 30;
    private static final int PRESSING_TICKS = 15;
    private static final int RETRACTION_TICKS = 10;
    private static final int CYCLE_TICKS = 45;
    private static final int OFFSET_STEP_TICKS = 8;

    private static float getSqueeze(float cycle) {
        float ticks = Mth.clamp(cycle, 0.0F, RETRACTION_END_TICKS);
        float distance;
        if (ticks < RETRACTION_START_TICKS) {
            float progress = ticks / PRESSING_TICKS;
            distance = Mth.clamp(Mth.square(progress) * progress, 0.0F, 1.0F);
        }
        else {
            distance = Mth.clamp((RETRACTION_END_TICKS - ticks) / RETRACTION_TICKS, 0.0F, 1.0F);
        }
        return distance * 8;
    }

    @SuppressWarnings("ConstantExpression")
    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        try {
            poseStack.translate(xOffset, yOffset, 192);
            poseStack.mulPose(Axis.XP.rotationDegrees(-15.5F));
            poseStack.mulPose(Axis.YP.rotationDegrees(22.5F));
            poseStack.pushPose();
            try {
                float cycle = (AnimationTickHolder.getRenderTime() - offset * OFFSET_STEP_TICKS) % CYCLE_TICKS;
                poseStack.translate(0, getSqueeze(cycle), 0);
                blockElement(CCBPartialModels.AIRTIGHT_FORGING_PRESS_PRESS_HEAD).atLocal(0, -0.5, 0).scale(SCALE).render(graphics);
            }
            finally {
                poseStack.popPose();
            }

            for (AirtightForgingPressStructuralPosition structuralPosition : AirtightForgingPressStructuralPosition.all()) {
                BlockPos structureOffset = structuralPosition.getStructureOffset();
                int x = structureOffset.getX();
                int y = structureOffset.getY();
                int z = structureOffset.getZ();
                if (!structuralPosition.isShaft()) {
                    blockElement(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_BLOCK.getDefaultState().setValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION, structuralPosition)).atLocal(x, -y, z).scale(SCALE).render(graphics);
                    continue;
                }

                blockElement(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT_BLOCK.getDefaultState().setValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION, structuralPosition)).atLocal(x, -y, z).scale(SCALE).render(graphics);
                if (structuralPosition == AirtightForgingPressStructuralPosition.TOP_LEFT_MID) {
                    blockElement(AllPartialModels.SHAFT_HALF).rotateBlock(getCurrentAngle() * 2 - 90, -90, 0).atLocal(x, -y, z).scale(SCALE).render(graphics);
                    continue;
                }

                if (structuralPosition != AirtightForgingPressStructuralPosition.TOP_MID_DOWN) {
                    continue;
                }

                blockElement(AllPartialModels.SHAFT_HALF).rotateBlock(0, 0, getCurrentAngle() * 2).atLocal(x, -y, z).scale(SCALE).render(graphics);
            }
        }
        finally {
            poseStack.popPose();
        }
    }
}
