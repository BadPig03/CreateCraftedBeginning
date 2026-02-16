package net.ty.createcraftedbeginning.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class AnimatedAirtightEngine extends AnimatedKinetics {
    private static final int SCALE = 23;

    private static float getPistonOffset() {
        float phase = AnimationTickHolder.getRenderTime() * 32 * AirtightEngineBlockEntity.DELTA_TIME % Mth.TWO_PI;
        return -0.2f * Mth.sin(phase) + 0.2f;
    }

    @Override
    public void draw(@NotNull GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();

        matrixStack.translate(xOffset, yOffset, 192);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
        blockElement(CCBBlocks.BREEZE_CHAMBER_BLOCK.getDefaultState()).atLocal(0.25, 0, -0.5).scale(SCALE).render(graphics);
        blockElement(CCBPartialModels.BREEZE_CHAMBER_WIND).rotateBlock(0, getCurrentAngle() * 4, 0).atLocal(0.25, 0.125, -0.5).scale(SCALE).render(graphics);
        blockElement(CCBPartialModels.BREEZE_GALE).rotateBlock(0, 180, 0).atLocal(0.25, 0.125, -0.5).scale(SCALE).render(graphics);
        blockElement(CCBBlocks.AIRTIGHT_ENGINE_BLOCK.getDefaultState().setValue(AirtightEngineBlock.FACE, AttachFace.WALL).setValue(AirtightEngineBlock.FACING, Direction.EAST)).atLocal(-0.75, 1, -0.5).scale(SCALE).render(graphics);
        blockElement(CCBPartialModels.AIRTIGHT_ENGINE_COGS).rotateBlock(0, getCurrentAngle() * 2, 90).atLocal(-0.75, 1, -0.5).scale(SCALE).render(graphics);
        blockElement(CCBBlocks.AIRTIGHT_TANK_BLOCK.getDefaultState()).atLocal(0.25, 1, -0.5).scale(SCALE).render(graphics);
        blockElement(CCBBlocks.RESIDUE_OUTLET_BLOCK.getDefaultState().setValue(ResidueOutletBlock.FACE, AttachFace.WALL).setValue(ResidueOutletBlock.FACING, Direction.NORTH)).atLocal(0.25, 1, 0.5).scale(SCALE).render(graphics);

        matrixStack.translate(getPistonOffset() * SCALE, 0, 0);
        blockElement(CCBPartialModels.AIRTIGHT_ENGINE_PISTON).rotateBlock(0, 0, 90).atLocal(-0.75, 1, -0.5).scale(SCALE).render(graphics);

        matrixStack.popPose();
    }
}
