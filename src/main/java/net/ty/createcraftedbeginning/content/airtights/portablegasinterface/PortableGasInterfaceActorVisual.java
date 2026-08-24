package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PortableGasInterfaceActorVisual extends ActorVisual {
    protected final PortableGasInterfaceInstance instance;

    public PortableGasInterfaceActorVisual(VisualizationContext context, VirtualRenderWorld world, MovementContext movementContext) {
        super(context, world, movementContext);
        instance = new PortableGasInterfaceInstance(context.instancerProvider(), movementContext.state, movementContext.localPos, false);

        int blockLight = localBlockLight();
        instance.middle.light(blockLight, 0);
        instance.top.light(blockLight, 0);
    }

    @Override
    public void beginFrame() {
        LerpedFloat connectionAnimation = PortableGasInterfaceMovement.getAnimation(context);
        instance.tick(connectionAnimation.settled());
        instance.beginFrame(connectionAnimation.getValue(AnimationTickHolder.getPartialTicks()));
    }

    @Override
    protected void _delete() {
        instance.remove();
    }
}
