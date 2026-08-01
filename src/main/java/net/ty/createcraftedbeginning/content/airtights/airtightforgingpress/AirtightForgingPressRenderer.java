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
    private static final float OUTPUT_RADIUS = 0.5f;
    private static final int MAX_OUTPUT_SLOTS = 8;
    private static final OutputPlacement[] OUTPUT_PLACEMENTS = createOutputPlacements();
    private static final ThreadLocal<Random> RENDER_RANDOM = ThreadLocal.withInitial(Random::new);

    public AirtightForgingPressRenderer(Context context) {
        super(context);
    }

    private static void renderPressHead(AirtightForgingPressBlockEntity press, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        SuperByteBuffer head = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_FORGING_PRESS_PRESS_HEAD, press.getBlockState());
        head.translate(0, -press.getPressHeadDistance(partialTicks), 0).light(light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    private static void renderItems(AirtightForgingPressBlockEntity press, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        Vec3 itemPosition = VecHelper.getCenterOf(press.getBlockPos());
        renderInputItem(press, poseStack, buffer, light, overlay, itemPosition);
        renderOutputItems(press, poseStack, buffer, light, overlay, itemPosition);
        renderPressHeadItem(press, partialTicks, poseStack, buffer, light, overlay, itemPosition);
    }

    private static void renderPressHeadItem(AirtightForgingPressBlockEntity press, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, Vec3 itemPosition) {
        ItemStack stack = press.getPressHeadInventory().getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5f, -0.1f - press.getPressHeadDistance(partialTicks), 0.5f);
        if (!Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0).isGui3d()) {
            poseStack.translate(0, 0.1875f, 0);
        }
        TransformStack.of(poseStack).nudge(0);

        Random random = getRenderRandom(press.getBlockPos().asLong());
        int angle = Mth.floor(360 * random.nextFloat());
        DepotRenderer.renderItem(poseStack, buffer, light, overlay, stack, angle, random, itemPosition, false);
        poseStack.popPose();
    }

    private static void renderInputItem(AirtightForgingPressBlockEntity press, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, Vec3 itemPosition) {
        ItemStack stack = press.getInputInventory().getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5f, -0.0625f, 0.5f);
        TransformStack.of(poseStack).nudge(0);

        Random random = getRenderRandom(press.getBlockPos().asLong());
        int angle = Mth.floor(360 * random.nextFloat());
        DepotRenderer.renderItem(poseStack, buffer, light, overlay, stack, angle, random, itemPosition, false);
        poseStack.popPose();
    }

    private static void renderOutputItems(AirtightForgingPressBlockEntity press, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, Vec3 itemPosition) {
        SmartInventory inventory = press.getOutputInventory();
        int slots = inventory.getSlots();
        if (slots <= 0) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5f, -0.0625f, 0.5f);

        long posSeed = press.getBlockPos().asLong();
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            poseStack.pushPose();
            OutputPlacement placement = OUTPUT_PLACEMENTS[slot];
            TransformStack.of(poseStack).rotateYDegrees(placement.angle());
            poseStack.translate(OUTPUT_RADIUS, 0, 0);

            Random random = getRenderRandom(slot + posSeed);
            Vec3 outputPosition = itemPosition.add(placement.offset());
            int angle = Mth.floor(360 * random.nextFloat());
            boolean isUpright = BeltHelper.isItemUpright(stack);
            if (isUpright) {
                TransformStack.of(poseStack).rotateYDegrees(-placement.angle());
                angle += 90;
            }

            DepotRenderer.renderItem(poseStack, buffer, light, overlay, stack, angle, random, outputPosition, false);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static Random getRenderRandom(long seed) {
        Random random = RENDER_RANDOM.get();
        random.setSeed(seed);
        return random;
    }

    private static OutputPlacement[] createOutputPlacements() {
        OutputPlacement[] placements = new OutputPlacement[MAX_OUTPUT_SLOTS];
        for (int slot = 0; slot < placements.length; slot++) {
            float angle = 360.0f / placements.length * slot;
            float radians = angle * Mth.DEG_TO_RAD;
            Vec3 offset = new Vec3(Mth.cos(radians) * OUTPUT_RADIUS, 0, -Mth.sin(radians) * OUTPUT_RADIUS);
            placements[slot] = new OutputPlacement(angle, offset);
        }
        return placements;
    }

    @Override
    protected void renderSafe(AirtightForgingPressBlockEntity press, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(press, partialTicks, poseStack, buffer, light, overlay);
        renderPressHead(press, partialTicks, poseStack, buffer, light);
        renderItems(press, partialTicks, poseStack, buffer, light, overlay);
    }

    private record OutputPlacement(float angle, Vec3 offset) {}
}
