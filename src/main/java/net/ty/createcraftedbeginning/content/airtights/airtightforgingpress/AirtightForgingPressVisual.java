package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressVisual extends AbstractBlockEntityVisual<AirtightForgingPressBlockEntity> implements SimpleDynamicVisual {
    protected final TransformedInstance head;
    protected float lastDistance = Float.NaN;

    public AirtightForgingPressVisual(VisualizationContext context, AirtightForgingPressBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        head = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.AIRTIGHT_FORGING_PRESS_PRESS_HEAD)).createInstance();
        animate(partialTick);
    }

    protected void animate(float partialTick) {
        float distance = blockEntity.getPressHeadDistance(partialTick);
        if (distance == lastDistance) {
            return;
        }

        head.setIdentityTransform().translate(getVisualPosition()).translateY(-distance).setChanged();
        lastDistance = distance;
    }

    @Override
    public void beginFrame(Context context) {
        if (!isVisible(context.frustum()) || doDistanceLimitThisFrame(context)) {
            return;
        }

        animate(context.partialTick());
    }

    @Override
    public void updateLight(float partialTick) {
        relight(head);
    }

    @Override
    protected void _delete() {
        head.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(head);
    }
}
