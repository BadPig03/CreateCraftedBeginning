package net.ty.createcraftedbeginning.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AnimatedGasInjectionChamber extends AnimatedKinetics {
    private static final int SCALE = 20;

    private final boolean isBasin;

    public AnimatedGasInjectionChamber(boolean isBasin) {
        this.isBasin = isBasin;
    }

    private static float getNozzleSqueeze(float cycle) {
        if (cycle < 20) {
            return Mth.sin(cycle / 40.0f * Mth.PI) * 15;
        }

        if (cycle > 60 && cycle < 80) {
            return Mth.sin((cycle - 40) / 40.0f * Mth.PI) * 15;
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
            return Mth.sin((cycle - 20) / 60.0f * Mth.PI) * 7;
        }

        if (cycle > 50) {
            return Mth.sin(cycle / 60.0f * Mth.PI) * 7;
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
        blockElement(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE).scale(SCALE).render(graphics);
        poseStack.translate(0, getNozzleTopSqueeze(cycle), 0);
        blockElement(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE_TOP).scale(SCALE).render(graphics);
        blockElement(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE_BOTTOM).scale(SCALE).render(graphics);

        poseStack.popPose();

        BlockState supportState = isBasin ? AllBlocks.BASIN.getDefaultState() : AllBlocks.DEPOT.getDefaultState();
        blockElement(supportState).atLocal(0, 2, 0).scale(SCALE).render(graphics);

        poseStack.popPose();
    }
}
