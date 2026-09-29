package net.ty.createcraftedbeginning.mixin.client.create;

import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.balloon.FrogBalloonRenderer;
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

    private FrogportVisualMixin(VisualizationContext context, FrogportBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
    }

    @Inject(method = "renderPackage", at = @At("HEAD"), cancellable = true)
    private void ccb$renderPackage(Vec3 diff, float scale, float itemDistance, CallbackInfo callback) {
        if (!FrogBalloonRenderer.renderVisual(blockEntity, diff, scale, itemDistance, getVisualPosition(), instancerProvider(), rig, box)) {
            return;
        }

        callback.cancel();
    }
}
