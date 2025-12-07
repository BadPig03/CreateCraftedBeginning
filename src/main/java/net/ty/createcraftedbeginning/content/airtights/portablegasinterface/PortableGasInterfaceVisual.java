package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PortableGasInterfaceVisual extends AbstractBlockEntityVisual<PortableGasInterfaceBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {
    private final PortableGasInterfaceInstance instance;

    public PortableGasInterfaceVisual(VisualizationContext visualizationContext, PortableGasInterfaceBlockEntity blockEntity, float partialTick) {
        super(visualizationContext, blockEntity, partialTick);

        instance = new PortableGasInterfaceInstance(visualizationContext.instancerProvider(), blockState, getVisualPosition(), isLit());
        instance.beginFrame(blockEntity.getExtensionDistance(partialTick));
    }

    private boolean isLit() {
        return blockEntity.isConnected();
    }

    @Override
    public void tick(TickableVisual.Context ctx) {
        instance.tick(isLit());
    }

    @Override
    public void beginFrame(DynamicVisual.@NotNull Context ctx) {
        instance.beginFrame(blockEntity.getExtensionDistance(ctx.partialTick()));
    }

    @Override
    public void updateLight(float partialTick) {
        relight(instance.middle, instance.top);
    }

    @Override
    protected void _delete() {
        instance.remove();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        instance.collectCrumblingInstances(consumer);
    }
}
