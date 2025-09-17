package net.ty.createcraftedbeginning.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.gui.GuiGraphics;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class AnimatedBreezeCooler extends AnimatedKinetics {
    @Override
    public void draw(@NotNull GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(xOffset, yOffset, 192);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));

        int scale = 23;

        PartialModel head = CCBPartialModels.BREEZE_CHILLED;
        int speed = 4;

        blockElement(CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState()).atLocal(0, 1, 0).scale(scale).render(graphics);
        blockElement(CCBPartialModels.BREEZE_COOLER_WIND).rotateBlock(0, getCurrentAngle() * speed, 0).atLocal(0, 1, 0).scale(scale).render(graphics);
        blockElement(head).rotateBlock(0, 180, 0).atLocal(0, 1, 0).scale(scale).render(graphics);

        matrixStack.popPose();
    }
}
