package net.ty.createcraftedbeginning.mixin.client.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
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
@Mixin(value = FrogportRenderer.class, remap = false)
public abstract class FrogportRendererMixin {
    @Inject(method = "renderPackage", at = @At("HEAD"), cancellable = true)
    private static void ccb$renderPackage(FrogportBlockEntity blockEntity, PoseStack ms, MultiBufferSource buffer, int light, int overlay, Vec3 diff, float scale, float itemDistance, CallbackInfo ci) {
        ItemStack boxItem = blockEntity.animatedPackage;
        if (boxItem == null || !BalloonUtils.isBalloon(boxItem)) {
            return;
        }

        if (scale < 0.45) {
            ci.cancel();
            return;
        }

        ResourceLocation key = BuiltInRegistries.ITEM.getKey(boxItem.getItem());
        if (key == BuiltInRegistries.ITEM.getDefaultKey()) {
            ci.cancel();
            return;
        }

        BlockState state = blockEntity.getBlockState();
        boolean depositing = blockEntity.currentlyDepositing;
        float hookDistance = BalloonStyleUtils.getFrogportHookDistance(diff, itemDistance, boxItem);
        float boxDistance = BalloonStyleUtils.getFrogportBoxDistance(diff, itemDistance, boxItem);
        float baseY = BalloonStyleUtils.getFrogportBaseY(diff, itemDistance, depositing);
        Vec3 offset = BalloonStyleUtils.getPackageOffset(diff, itemDistance, blockEntity.isAnimationInProgress() && depositing);

        SuperByteBuffer boxBuffer = CachedBuffers.partial(AllPartialModels.PACKAGES.get(key), state);
        boxBuffer.translate(0, baseY, 0).translate(offset).center().scale(scale).uncenter().translate(0, hookDistance + boxDistance, 0).light(light).overlay(overlay).renderInto(ms, buffer.getBuffer(RenderType.cutout()));
        if (depositing) {
            SuperByteBuffer rigBuffer = CachedBuffers.partial(AllPartialModels.PACKAGE_RIGGING.get(key), state);
            rigBuffer.translate(0, baseY, 0).translate(offset).center().scale(scale).uncenter().translate(0, hookDistance, 0).light(light).overlay(overlay).renderInto(ms, buffer.getBuffer(RenderType.cutout()));
        }

        ci.cancel();
    }
}