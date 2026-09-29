package net.ty.createcraftedbeginning.mixin.client.create;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractEntityVisual;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonEntityBehaviour;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonWorldPhysics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = PackageVisual.class, remap = false)
public abstract class PackageVisualMixin extends AbstractEntityVisual<PackageEntity> {
    @Shadow
    @Final
    public TransformedInstance instance;

    private PackageVisualMixin(VisualizationContext context, PackageEntity entity, float partialTick) {
        super(context, entity, partialTick);
    }

    @SuppressWarnings("ConstantExpression")
    @Inject(method = "animate", at = @At("TAIL"))
    private void ccb$animate(float partialTick, CallbackInfo callback) {
        if (!BalloonEntityBehaviour.isBalloon(entity)) {
            return;
        }

        float scale = BalloonWorldPhysics.of(entity.getBox(), entity.level(), entity.blockPosition()).linearScale();
        instance.translate(0.5F, 0, 0.5F).scale(scale).translate(-0.5F, 0, -0.5F).setChanged();
    }
}
