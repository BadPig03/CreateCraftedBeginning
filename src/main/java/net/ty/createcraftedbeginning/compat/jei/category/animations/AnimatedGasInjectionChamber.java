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
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AnimatedGasInjectionChamber extends AnimatedKinetics {
    private static final int SCALE = 20;
    private static final int NOZZLE_EXTEND_END_TICKS = 20;
    private static final int NOZZLE_RETRACT_START_TICKS = 60;
    private static final int NOZZLE_RETRACT_END_TICKS = 80;
    private static final int NOZZLE_RETURN_PHASE_TICKS = 40;
    private static final float NOZZLE_WAVE_TICKS = 40.0F;
    private static final int TOP_EXTEND_END_TICKS = 30;
    private static final int TOP_RETRACT_START_TICKS = 50;
    private static final float TOP_WAVE_TICKS = 60.0F;
    private static final int CYCLE_TICKS = 100;
    private static final int OFFSET_STEP_TICKS = 8;

    private final boolean isBasin;

    public AnimatedGasInjectionChamber(boolean isBasin) {
        this.isBasin = isBasin;
    }

    private static float getNozzleSqueeze(float cycle) {
        if (cycle < NOZZLE_EXTEND_END_TICKS) {
            return Mth.sin(cycle / NOZZLE_WAVE_TICKS * Mth.PI) * 15;
        }

        if (cycle > NOZZLE_RETRACT_START_TICKS && cycle < NOZZLE_RETRACT_END_TICKS) {
            return Mth.sin((cycle - NOZZLE_RETURN_PHASE_TICKS) / NOZZLE_WAVE_TICKS * Mth.PI) * 15;
        }

        if (cycle >= NOZZLE_RETRACT_END_TICKS) {
            return 0;
        }

        return 15;
    }

    private static float getNozzleTopSqueeze(float cycle) {
        if (cycle <= NOZZLE_EXTEND_END_TICKS || cycle >= NOZZLE_RETRACT_START_TICKS) {
            return 0;
        }

        if (cycle < TOP_EXTEND_END_TICKS) {
            return Mth.sin((cycle - NOZZLE_EXTEND_END_TICKS) / TOP_WAVE_TICKS * Mth.PI) * 7;
        }

        if (cycle > TOP_RETRACT_START_TICKS) {
            return Mth.sin(cycle / TOP_WAVE_TICKS * Mth.PI) * 7;
        }

        return 3.5F;
    }

    @SuppressWarnings("ConstantExpression")
    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        try {
            poseStack.translate(xOffset, yOffset, 100);
            poseStack.mulPose(Axis.XP.rotationDegrees(-15.5F));
            poseStack.mulPose(Axis.YP.rotationDegrees(22.5F));

            poseStack.pushPose();
            try {
                blockElement(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK.getDefaultState()).scale(SCALE).render(graphics);
                float cycle = (AnimationTickHolder.getRenderTime() - offset * OFFSET_STEP_TICKS) % CYCLE_TICKS;
                poseStack.translate(0, getNozzleSqueeze(cycle), 0);
                blockElement(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE).scale(SCALE).render(graphics);
                poseStack.translate(0, getNozzleTopSqueeze(cycle), 0);
                blockElement(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE_TOP).scale(SCALE).render(graphics);
                blockElement(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE_BOTTOM).scale(SCALE).render(graphics);
            }
            finally {
                poseStack.popPose();
            }

            BlockState supportState = isBasin ? AllBlocks.BASIN.getDefaultState() : AllBlocks.DEPOT.getDefaultState();
            blockElement(supportState).atLocal(0, 2, 0).scale(SCALE).render(graphics);
        }
        finally {
            poseStack.popPose();
        }
    }
}
