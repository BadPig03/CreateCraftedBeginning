package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.Translate;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndSculkSilencerVisual extends AbstractBlockEntityVisual<EndSculkSilencerBlockEntity> implements SimpleDynamicVisual {
    private final TransformedInstance core;

    public EndSculkSilencerVisual(VisualizationContext context, EndSculkSilencerBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        core = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.END_SCULK_SILENCER_CORE)).createInstance();
        animate(partialTick);
    }

    private void animate(float partialTick) {
        float angle = blockEntity.getAnimation().getValue(partialTick) * Mth.DEG_TO_RAD;
        core.setIdentityTransform().translate(getVisualPosition()).translateY(0.5f).translate(Translate.CENTER).rotateX(angle).rotateY(angle).rotateZ(Mth.PI / 4).translateBack(Translate.CENTER).setChanged();
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
        relight(core);
    }

    @Override
    protected void _delete() {
        core.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(core);
    }
}
