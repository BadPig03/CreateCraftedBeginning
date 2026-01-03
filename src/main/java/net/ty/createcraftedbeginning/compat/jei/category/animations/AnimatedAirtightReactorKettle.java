package net.ty.createcraftedbeginning.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.minecraft.client.gui.GuiGraphics;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralCogBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralPosition;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class AnimatedAirtightReactorKettle extends AnimatedKinetics {
    private static final int SCALE = 12;
    private final boolean closed;

    public AnimatedAirtightReactorKettle(boolean closed) {
        this.closed = closed;
    }

    @Override
    public void draw(@NotNull GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();

        matrixStack.translate(xOffset, yOffset, 192);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    if (i == 0 && j == 0 && k == 0) {
                        blockElement(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_MIXER).rotateBlock(0, getCurrentAngle() * 2, 0).atLocal(0, 0.5, 0).scale(SCALE).render(graphics);
                        continue;
                    }

                    AirtightReactorKettleStructuralPosition structuralPos = AirtightReactorKettleStructuralPosition.fromOffset(i, j, k);
                    if (structuralPos.isCog()) {
                        blockElement(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG_BLOCK.getDefaultState().setValue(AirtightReactorKettleStructuralCogBlock.STRUCTURAL_POSITION, structuralPos)).atLocal(i, -j, k).scale(SCALE).render(graphics);
                        blockElement(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_COGS).rotateBlock(0, getCurrentAngle() * 2, 0).atLocal(i, -j, k).scale(SCALE).render(graphics);
                    }
                    else {
                        blockElement(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_BLOCK.getDefaultState().setValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION, structuralPos)).atLocal(i, -j, k).scale(SCALE).render(graphics);
                    }
                }
            }
        }

        if (closed) {
            blockElement(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_WINDOW_CLOSED).scale(SCALE).render(graphics);
        }
        else {
            blockElement(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_WINDOW_OPENED).scale(SCALE).render(graphics);
        }

        matrixStack.popPose();
    }
}
