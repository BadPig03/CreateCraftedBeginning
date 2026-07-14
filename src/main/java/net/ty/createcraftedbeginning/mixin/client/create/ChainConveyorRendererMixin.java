package net.ty.createcraftedbeginning.mixin.client.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage.ChainConveyorPackagePhysicsData;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRenderer;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonStyleUtils;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = ChainConveyorRenderer.class, remap = false)
public abstract class ChainConveyorRendererMixin {
    @SuppressWarnings("SuspiciousNameCombination")
    @Inject(method = "renderBox", at = @At("HEAD"), cancellable = true)
    private void ccb$renderBox(ChainConveyorBlockEntity be, PoseStack ms, MultiBufferSource buffer, int overlay, BlockPos pos, ChainConveyorPackage box, float partialTicks, CallbackInfo ci) {
        if (box.worldPosition == null) {
            return;
        }

        ItemStack boxItem = box.item;
        if (boxItem == null || boxItem.isEmpty() || !BalloonUtils.isBalloon(boxItem)) {
            return;
        }

        Level level = be.getLevel();
        if (level == null) {
            return;
        }

        ChainConveyorPackagePhysicsData physicsData = box.physicsData(level);
        Vec3 prevPos = physicsData.prevPos;
        if (prevPos == null) {
            return;
        }

        if (physicsData.modelKey == null) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(boxItem.getItem());
            if (key == BuiltInRegistries.ITEM.getDefaultKey()) {
                return;
            }

            physicsData.modelKey = key;
        }

        Vec3 position = prevPos.lerp(physicsData.pos, partialTicks);
        BlockPos containingPos = BlockPos.containing(position);
        Vec3 targetPosition = physicsData.prevTargetPos.lerp(physicsData.targetPos, partialTicks);
        Vec3 offset = new Vec3(targetPosition.x - pos.getX(), targetPosition.y - pos.getY(), targetPosition.z - pos.getZ());
        BlockState blockState = be.getBlockState();

        SuperByteBuffer rigBuffer = CachedBuffers.partial(AllPartialModels.PACKAGE_RIGGING.get(physicsData.modelKey), blockState);
        SuperByteBuffer boxBuffer = CachedBuffers.partial(AllPartialModels.PACKAGES.get(physicsData.modelKey), blockState);
        float yaw = AngleHelper.angleLerp(partialTicks, physicsData.prevYaw, physicsData.yaw);
        Vec3 dangleDiff = VecHelper.rotate(targetPosition.add(0, 0.5, 0).subtract(position), -yaw, Axis.Y);
        float zRot = Mth.clamp(Mth.wrapDegrees((float) Mth.atan2(-dangleDiff.x, dangleDiff.y) * Mth.RAD_TO_DEG) / 2, -25, 25);
        float xRot = Mth.clamp(Mth.wrapDegrees((float) Mth.atan2(dangleDiff.z, dangleDiff.y) * Mth.RAD_TO_DEG) / 2, -25, 25);
        int light = LightTexture.pack(level.getBrightness(LightLayer.BLOCK, containingPos), level.getBrightness(LightLayer.SKY, containingPos));

        for (SuperByteBuffer buf : new SuperByteBuffer[]{rigBuffer, boxBuffer}) {
            buf.translate(offset).translate(0, 0.625f, 0).rotateYDegrees(yaw).rotateZDegrees(zRot).rotateXDegrees(xRot);
            if (physicsData.flipped && buf == rigBuffer) {
                buf.rotateYDegrees(180);
            }
            buf.uncenter().translate(0, BalloonStyleUtils.getHookDistance(boxItem), 0);
            if (buf == boxBuffer) {
                buf.translate(0, BalloonStyleUtils.getBoxDistance(boxItem), 0);
            }
            buf.light(light).overlay(overlay).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        }

        ci.cancel();
    }
}
