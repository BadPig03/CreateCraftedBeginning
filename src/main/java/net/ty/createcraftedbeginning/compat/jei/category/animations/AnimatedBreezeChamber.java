package net.ty.createcraftedbeginning.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.gui.GuiGraphics;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

public class AnimatedBreezeChamber extends AnimatedKinetics {
    private boolean isGalling = false;

    public AnimatedBreezeChamber withFrost(boolean isGalling) {
		this.isGalling = isGalling;
		return this;
	}

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(xOffset, yOffset, 192);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));

        int scale = 23;

        PartialModel head = isGalling ? CCBPartialModels.BREEZE_GALLING : CCBPartialModels.BREEZE_CHILLED;
        int speed = isGalling ? 6 : 4;

        blockElement(CCBBlocks.BREEZE_CHAMBER_BLOCK.getDefaultState()).atLocal(0, 1, 0).scale(scale).render(graphics);
        blockElement(CCBPartialModels.BREEZE_WIND).rotateBlock(0, getCurrentAngle() * speed, 0).atLocal(0, 1, 0).scale(scale).render(graphics);
        blockElement(head).rotateBlock(0, 180, 0).atLocal(0, 1, 0).scale(scale).render(graphics);

        matrixStack.popPose();
    }
}
