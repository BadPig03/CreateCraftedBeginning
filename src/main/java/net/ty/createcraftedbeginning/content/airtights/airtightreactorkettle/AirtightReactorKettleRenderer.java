package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class AirtightReactorKettleRenderer extends SmartBlockEntityRenderer<AirtightReactorKettleBlockEntity> {
    public AirtightReactorKettleRenderer(Context context) {
        super(context);
    }

    private static float renderFluids(@NotNull AirtightReactorKettleBlockEntity kettle, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
        Couple<SmartFluidTankBehaviour> fluidTanks = kettle.getFluidTanks();
        float totalUnits = AirtightReactorKettleUtils.getTotalFluidUnits(fluidTanks, partialTicks);
        if (totalUnits < 1) {
            return 0;
        }

        ms.pushPose();

        ms.translate(0, -1, 0);
        float fluidLevel = Mth.clamp(totalUnits / AirtightReactorKettleBlockEntity.MAX_FLUID_CAPACITY, 0, 1);
        fluidLevel = 1 - (1 - fluidLevel) * (1 - fluidLevel);
        float xMin = -0.875f;
        float xMax = -0.875f;
        float yMin = 0.125f;
        float yMax = yMin + (0.875f - yMin) * fluidLevel;
        float zMax = 1.875f;
        for (SmartFluidTankBehaviour behaviour : fluidTanks) {
            if (behaviour == null) {
                continue;
            }

            for (TankSegment tankSegment : behaviour.getTanks()) {
                FluidStack renderedFluid = tankSegment.getRenderedFluid();
                if (renderedFluid.isEmpty()) {
                    continue;
                }

                float units = tankSegment.getTotalUnits(partialTicks);
                if (units < 1) {
                    continue;
                }

                float partial = Mth.clamp(units / totalUnits, 0, 1);
                xMax += partial * 2.75f;
                NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(renderedFluid, xMin, yMin, -0.875f, xMax, yMax, zMax, buffer, ms, light, false, false);
                xMin = xMax;
            }
        }

        ms.popPose();
        return yMax;
    }

    private static void renderItems(@NotNull AirtightReactorKettleBlockEntity kettle, float fluidLevel, float partialTicks, @NotNull PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ms.pushPose();

        ms.translate(0.5f, -0.8f, 0.5f);
        TransformStack.of(ms).rotateYDegrees(kettle.getIngredientRotation().getValue(partialTicks));
        int hashCode = kettle.getBlockPos().hashCode();
        float clampedFluidLevel = Mth.clamp(fluidLevel - 0.3f, 0.05f, 0.6f);
        IItemHandlerModifiable inventory = kettle.getItemCapability();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            RandomSource random = RandomSource.create(hashCode + slot * 31L);
            float angle = random.nextIntBetweenInclusive(-180, 180);
            float distance = random.nextFloat();
            ms.pushPose();
            if (fluidLevel > 0) {
                ms.translate(0, (Mth.sin(AnimationTickHolder.getRenderTime(kettle.getLevel()) / 12.0f + angle) + 1.5f) / 32.0f, 0);
            }
            float itemOffset = stack.getItem() instanceof BlockItem ? 0 : 0.1f;
            Vec3 positionOffset = new Vec3(distance, clampedFluidLevel - itemOffset, 0);
            Vec3 itemPosition = VecHelper.rotate(positionOffset, angle, Axis.Y);
            ms.translate(itemPosition.x, itemPosition.y, itemPosition.z);
            TransformStack.of(ms).rotateYDegrees(angle + 35).rotateXDegrees(90);
            for (int i = 0; i <= stack.getCount() / 8; i++) {
                ms.pushPose();

                Vec3 vec = Vec3.ZERO;
                if (i > 0) {
                    vec = VecHelper.offsetRandomly(vec, random, 0.0625f);
                }
                ms.translate(vec.x, vec.y, vec.z);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, light, overlay, ms, buffer, Minecraft.getInstance().level, 0);

                ms.popPose();
            }

            ms.popPose();
        }

        ms.popPose();
    }

    private static void renderMixerModels(@NotNull AirtightReactorKettleBlockEntity kettle, float partialTicks, PoseStack ms, @NotNull MultiBufferSource buffer, int light) {
        BlockState state = kettle.getBlockState();
        SuperByteBuffer mixerModel = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_MIXER, state);
        float angle = kettle.getMixerRotation().getValue(partialTicks) * Mth.DEG_TO_RAD;
        float offset = kettle.getMixerOffset(partialTicks);
        mixerModel.translate(0, -offset, 0).rotateCentered(angle, Direction.UP).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    private static void renderWindowsModels(@NotNull AirtightReactorKettleBlockEntity kettle, float partialTicks, PoseStack ms, @NotNull MultiBufferSource buffer, int light) {
        BlockState state = kettle.getBlockState();
        SuperByteBuffer leftWindowModel = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_LEFT_WINDOW, state);
        SuperByteBuffer rightWindowModel = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_RIGHT_WINDOW, state);
        float distanceScale = kettle.getWindowDistance().getValue(partialTicks);
        for (Direction direction : Iterate.horizontalDirections) {
            Vec3i leftDistance = direction.getClockWise().getNormal();
            Vec3i rightDistance = direction.getCounterClockWise().getNormal();
            leftWindowModel.translate(direction.getNormal()).translate(leftDistance.getX() * distanceScale, leftDistance.getY() * distanceScale, leftDistance.getZ() * distanceScale).rotateYCenteredDegrees(AngleHelper.horizontalAngle(direction)).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
            rightWindowModel.translate(direction.getNormal()).translate(rightDistance.getX() * distanceScale, rightDistance.getY() * distanceScale, rightDistance.getZ() * distanceScale).rotateYCenteredDegrees(AngleHelper.horizontalAngle(direction)).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        }
    }

    @Override
    protected void renderSafe(@NotNull AirtightReactorKettleBlockEntity kettle, float partialTicks, PoseStack ms, @NotNull MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(kettle, partialTicks, ms, buffer, light, overlay);
        renderMixerModels(kettle, partialTicks, ms, buffer, light);
        float fluidLevel = renderFluids(kettle, partialTicks, ms, buffer, light);
        renderWindowsModels(kettle, partialTicks, ms, buffer, light);
        renderItems(kettle, fluidLevel, partialTicks, ms, buffer, light, overlay);
    }

    @Override
	public boolean shouldRenderOffScreen(@NotNull AirtightReactorKettleBlockEntity kettle) {
		return true;
	}
}
