package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleRenderer extends SmartBlockEntityRenderer<AirtightReactorKettleBlockEntity> {
    private static final float MIN_RADIUS = 0.08f;
    private static final float MAX_RADIUS = 1.1f;
    private static final int MAX_RENDERED_ITEM_SLOTS = 64;
    private static final double FULL_ITEM_DETAIL_DISTANCE_SQR = 256;
    private static final ItemPlacement[] ITEM_PLACEMENTS = createItemPlacements();

    public AirtightReactorKettleRenderer(Context context) {
        super(context);
    }

    private static float renderFluids(AirtightReactorKettleBlockEntity kettle, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        SmartFluidTankBehaviour inputTank = kettle.getInputFluidTank();
        SmartFluidTankBehaviour outputTank = kettle.getOutputFluidTank();
        float totalUnits = AirtightReactorKettleUtils.getTotalFluidUnits(inputTank, outputTank, partialTicks);
        int totalCapacity = AirtightReactorKettleUtils.getTotalFluidCapacity(inputTank, outputTank);
        if (totalUnits < 1 || totalCapacity <= 0) {
            return 0;
        }

        poseStack.pushPose();
        poseStack.translate(0, -1, 0);
        float fluidLevel = CCBMathUtils.clampUnit(totalUnits / totalCapacity);
        fluidLevel = 1 - (1 - fluidLevel) * (1 - fluidLevel);
        float xMin = -0.875f;
        float yMin = 0.125f;
        float yMax = yMin + (0.875f - yMin) * fluidLevel;
        float zMax = 1.875f;
        xMin = renderFluidTank(inputTank, totalUnits, partialTicks, xMin, yMin, yMax, zMax, poseStack, buffer, light);
        renderFluidTank(outputTank, totalUnits, partialTicks, xMin, yMin, yMax, zMax, poseStack, buffer, light);

        poseStack.popPose();
        return yMax;
    }

    private static float renderFluidTank(SmartFluidTankBehaviour tankBehaviour, float totalUnits, float partialTicks, float xMin, float yMin, float yMax, float zMax, PoseStack poseStack, MultiBufferSource buffer, int light) {
        for (TankSegment tankSegment : tankBehaviour.getTanks()) {
            FluidStack renderedFluid = tankSegment.getRenderedFluid();
            if (renderedFluid.isEmpty()) {
                continue;
            }

            float fluidUnits = tankSegment.getTotalUnits(partialTicks);
            if (fluidUnits < 1) {
                continue;
            }

            float widthFraction = CCBMathUtils.clampUnit(fluidUnits / totalUnits);
            float xMax = xMin + widthFraction * 2.75f;
            NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(renderedFluid, xMin, yMin, -0.875f, xMax, yMax, zMax, buffer, poseStack, light, false, true);
            xMin = xMax;
        }
        return xMin;
    }

    private static void renderItems(AirtightReactorKettleBlockEntity kettle, float fluidLevel, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, -0.8, 0.5);
        float blockRotation = (kettle.getBlockPos().hashCode() & 255) * 1.40625f;
        TransformStack.of(poseStack).rotateYDegrees(kettle.getIngredientRotation().getValue(partialTicks) + blockRotation);

        float itemSurfaceY = fluidLevel <= 0 ? 0.05f : fluidLevel - 0.13f;
        IItemHandler items = kettle.getAvailableItems();
        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        ClientLevel clientLevel = minecraft.level;
        boolean shouldRenderSingleCopy = isBeyondFullItemDetailDistance(kettle, minecraft.cameraEntity);
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack itemStack = items.getStackInSlot(slot);
            if (itemStack.isEmpty()) {
                continue;
            }

            ItemPlacement itemPlacement = ITEM_PLACEMENTS[slot % ITEM_PLACEMENTS.length];
            float itemAngle = itemPlacement.angle();
            Vec3 itemPosition = itemPlacement.position();

            poseStack.pushPose();

            float itemOffset = 0.035f;
            if (!itemRenderer.getModel(itemStack, null, null, 0).isGui3d()) {
                itemOffset -= 0.1f;
            }
            if (fluidLevel > 0) {
                itemOffset += Mth.sin(AnimationTickHolder.getRenderTime(kettle.getLevel()) / 12 + itemAngle) * 0.025f;
            }
            poseStack.translate(itemPosition.x, itemSurfaceY + itemOffset, itemPosition.z);
            TransformStack.of(poseStack).rotateYDegrees(itemAngle + 35).rotateXDegrees(90);

            int copyCount = shouldRenderSingleCopy ? 1 : getRenderedCopyCount(itemStack.getCount());
            for (int copyIndex = 0; copyIndex < copyCount; copyIndex++) {
                poseStack.pushPose();

                Vec3 copyOffset = itemPlacement.copyOffsets()[copyIndex];
                poseStack.translate(copyOffset.x, copyOffset.y, copyOffset.z);
                itemRenderer.renderStatic(itemStack, ItemDisplayContext.GROUND, light, overlay, poseStack, buffer, clientLevel, 0);

                poseStack.popPose();
            }

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static boolean isBeyondFullItemDetailDistance(AirtightReactorKettleBlockEntity kettle, @Nullable Entity camera) {
        return camera != null && kettle.getLevel() == camera.level() && camera.position().distanceToSqr(VecHelper.getCenterOf(kettle.getBlockPos())) > FULL_ITEM_DETAIL_DISTANCE_SQR;
    }

    private static void renderMixerModels(AirtightReactorKettleBlockEntity kettle, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        SuperByteBuffer mixerModel = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_MIXER, kettle.getBlockState());
        float mixerAngle = kettle.getMixerRotation().getValue(partialTicks) * Mth.DEG_TO_RAD;
        float mixerOffset = kettle.getMixerOffset(partialTicks);
        mixerModel.translate(0, -mixerOffset, 0).rotateCentered(mixerAngle, Direction.UP).light(light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    private static void renderWindowsModels(AirtightReactorKettleBlockEntity kettle, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        BlockState blockState = kettle.getBlockState();
        SuperByteBuffer leftWindowModel = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_LEFT_WINDOW, blockState);
        SuperByteBuffer rightWindowModel = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_RIGHT_WINDOW, blockState);
        float windowDistance = kettle.getWindowDistance().getValue(partialTicks);
        for (Direction direction : Iterate.horizontalDirections) {
            Vec3i leftDistance = direction.getClockWise().getNormal();
            Vec3i rightDistance = direction.getCounterClockWise().getNormal();
            leftWindowModel.translate(direction.getNormal()).translate(leftDistance.getX() * windowDistance, leftDistance.getY() * windowDistance, leftDistance.getZ() * windowDistance).rotateYCenteredDegrees(AngleHelper.horizontalAngle(direction)).light(light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
            rightWindowModel.translate(direction.getNormal()).translate(rightDistance.getX() * windowDistance, rightDistance.getY() * windowDistance, rightDistance.getZ() * windowDistance).rotateYCenteredDegrees(AngleHelper.horizontalAngle(direction)).light(light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
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
        RandomSource random = RandomSource.create(31 + slot * 9973);
        Vec3 bestPosition = Vec3.ZERO;
        float bestAngle = 0;
        double bestScore = -Double.MAX_VALUE;
        for (int attempt = 0; attempt < 24; attempt++) {
            float candidateAngle = random.nextFloat() * 360;
            float radiusRandom = random.nextFloat();
            float radius = Mth.lerp(radiusRandom * radiusRandom, MIN_RADIUS, MAX_RADIUS);
            if (random.nextFloat() < 0.25f) {
                radius = Mth.lerp(random.nextFloat(), MIN_RADIUS, MAX_RADIUS);
            }

            Vec3 candidatePosition = VecHelper.rotate(new Vec3(radius, 0, 0), candidateAngle, Axis.Y);
            double nearestDistanceSqr = MAX_RADIUS * MAX_RADIUS;
            for (Vec3 occupiedPosition : occupiedPositions) {
                double dx = candidatePosition.x - occupiedPosition.x;
                double dz = candidatePosition.z - occupiedPosition.z;
                nearestDistanceSqr = Math.min(nearestDistanceSqr, dx * dx + dz * dz);
            }

            double preferredRadius = MAX_RADIUS * 0.55;
            double radiusPenalty = Math.abs(radius - preferredRadius) * 0.04;
            double score = nearestDistanceSqr - radiusPenalty + random.nextDouble() * 0.025;
            if (score <= bestScore) {
                continue;
            }

            bestScore = score;
            bestPosition = candidatePosition;
            bestAngle = candidateAngle;
        }

        Vec3[] copyOffsets = new Vec3[4];
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
        return 4;
    }

    @Override
    protected void renderSafe(AirtightReactorKettleBlockEntity kettle, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(kettle, partialTicks, poseStack, buffer, light, overlay);
        boolean useVisualization = VisualizationManager.supportsVisualization(kettle.getLevel());
        if (!useVisualization) {
            renderMixerModels(kettle, partialTicks, poseStack, buffer, light);
        }
        float fluidLevel = renderFluids(kettle, partialTicks, poseStack, buffer, light);
        if (!useVisualization) {
            renderWindowsModels(kettle, partialTicks, poseStack, buffer, light);
        }
        renderItems(kettle, fluidLevel, partialTicks, poseStack, buffer, light, overlay);
    }

    protected record ItemPlacement(Vec3 position, float angle, Vec3[] copyOffsets) {}
}
