package net.ty.createcraftedbeginning.content.opticalpower.amethystloader;

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
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystLoaderVisual extends AbstractBlockEntityVisual<AmethystLoaderBlockEntity> implements SimpleDynamicVisual {
    private final TransformedInstance core;

    public AmethystLoaderVisual(VisualizationContext context, AmethystLoaderBlockEntity loader, float partialTick) {
        super(context, loader, partialTick);
        core = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.AMETHYST_LOADER_CORE)).createInstance();
        updateLight(partialTick);
        animate(partialTick);
    }

    @Override
    public void beginFrame(Context context) {
        animate(context.partialTick());
    }

    @Override
    public void updateLight(float partialTick) {
        relight(core);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(core);
    }

    @Override
    protected void _delete() {
        core.delete();
    }

    private void animate(float partialTick) {
        core.setIdentityTransform().translate(getVisualPosition()).translate(Translate.CENTER).rotateY(blockEntity.animationAngle(partialTick) * Mth.DEG_TO_RAD).translateBack(Translate.CENTER).setChanged();
    }
}
