package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionChamberVisual extends AbstractBlockEntityVisual<GasInjectionChamberBlockEntity> implements SimpleDynamicVisual {
    protected final TransformedInstance nozzle;
    protected final TransformedInstance nozzleTop;
    protected final TransformedInstance nozzleBottom;

    @Nullable
    protected TransformedInstance filter;
    @Nullable
    protected TransformedInstance filterInner;

    protected float lastNozzleOffset = Float.NaN;
    protected float lastPartOffset = Float.NaN;
    protected int lastFilterColor = Integer.MIN_VALUE;

    public GasInjectionChamberVisual(VisualizationContext context, GasInjectionChamberBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        nozzle = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE)).createInstance();
        nozzleTop = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE_TOP)).createInstance();
        nozzleBottom = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE_BOTTOM)).createInstance();
        animate(partialTick);
    }

    protected void animate(float partialTick) {
        ItemStack installedFilter = blockEntity.getInstalledFilter();
        boolean filterInstancesChanged = updateFilterInstances(installedFilter);

        float ticks = blockEntity.getRenderedProcessingTicks(partialTick);
        float nozzleOffset = GasInjectionChamberRenderer.getNozzleSqueeze(ticks);
        float partOffset = GasInjectionChamberRenderer.getNozzleSqueezePart(ticks);
        if (filterInstancesChanged || nozzleOffset != lastNozzleOffset || partOffset != lastPartOffset) {
            updateTransforms(nozzleOffset, partOffset);
            lastNozzleOffset = nozzleOffset;
            lastPartOffset = partOffset;
        }

        if (filterInner == null) {
            return;
        }

        int color = installedFilter.getOrDefault(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_COLOR, 0xFFFFFFFF);
        if (color == lastFilterColor) {
            return;
        }

        filterInner.color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 0xFF);
        filterInner.setChanged();
        lastFilterColor = color;
    }

    protected boolean updateFilterInstances(ItemStack installedFilter) {
        if (installedFilter.isEmpty()) {
            if (filter == null || filterInner == null) {
                return false;
            }

            filter.delete();
            filterInner.delete();
            filter = null;
            filterInner = null;
            lastFilterColor = Integer.MIN_VALUE;
            return true;
        }

        if (filter != null && filterInner != null) {
            return false;
        }

        filter = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.GAS_INJECTION_CHAMBER_FILTER)).createInstance();
        filterInner = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.GAS_INJECTION_CHAMBER_FILTER_INNER)).createInstance();
        relight(filter, filterInner);
        lastFilterColor = Integer.MIN_VALUE;
        return true;
    }

    protected void updateTransforms(float nozzleOffset, float partOffset) {
        nozzle.setIdentityTransform().translate(getVisualPosition()).translateY(nozzleOffset).setChanged();

        float attachedOffset = nozzleOffset + partOffset;
        nozzleTop.setIdentityTransform().translate(getVisualPosition()).translateY(attachedOffset).setChanged();
        nozzleBottom.setIdentityTransform().translate(getVisualPosition()).translateY(attachedOffset).setChanged();
        if (filter == null || filterInner == null) {
            return;
        }

        filter.setIdentityTransform().translate(getVisualPosition()).translateY(attachedOffset).setChanged();
        filterInner.setIdentityTransform().translate(getVisualPosition()).translateY(attachedOffset).setChanged();
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
        relight(nozzle, nozzleTop, nozzleBottom, filter, filterInner);
    }

    @Override
    protected void _delete() {
        nozzle.delete();
        nozzleTop.delete();
        nozzleBottom.delete();
        if (filter == null || filterInner == null) {
            return;
        }

        filter.delete();
        filterInner.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(nozzle);
        consumer.accept(nozzleTop);
        consumer.accept(nozzleBottom);
        if (filter == null || filterInner == null) {
            return;
        }

        consumer.accept(filter);
        consumer.accept(filterInner);
    }
}
