package net.ty.createcraftedbeginning.mixin.create;

import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage.ChainConveyorPackagePhysicsData;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorVisual;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.util.SmartRecycler;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonStyles;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(ChainConveyorVisual.class)
public abstract class ChainConveyorVisualMixin extends SingleAxisRotatingVisual<ChainConveyorBlockEntity> {
    @Shadow
    @Final
    public SmartRecycler<ResourceLocation, TransformedInstance> boxes;
    @Shadow
    @Final
    public SmartRecycler<ResourceLocation, TransformedInstance> rigging;

    private ChainConveyorVisualMixin(VisualizationContext context, ChainConveyorBlockEntity blockEntity, float partialTick, Model model) {
        super(context, blockEntity, partialTick, model);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Inject(method = "setupBoxVisual", at = @At("HEAD"), cancellable = true)
    private void ccb$setupBoxVisual(ChainConveyorBlockEntity be, ChainConveyorPackage box, float partialTicks, CallbackInfo ci) {
        if (box.worldPosition == null) {
            return;
        }

        ItemStack boxItem = box.item;
        if (boxItem == null || boxItem.isEmpty() || !(boxItem.getItem() instanceof BalloonItem)) {
            return;
        }

        Level level = be.getLevel();
        if (level == null) {
            return;
        }

        ChainConveyorPackagePhysicsData physicsData = box.physicsData(be.getLevel());
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

        TransformedInstance rigBuffer = rigging.get(physicsData.modelKey);
        TransformedInstance boxBuffer = boxes.get(physicsData.modelKey);
        float yaw = AngleHelper.angleLerp(partialTicks, physicsData.prevYaw, physicsData.yaw);
        Vec3 dangleDiff = VecHelper.rotate(targetPosition.add(0, 0.5, 0).subtract(position), -yaw, Axis.Y);
        float zRot = Mth.clamp(Mth.wrapDegrees((float) Mth.atan2(-dangleDiff.x, dangleDiff.y) * Mth.RAD_TO_DEG) / 2, -25, 25);
        float xRot = Mth.clamp(Mth.wrapDegrees((float) Mth.atan2(dangleDiff.z, dangleDiff.y) * Mth.RAD_TO_DEG) / 2, -25, 25);
        int light = LightTexture.pack(level.getBrightness(LightLayer.BLOCK, containingPos), level.getBrightness(LightLayer.SKY, containingPos));

        for (TransformedInstance buf : new TransformedInstance[]{ rigBuffer, boxBuffer }) {
            buf.setIdentityTransform().translate(getVisualPosition()).translate(offset).translate(0, 0.625f, 0).rotateYDegrees(yaw).rotateZDegrees(zRot).rotateXDegrees(xRot);
            if (physicsData.flipped && buf == rigBuffer) {
                buf.rotateYDegrees(180);
            }
            buf.uncenter().translate(0, BalloonStyles.getHookDistance(boxItem), 0);
            if (buf == boxBuffer) {
                buf.translate(0, BalloonStyles.getBoxDistance(boxItem), 0);
            }
            buf.light(light).setChanged();
        }

        ci.cancel();
    }
}
