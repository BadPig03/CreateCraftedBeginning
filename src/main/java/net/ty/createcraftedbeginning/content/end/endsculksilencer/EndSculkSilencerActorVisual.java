package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.Translate;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class EndSculkSilencerActorVisual extends ActorVisual {
    private final TransformedInstance core;

    EndSculkSilencerActorVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext movementContext) {
        super(visualizationContext, simulationWorld, movementContext);
        core = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.END_SCULK_SILENCER_CORE)).createInstance();
        core.light(localBlockLight(), 0);
        animate();
    }

    @Override
    public void beginFrame() {
        animate();
    }

    @Override
    protected void _delete() {
        core.delete();
    }

    private void animate() {
        float angle = EndSculkSilencerMovementBehaviour.getAnimationAngle(context, AnimationTickHolder.getPartialTicks(context.world)) * Mth.DEG_TO_RAD;
        core.setIdentityTransform().translate(context.localPos).translateY(0.5f).translate(Translate.CENTER).rotateX(angle).rotateY(angle).rotateZ(Mth.PI / 4).translateBack(Translate.CENTER).setChanged();
    }
}
