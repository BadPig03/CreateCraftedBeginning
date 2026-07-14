package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleRenderer extends SmartBlockEntityRenderer<AirtightReactorKettleBlockEntity> {
    private static final float MIN_RADIUS = 0.08f;
    private static final float MAX_RADIUS = 1.1f;
    private static final int MAX_RENDERED_COPIES = 4;
    private static final int MAX_RENDERED_ITEM_SLOTS = 64;
    private static final ItemPlacement[] ITEM_PLACEMENTS = createItemPlacements();

    public AirtightReactorKettleRenderer(Context context) {
        super(context);
    }

    private static float renderFluids(AirtightReactorKettleBlockEntity kettle, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
        SmartFluidTankBehaviour inputTank = kettle.getInputFluidTank();
        SmartFluidTankBehaviour outputTank = kettle.getOutputFluidTank();
        float totalUnits = AirtightReactorKettleUtils.getTotalFluidUnits(inputTank, outputTank, partialTicks);
        int totalCapacity = AirtightReactorKettleUtils.getTotalFluidCapacity(inputTank, outputTank);
        if (totalUnits < 1 || totalCapacity <= 0) {
            return 0;
        }

        ms.pushPose();
        ms.translate(0, -1, 0);
        float fluidLevel = Mth.clamp(totalUnits / totalCapacity, 0, 1);
        fluidLevel = 1 - (1 - fluidLevel) * (1 - fluidLevel);
        float xMin = -0.875f;
        float yMin = 0.125f;
        float yMax = yMin + (0.875f - yMin) * fluidLevel;
        float zMax = 1.875f;
        xMin = renderFluidTank(inputTank, totalUnits, partialTicks, xMin, yMin, yMax, zMax, ms, buffer, light);
        renderFluidTank(outputTank, totalUnits, partialTicks, xMin, yMin, yMax, zMax, ms, buffer, light);

        ms.popPose();
        return yMax;
    }

    private static float renderFluidTank(SmartFluidTankBehaviour behaviour, float totalUnits, float partialTicks, float xMin, float yMin, float yMax, float zMax, PoseStack ms, MultiBufferSource buffer, int light) {
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
            float xMax = xMin + partial * 2.75f;
            NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(renderedFluid, xMin, yMin, -0.875f, xMax, yMax, zMax, buffer, ms, light, false, true);
            xMin = xMax;
        }
        return xMin;
    }

    private static void renderItems(AirtightReactorKettleBlockEntity kettle, float fluidLevel, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ms.pushPose();
        ms.translate(0.5f, -0.8f, 0.5f);
        float blockRotation = (kettle.getBlockPos().hashCode() & 255) * 1.40625f;
        TransformStack.of(ms).rotateYDegrees(kettle.getIngredientRotation().getValue(partialTicks) + blockRotation);

        float itemSurfaceY = fluidLevel <= 0 ? 0.05f : fluidLevel - 0.13f;
        IItemHandlerModifiable inventory = kettle.getItemCapability();
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        ClientLevel level = Minecraft.getInstance().level;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ItemPlacement placement = ITEM_PLACEMENTS[slot % ITEM_PLACEMENTS.length];
            float angle = placement.angle();
            Vec3 itemPosition = placement.position();

            ms.pushPose();

            float itemOffset = 0.035f;
            if (!renderer.getModel(stack, null, null, 0).isGui3d()) {
                itemOffset -= 0.1f;
            }
            if (fluidLevel > 0) {
                itemOffset += Mth.sin(AnimationTickHolder.getRenderTime(kettle.getLevel()) / 12.0f + angle) * 0.025f;
            }
            ms.translate(itemPosition.x, itemSurfaceY + itemOffset, itemPosition.z);
            TransformStack.of(ms).rotateYDegrees(angle + 35).rotateXDegrees(90);

            int copies = getRenderedCopyCount(stack.getCount());
            for (int i = 0; i < copies; i++) {
                ms.pushPose();

                Vec3 offset = placement.copyOffsets()[i];
                ms.translate(offset.x, offset.y, offset.z);
                renderer.renderStatic(stack, ItemDisplayContext.GROUND, light, overlay, ms, buffer, level, 0);

                ms.popPose();
            }

            ms.popPose();
        }

        ms.popPose();
    }

    private static void renderMixerModels(AirtightReactorKettleBlockEntity kettle, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
        BlockState state = kettle.getBlockState();
        SuperByteBuffer mixerModel = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_MIXER, state);
        float angle = kettle.getMixerRotation().getValue(partialTicks) * Mth.DEG_TO_RAD;
        float offset = kettle.getMixerOffset(partialTicks);
        mixerModel.translate(0, -offset, 0).rotateCentered(angle, Direction.UP).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    private static void renderWindowsModels(AirtightReactorKettleBlockEntity kettle, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
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

    private static ItemPlacement[] createItemPlacements() {
        ItemPlacement[] placements = new ItemPlacement[MAX_RENDERED_ITEM_SLOTS];
        List<Vec3> occupiedPositions = new ArrayList<>(MAX_RENDERED_ITEM_SLOTS);
        for (int slot = 0; slot < MAX_RENDERED_ITEM_SLOTS; slot++) {
            ItemPlacement placement = pickSeparatedItemPlacement(slot, occupiedPositions);
            placements[slot] = placement;
            occupiedPositions.add(placement.position());
        }
        return placements;
    }

    private static ItemPlacement pickSeparatedItemPlacement(int slot, List<Vec3> occupiedPositions) {
        RandomSource random = RandomSource.create(31L + slot * 9973L);
        Vec3 bestPosition = Vec3.ZERO;
        float bestAngle = 0;
        double bestScore = -Double.MAX_VALUE;
        for (int i = 0; i < 24; i++) {
            float angle = random.nextFloat() * 360.0f;
            float radiusRandom = random.nextFloat();
            float radius = Mth.lerp(radiusRandom * radiusRandom, MIN_RADIUS, MAX_RADIUS);
            if (random.nextFloat() < 0.25f) {
                radius = Mth.lerp(random.nextFloat(), MIN_RADIUS, MAX_RADIUS);
            }

            Vec3 candidate = VecHelper.rotate(new Vec3(radius, 0, 0), angle, Axis.Y);
            double nearestDistanceSqr = MAX_RADIUS * MAX_RADIUS;
            for (Vec3 occupied : occupiedPositions) {
                double dx = candidate.x - occupied.x;
                double dz = candidate.z - occupied.z;
                nearestDistanceSqr = Math.min(nearestDistanceSqr, dx * dx + dz * dz);
            }

            double preferredRadius = MAX_RADIUS * 0.55f;
            double radiusPenalty = Math.abs(radius - preferredRadius) * 0.04f;
            double score = nearestDistanceSqr - radiusPenalty + random.nextDouble() * 0.025f;
            if (score > bestScore) {
                bestScore = score;
                bestPosition = candidate;
                bestAngle = angle;
            }
        }

        Vec3[] copyOffsets = new Vec3[MAX_RENDERED_COPIES];
        copyOffsets[0] = Vec3.ZERO;
        for (int copy = 1; copy < copyOffsets.length; copy++) {
            copyOffsets[copy] = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.0625f);
        }
        return new ItemPlacement(bestPosition, bestAngle, copyOffsets);
    }

    private static int getRenderedCopyCount(int count) {
        if (count <= 1) {
            return 1;
        }
        if (count <= 8) {
            return 2;
        }
        if (count <= 32) {
            return 3;
        }
        return MAX_RENDERED_COPIES;
    }

    @Override
    protected void renderSafe(AirtightReactorKettleBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        renderMixerModels(be, partialTicks, ms, buffer, light);
        float fluidLevel = renderFluids(be, partialTicks, ms, buffer, light);
        renderWindowsModels(be, partialTicks, ms, buffer, light);
        renderItems(be, fluidLevel, partialTicks, ms, buffer, light, overlay);
    }

    private record ItemPlacement(Vec3 position, float angle, Vec3[] copyOffsets) {}
}
