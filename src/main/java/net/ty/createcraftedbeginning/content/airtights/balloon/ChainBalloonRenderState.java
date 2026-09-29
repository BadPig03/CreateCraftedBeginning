package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage.ChainConveyorPackagePhysicsData;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record ChainBalloonRenderState(ItemStack item, ResourceLocation model, ChainBalloonPose pose, int light, float scale, boolean flipped) {
    static @Nullable ChainBalloonRenderState create(ChainConveyorBlockEntity conveyor, ChainConveyorPackage box, BlockPos origin, float partialTicks) {
        ItemStack item = box.item;
        Level level = conveyor.getLevel();
        if (box.worldPosition == null || item == null || item.isEmpty() || !BalloonItem.isBalloon(item) || level == null) {
            return null;
        }

        ChainConveyorPackagePhysicsData physics = box.physicsData(level);
        Vec3 previous = physics.prevPos;
        if (previous == null) {
            return null;
        }

        ResourceLocation model = physics.modelKey;
        if (model == null) {
            model = BuiltInRegistries.ITEM.getKey(item.getItem());
        }
        if (model == BuiltInRegistries.ITEM.getDefaultKey()) {
            return null;
        }

        physics.modelKey = model;
        Vec3 position = previous.lerp(physics.pos, partialTicks);
        Vec3 target = physics.prevTargetPos.lerp(physics.targetPos, partialTicks);
        float yaw = AngleHelper.angleLerp(partialTicks, physics.prevYaw, physics.yaw);
        BlockPos lightPos = BlockPos.containing(position);
        int light = LightTexture.pack(level.getBrightness(LightLayer.BLOCK, lightPos), level.getBrightness(LightLayer.SKY, lightPos));
        float scale = BalloonRenderHelper.getLinearScale(item, level, lightPos);
        return new ChainBalloonRenderState(item, model, ChainBalloonPose.calculate(position, target, yaw, origin), light, scale, physics.flipped);
    }
}
