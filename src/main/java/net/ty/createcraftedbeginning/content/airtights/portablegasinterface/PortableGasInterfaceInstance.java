package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PortableGasInterfaceInstance {
    private final InstancerProvider instancerProvider;
    private final BlockPos instancePos;
    private final float angleX;
    private final float angleY;

    public TransformedInstance middle;
    public TransformedInstance top;

    private boolean lit;

    public PortableGasInterfaceInstance(@NotNull InstancerProvider instancerProvider, @NotNull BlockState blockState, BlockPos instancePos, boolean lit) {
        this.instancerProvider = instancerProvider;
        this.instancePos = instancePos;
        this.lit = lit;
        Direction facing = blockState.getValue(PortableGasInterfaceBlock.FACING);
        angleX = facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90;
        angleY = AngleHelper.horizontalAngle(facing);
        middle = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(PortableGasInterfaceRenderer.getMiddleForState(lit))).createInstance();
        top = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(PortableGasInterfaceRenderer.getTopForState())).createInstance();
    }

    public void beginFrame(float progress) {
        middle.setIdentityTransform().translate(instancePos).center().rotateYDegrees(angleY).rotateXDegrees(angleX).uncenter();
        middle.translate(0, progress * 0.5f + 0.375f, 0);
        middle.setChanged();
        top.setIdentityTransform().translate(instancePos).center().rotateYDegrees(angleY).rotateXDegrees(angleX).uncenter();
        top.translate(0, progress, 0);
        top.setChanged();
    }

    public void tick(boolean lit) {
        if (this.lit == lit) {
            return;
        }

        this.lit = lit;
        instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(PortableGasInterfaceRenderer.getMiddleForState(lit))).stealInstance(middle);
    }

    public void remove() {
        middle.delete();
        top.delete();
    }

    public void collectCrumblingInstances(@NotNull Consumer<Instance> consumer) {
        consumer.accept(middle);
        consumer.accept(top);
    }
}
