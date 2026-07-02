package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.item.SmartInventory;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Random;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressRenderer extends SmartBlockEntityRenderer<AirtightForgingPressBlockEntity> {
    public AirtightForgingPressRenderer(Context context) {
        super(context);
    }

    private static void renderHeadModels(AirtightForgingPressBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
        SuperByteBuffer headModel = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_FORGING_PRESS_PRESS_HEAD, be.getBlockState());
        headModel.translate(0, -be.getPressHeadDistance(partialTicks), 0).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    private static void renderItems(AirtightForgingPressBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Vec3 itemPosition = VecHelper.getCenterOf(be.getBlockPos());
        renderInputItem(be, ms, buffer, light, overlay, itemPosition);
        renderOutputItems(be, ms, buffer, light, overlay, itemPosition);
        renderPressHeadItem(be, partialTicks, ms, buffer, light, overlay, itemPosition);
    }

    private static void renderPressHeadItem(AirtightForgingPressBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay, Vec3 itemPosition) {
        SmartInventory inventory = be.getProcessingInventories().getFirst();
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }

        ms.pushPose();

        ms.translate(0.5f, -0.1f - be.getPressHeadDistance(partialTicks), 0.5f);
        if (!Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0).isGui3d()) {
            ms.translate(0, 0.1875f, 0);
        }
        TransformStack.of(ms).nudge(0);

        Random random = new Random(be.getBlockPos().asLong());
        DepotRenderer.renderItem(ms, buffer, light, overlay, stack, Mth.floor(360 * random.nextFloat()), random, itemPosition, false);

        ms.popPose();
    }

    private static void renderInputItem(AirtightForgingPressBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light, int overlay, Vec3 itemPosition) {
        SmartInventory inputInventory = be.getInputOutputInventories().getFirst();
        ItemStack stack = inputInventory.getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }

        ms.pushPose();
        ms.translate(0.5f, -0.0625f, 0.5f);
        TransformStack.of(ms).nudge(0);

        Random random = new Random(be.getBlockPos().asLong());
        DepotRenderer.renderItem(ms, buffer, light, overlay, stack, Mth.floor(360 * random.nextFloat()), random, itemPosition, false);

        ms.popPose();
    }

    private static void renderOutputItems(AirtightForgingPressBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light, int overlay, Vec3 itemPosition) {
        SmartInventory outputInventory = be.getInputOutputInventories().getSecond();
        int slots = outputInventory.getSlots();
        if (slots <= 0) {
            return;
        }

        ms.pushPose();
        ms.translate(0.5f, -0.0625f, 0.5f);

        long posLong = be.getBlockPos().asLong();
        float radius = 0.5f;
        float outputBaseAngle = 0;
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = outputInventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ms.pushPose();

            float angle = outputBaseAngle + 360.0f / slots * slot;
            TransformStack.of(ms).rotateYDegrees(angle);
            ms.translate(radius, 0, 0);

            Random random = new Random(slot + posLong);
            float rad = angle / 180.0f * Mth.PI;
            Vec3 outputItemPosition = itemPosition.add(Mth.cos(rad) * radius, 0, -Mth.sin(rad) * radius);
            int itemAngle = Mth.floor(360 * random.nextFloat());
            boolean renderUpright = BeltHelper.isItemUpright(stack);
            if (renderUpright) {
                TransformStack.of(ms).rotateYDegrees(-angle);
            }

            DepotRenderer.renderItem(ms, buffer, light, overlay, stack, renderUpright ? itemAngle + 90 : itemAngle, random, outputItemPosition, false);

            ms.popPose();
        }

        ms.popPose();
    }

    @Override
    protected void renderSafe(AirtightForgingPressBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        renderHeadModels(be, partialTicks, ms, buffer, light);
        renderItems(be, partialTicks, ms, buffer, light, overlay);
    }

    @Override
    public boolean shouldRenderOffScreen(AirtightForgingPressBlockEntity kettle) {
        return true;
    }
}
