package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer;

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
import net.minecraft.world.level.LightLayer;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class ChunkStabilizerActorVisual extends ActorVisual {
    private final TransformedInstance core;

    ChunkStabilizerActorVisual(VisualizationContext visualizationContext, VirtualRenderWorld renderWorld, MovementContext context) {
        super(visualizationContext, renderWorld, context);
        core = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.CHUNK_STABILIZER_CORE)).createInstance();
        core.light(localBlockLight(), simulationWorld.getBrightness(LightLayer.SKY, context.localPos));
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
        float angle = ChunkStabilizerMovementBehaviour.animation(context).angle(AnimationTickHolder.getPartialTicks(context.world));
        core.setIdentityTransform().translate(context.localPos).translate(Translate.CENTER).rotateY(angle * Mth.DEG_TO_RAD).translateBack(Translate.CENTER).setChanged();
    }
}
