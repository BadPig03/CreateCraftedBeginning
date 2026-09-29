package net.ty.createcraftedbeginning.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;
import net.ty.createcraftedbeginning.compat.jei.CCBJEITextures;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AnimatedAirtightFractionationTower extends AnimatedKinetics {
    private static final int SCALE = 12;
    private static final double PROJECTED_HALF_DEPTH = SCALE * 1.5 * Math.sin(0.27052603405912107) * (Math.cos(0.39269908169872414) + Math.sin(0.39269908169872414));
    private static final double PROJECTED_HEIGHT = SCALE * 3 * Math.cos(-0.27052603405912107) + 2 * PROJECTED_HALF_DEPTH;

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        draw(graphics, xOffset, yOffset, 3, Mth.ceil(PROJECTED_HEIGHT));
    }

    @SuppressWarnings("ConstantExpression")
    public void draw(GuiGraphics graphics, int xOffset, int bottomY, int requiredLayers, int availableHeight) {
        float scale = (float) Math.min(1, Math.max(1, availableHeight) / PROJECTED_HEIGHT);
        double yOffset = bottomY - PROJECTED_HALF_DEPTH * scale;
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset, 0);
        poseStack.scale(scale, scale, 1);
        CCBJEITextures.JEI_SHADOW.render(graphics, -CCBJEITextures.JEI_SHADOW.getWidth() / 2, -CCBJEITextures.JEI_SHADOW.getHeight() / 2);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset, 192);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(-15.5F));
        poseStack.mulPose(Axis.YP.rotationDegrees(22.5F));
        blockElement(CCBPartialModels.AIRTIGHT_FRACTIONATION_TOWER_JEI_PREVIEW).atLocal(-0.5, -1, -0.5).scale(SCALE).render(graphics);
        poseStack.popPose();

        Font font = Minecraft.getInstance().font;
        String layerCount = Integer.toString(requiredLayers);
        poseStack.pushPose();
        poseStack.translate(xOffset, bottomY, 300);
        graphics.drawString(font, layerCount, 2 + Math.round(24 * scale) - font.width(layerCount), -font.lineHeight, 0xFFFFFFFF, true);
        poseStack.popPose();
    }
}
