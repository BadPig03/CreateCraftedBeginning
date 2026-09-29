package net.ty.createcraftedbeginning.client.stockkeeper;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestFormat;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerRenderer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class StockKeeperRequestRendering {
    private StockKeeperRequestRendering() {
    }

    public static void drawGasCount(GuiGraphics graphics, int customCount) {
        String text = GasRequestFormat.formatDecoration(customCount, true);
        if (text.isBlank()) {
            return;
        }

        int textWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            char character = Character.toLowerCase(text.charAt(i));
            if (character == ',') {
                continue;
            }

            int spriteWidth;
            switch (character) {
                case ' ' -> spriteWidth = 4;
                case '.' -> spriteWidth = 3;
                case 'm' -> spriteWidth = 7;
                case '+' -> spriteWidth = 9;
                default -> spriteWidth = AllGuiTextures.NUMBERS.getWidth();
            }

            textWidth += spriteWidth;
            if (i >= text.length() - 1) {
                continue;
            }

            textWidth -= 1;
        }

        RenderSystem.enableBlend();
        int x = AllGuiTextures.NUMBERS.getWidth() - textWidth;
        for (char character : text.toCharArray()) {
            character = Character.toLowerCase(character);
            int index = character - '0';
            int xOffset = index * 6;
            int spriteWidth = AllGuiTextures.NUMBERS.getWidth();
            switch (character) {
                case ' ':
                    x += 4;
                    continue;

                case ',':
                    continue;

                case '.':
                    spriteWidth = 3;
                    xOffset = 60;
                    break;

                case 'k':
                    xOffset = 64;
                    break;

                case 'm':
                    spriteWidth = 7;
                    xOffset = 70;
                    break;

                case 'b':
                    xOffset = 78;
                    break;

                case '+':
                    spriteWidth = 9;
                    xOffset = 84;
                    break;
            }

            graphics.blit(AllGuiTextures.NUMBERS.location, 14 + x, 10, 0, AllGuiTextures.NUMBERS.getStartX() + xOffset, AllGuiTextures.NUMBERS.getStartY(), spriteWidth, AllGuiTextures.NUMBERS.getHeight(), 256, 256);
            x += spriteWidth - 1;
        }
    }

    public static void renderBreeze(GuiGraphics graphics, Level level, BreezeCoolerBlockEntity breeze, boolean hasBlaze, int left, int top, int windowHeight) {
        PoseStack pose = graphics.pose();
        pose.pushPose();

        if (hasBlaze) {
            pose.translate(0, -64, 0);
        }

        pose.translate(left - 35, top + windowHeight - 43, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-22.5F));
        pose.mulPose(Axis.YP.rotationDegrees(-45));
        pose.scale(48, -48, 48);

        Lighting.setupForEntityInInventory();

        BlockState state = breeze.getBlockState();
        BufferSource buffers = graphics.bufferSource();
        float horizontalAngle = AngleHelper.rad(270);
        boolean isChilled = breeze.getFrostLevel().isAtLeast(FrostLevel.CHILLED);
        CachedBuffers.partial(CCBPartialModels.BREEZE_COOLER_BLOCK, state).rotateCentered(horizontalAngle + Mth.PI, Direction.UP).light(LightTexture.FULL_BRIGHT).renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
        PartialModel hatModel = null;
        if (breeze.hasTrainHat()) {
            hatModel = CCBPartialModels.BREEZE_TRAIN_HAT;
        }
        else if (breeze.isStockKeeper()) {
            hatModel = CCBPartialModels.BREEZE_LOGISTICS_HAT;
        }
        BreezeCoolerRenderer.renderShared(pose, null, buffers, level, state, breeze.getFrostLevelForRender(), breeze.getHeadAnimation().getValue(AnimationTickHolder.getPartialTicks()) * 0.175F, horizontalAngle, breeze.hasGoggles(), hatModel, isChilled, isChilled ? 24 : 0, breeze.hashCode(), LightTexture.FULL_BRIGHT, null);

        Lighting.setupFor3DItems();

        pose.popPose();
    }
}
