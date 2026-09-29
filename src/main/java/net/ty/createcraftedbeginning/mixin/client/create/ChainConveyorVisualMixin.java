package net.ty.createcraftedbeginning.mixin.client.create;

import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorVisual;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.util.SmartRecycler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.content.airtights.balloon.ChainBalloonRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = ChainConveyorVisual.class, remap = false)
public abstract class ChainConveyorVisualMixin extends SingleAxisRotatingVisual<ChainConveyorBlockEntity> {
    @Shadow
    @Final
    private SmartRecycler<ResourceLocation, TransformedInstance> boxes;
    @Shadow
    @Final
    private SmartRecycler<ResourceLocation, TransformedInstance> rigging;

    private ChainConveyorVisualMixin(VisualizationContext context, ChainConveyorBlockEntity blockEntity, float partialTick, Model model) {
        super(context, blockEntity, partialTick, model);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Inject(method = "setupBoxVisual", at = @At("HEAD"), cancellable = true)
    private void ccb$setupBoxVisual(ChainConveyorBlockEntity conveyor, ChainConveyorPackage box, float partialTicks, CallbackInfo callback) {
        if (!ChainBalloonRenderer.renderVisual(conveyor, box, partialTicks, pos, getVisualPosition(), rigging, boxes)) {
            return;
        }

        callback.cancel();
    }
}
