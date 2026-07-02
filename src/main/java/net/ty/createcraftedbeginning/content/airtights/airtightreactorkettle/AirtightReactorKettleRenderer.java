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
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleRenderer extends SmartBlockEntityRenderer<AirtightReactorKettleBlockEntity> {
    private static final float MIN_RADIUS = 0.08f;
    private static final float MAX_RADIUS = 1.1f;

    public AirtightReactorKettleRenderer(Context context) {
        super(context);
    }

    private static float renderFluids(AirtightReactorKettleBlockEntity kettle, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
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
                NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(renderedFluid, xMin, yMin, -0.875f, xMax, yMax, zMax, buffer, ms, light, false, true);
                xMin = xMax;
            }
        }

        ms.popPose();
        return yMax;
    }

    private static void renderItems(AirtightReactorKettleBlockEntity kettle, float fluidLevel, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ms.pushPose();
        ms.translate(0.5f, -0.8f, 0.5f);
        TransformStack.of(ms).rotateYDegrees(kettle.getIngredientRotation().getValue(partialTicks));

        int hashCode = kettle.getBlockPos().hashCode();
        float itemSurfaceY = fluidLevel <= 0 ? 0.05f : fluidLevel - 0.13f;
        IItemHandlerModifiable inventory = kettle.getItemCapability();
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        ClientLevel level = Minecraft.getInstance().level;
        List<Vec3> occupiedPositions = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            RandomSource random = RandomSource.create(hashCode + slot * 31L);
            ItemPlacement placement = pickSeparatedItemPlacement(hashCode, slot, itemSurfaceY, occupiedPositions);
            float angle = placement.angle;
            Vec3 itemPosition = placement.position;
            occupiedPositions.add(itemPosition);

            ms.pushPose();

            float itemOffset = 0.035f;
            if (!renderer.getModel(stack, null, null, 0).isGui3d()) {
                itemOffset -= 0.1f;
            }
            if (fluidLevel > 0) {
                itemOffset += Mth.sin(AnimationTickHolder.getRenderTime(kettle.getLevel()) / 12.0f + angle) * 0.025f;
            }
            ms.translate(itemPosition.x, itemPosition.y + itemOffset, itemPosition.z);
            TransformStack.of(ms).rotateYDegrees(angle + 35).rotateXDegrees(90);

            for (int i = 0; i <= stack.getCount() / 8; i++) {
                ms.pushPose();

                Vec3 vec = Vec3.ZERO;
                if (i > 0) {
                    vec = VecHelper.offsetRandomly(vec, random, 0.0625f);
                }

                ms.translate(vec.x, vec.y, vec.z);
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

    @Contract("_, _, _, _ -> new")
    private static ItemPlacement pickSeparatedItemPlacement(int blockHash, int slot, float y, List<Vec3> occupiedPositions) {
        RandomSource random = RandomSource.create(blockHash * 31L + slot * 9973L);
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

            Vec3 candidate = VecHelper.rotate(new Vec3(radius, y, 0), angle, Axis.Y);
            double nearestDistanceSqr = Double.MAX_VALUE;
            for (Vec3 occupied : occupiedPositions) {
                double dx = candidate.x - occupied.x;
                double dz = candidate.z - occupied.z;
                double distanceSqr = dx * dx + dz * dz;

                nearestDistanceSqr = Math.min(nearestDistanceSqr, distanceSqr);
            }

            if (occupiedPositions.isEmpty()) {
                nearestDistanceSqr = MAX_RADIUS * MAX_RADIUS;
            }
            double separationScore = nearestDistanceSqr;
            double preferredRadius = MAX_RADIUS * 0.55f;
            double radiusPenalty = Math.abs(radius - preferredRadius) * 0.04f;
            double noise = random.nextDouble() * 0.025f;
            double score = separationScore - radiusPenalty + noise;
            if (score > bestScore) {
                bestScore = score;
                bestPosition = candidate;
                bestAngle = angle;
            }
        }

        return new ItemPlacement(bestPosition, bestAngle);
    }

    @Override
    protected void renderSafe(AirtightReactorKettleBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        renderMixerModels(be, partialTicks, ms, buffer, light);
        float fluidLevel = renderFluids(be, partialTicks, ms, buffer, light);
        renderWindowsModels(be, partialTicks, ms, buffer, light);
        renderItems(be, fluidLevel, partialTicks, ms, buffer, light, overlay);
    }

    @Override
    public boolean shouldRenderOffScreen(AirtightReactorKettleBlockEntity kettle) {
        return true;
    }

    private record ItemPlacement(Vec3 position, float angle) {}
}
