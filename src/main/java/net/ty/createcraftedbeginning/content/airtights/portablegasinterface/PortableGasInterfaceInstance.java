package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class PortableGasInterfaceInstance {
    private final InstancerProvider instancerProvider;
    private final BlockPos pos;
    private final float xRotation;
    private final float yRotation;

    TransformedInstance middle;
    TransformedInstance top;

    private boolean lit;

    PortableGasInterfaceInstance(InstancerProvider instancerProvider, BlockState blockState, BlockPos instancePos, boolean lit) {
        this.instancerProvider = instancerProvider;
        pos = instancePos;
        this.lit = lit;

        Direction facing = blockState.getValue(PortableGasInterfaceBlock.FACING);
        xRotation = switch (facing) {
            case UP -> 0;
            case DOWN -> 180;
            default -> 90;
        };
        yRotation = AngleHelper.horizontalAngle(facing);

        middle = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(PortableGasInterfaceRenderer.getMiddleForState(lit))).createInstance();
        top = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(PortableGasInterfaceRenderer.getTopForState())).createInstance();
    }

    void beginFrame(float extensionProgress) {
        applyBaseTransform(middle);
        middle.translate(0, extensionProgress * 0.5 + 0.375, 0);
        middle.setChanged();

        applyBaseTransform(top);
        top.translate(0, extensionProgress, 0);
        top.setChanged();
    }

    private void applyBaseTransform(TransformedInstance instance) {
        instance.setIdentityTransform().translate(pos).center().rotateYDegrees(yRotation).rotateXDegrees(xRotation).uncenter();
    }

    void tick(boolean lit) {
        if (this.lit == lit) {
            return;
        }

        this.lit = lit;
        instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(PortableGasInterfaceRenderer.getMiddleForState(lit))).stealInstance(middle);
    }

    void remove() {
        middle.delete();
        top.delete();
    }

    void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(middle);
        consumer.accept(top);
    }
}
