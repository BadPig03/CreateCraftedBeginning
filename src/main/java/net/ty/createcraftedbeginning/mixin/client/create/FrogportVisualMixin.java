package net.ty.createcraftedbeginning.mixin.client.create;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonStyleUtils;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = FrogportVisual.class, remap = false)
public abstract class FrogportVisualMixin extends AbstractBlockEntityVisual<FrogportBlockEntity> {
    @Shadow
    @Final
    private TransformedInstance rig;

    @Shadow
    @Final
    private TransformedInstance box;

    private FrogportVisualMixin(VisualizationContext ctx, FrogportBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
    }

    @Inject(method = "renderPackage", at = @At("HEAD"), cancellable = true)
    private void ccb$renderPackage(Vec3 diff, float scale, float itemDistance, CallbackInfo ci) {
        ItemStack boxItem = blockEntity.animatedPackage;
        if (boxItem == null || boxItem.isEmpty() || !BalloonUtils.isBalloon(boxItem)) {
            return;
        }

        if (scale < 0.45) {
            rig.handle().setVisible(false);
            box.handle().setVisible(false);
            ci.cancel();
            return;
        }

        ResourceLocation key = BuiltInRegistries.ITEM.getKey(boxItem.getItem());
        if (key == BuiltInRegistries.ITEM.getDefaultKey()) {
            rig.handle().setVisible(false);
            box.handle().setVisible(false);
            ci.cancel();
            return;
        }

        boolean depositing = blockEntity.currentlyDepositing;
        float hookDistance = BalloonStyleUtils.getFrogportHookDistance(diff, itemDistance, boxItem);
        float boxDistance = BalloonStyleUtils.getFrogportBoxDistance(diff, itemDistance, boxItem);
        float baseY = BalloonStyleUtils.getFrogportBaseY(diff, itemDistance, depositing);
        BlockPos visualPos = getVisualPosition();
        Vec3 offset = BalloonStyleUtils.getPackageOffset(diff, itemDistance, blockEntity.isAnimationInProgress() && depositing);

        instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.PACKAGES.get(key))).stealInstance(box);
        box.handle().setVisible(true);
        box.setIdentityTransform().translate(visualPos).translate(0, baseY, 0).translate(offset).center().scale(scale).uncenter().translate(0, hookDistance + boxDistance, 0).setChanged();
        if (!depositing) {
            rig.handle().setVisible(false);
            ci.cancel();
            return;
        }

        instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.PACKAGE_RIGGING.get(key))).stealInstance(rig);
        rig.handle().setVisible(true);
        rig.setIdentityTransform().translate(visualPos).translate(0, baseY, 0).translate(offset).center().scale(scale).uncenter().translate(0, hookDistance, 0).setChanged();

        ci.cancel();
    }
}
