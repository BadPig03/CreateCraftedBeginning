package net.ty.createcraftedbeginning.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AnimatedGasInjectionChamber extends AnimatedKinetics {
    private static final int SCALE = 20;

    private static float getNozzleSqueeze(float cycle) {
        if (cycle < 20) {
            return Mth.sin((float) (cycle / 40.0f * Math.PI)) * 15;
        }
        if (cycle > 60 && cycle < 80) {
            return Mth.sin((float) ((cycle - 40) / 40.0f * Math.PI)) * 15;
        }
        if (cycle >= 80) {
            return 0;
        }
        return 15;
    }

    private static float getNozzleTopSqueeze(float cycle) {
        if (cycle <= 20 || cycle >= 60) {
            return 0;
        }

        if (cycle < 30) {
            return Mth.sin((float) ((cycle - 20) / 60.0f * Math.PI)) * 7;
        }
        if (cycle > 50) {
            return Mth.sin((float) (cycle / 60.0f * Math.PI)) * 7;
        }
        return 3.5f;
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        poseStack.translate(xOffset, yOffset, 100);
        poseStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        poseStack.mulPose(Axis.YP.rotationDegrees(22.5f));

        poseStack.pushPose();

        blockElement(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK.getDefaultState()).scale(SCALE).render(graphics);
        float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 100;
        poseStack.translate(0, getNozzleSqueeze(cycle), 0);
        blockElement(CCBPartialModels.NOZZLE).scale(SCALE).render(graphics);
        poseStack.translate(0, getNozzleTopSqueeze(cycle), 0);
        blockElement(CCBPartialModels.NOZZLE_TOP).scale(SCALE).render(graphics);
        blockElement(CCBPartialModels.NOZZLE_BOTTOM).scale(SCALE).render(graphics);

        poseStack.popPose();

        blockElement(AllBlocks.DEPOT.getDefaultState()).atLocal(0, 2, 0).scale(SCALE).render(graphics);

        poseStack.popPose();
    }
}
