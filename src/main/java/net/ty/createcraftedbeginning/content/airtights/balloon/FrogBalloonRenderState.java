package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record FrogBalloonRenderState(ResourceLocation model, FrogBalloonPose pose, float scale, boolean depositing, boolean visible) {
    static @Nullable FrogBalloonRenderState create(FrogportBlockEntity port, Vec3 travel, float scale, float distance) {
        ItemStack item = port.animatedPackage;
        if (item == null || item.isEmpty() || !BalloonItem.isBalloon(item)) {
            return null;
        }

        ResourceLocation model = BuiltInRegistries.ITEM.getKey(item.getItem());
        boolean visible = scale >= 0.45 && !model.equals(BuiltInRegistries.ITEM.getDefaultKey());
        boolean depositing = port.currentlyDepositing;
        FrogBalloonPose pose = FrogBalloonPose.calculate(travel, distance, item, depositing, port.isAnimationInProgress());
        float balloonScale = visible ? BalloonRenderHelper.getLinearScale(item, port.getLevel(), port.getBlockPos()) : 1;
        return new FrogBalloonRenderState(model, pose, scale * balloonScale, depositing, visible);
    }
}
