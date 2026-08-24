package net.ty.createcraftedbeginning.content.airtights.airvents;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirVentVisual extends AbstractBlockEntityVisual<AirVentBlockEntity> implements SimpleDynamicVisual {
    private static final float LOUVER_SURFACE_OFFSET = 0.001953125f;

    protected final TransformedInstance[] louvers = new TransformedInstance[Direction.values().length];
    protected final TransformedInstance[] innerLouvers = new TransformedInstance[Direction.values().length];
    protected int visibleMask = -1;
    protected int openedMask = -1;

    public AirVentVisual(VisualizationContext context, AirVentBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        syncLouvers();
    }

    private static void deleteLouvers(TransformedInstance[] louvers) {
        for (TransformedInstance louver : louvers) {
            if (louver == null) {
                continue;
            }

            louver.delete();
        }
    }

    private static void collectCrumblingInstances(Consumer<Instance> consumer, TransformedInstance[] louvers) {
        for (TransformedInstance louver : louvers) {
            if (louver == null) {
                continue;
            }

            consumer.accept(louver);
        }
    }

    protected void syncLouvers() {
        int nextVisibleMask = blockEntity.getVisibleLouverMask();
        int nextOpenedMask = blockEntity.getOpenedLouverMask();
        if (nextVisibleMask == visibleMask && nextOpenedMask == openedMask) {
            return;
        }

        for (Direction direction : Iterate.directions) {
            int directionIndex = direction.get3DDataValue();
            int directionMask = 1 << directionIndex;
            TransformedInstance louver = louvers[directionIndex];
            TransformedInstance innerLouver = innerLouvers[directionIndex];
            if ((nextVisibleMask & directionMask) == 0) {
                if (louver != null) {
                    louver.delete();
                    louvers[directionIndex] = null;
                }
                if (innerLouver != null) {
                    innerLouver.delete();
                    innerLouvers[directionIndex] = null;
                }
                continue;
            }

            boolean isOpen = (nextOpenedMask & directionMask) != 0;
            boolean modelChanged = ((openedMask & directionMask) == 0) == isOpen;
            PartialModel louverModel = isOpen ? CCBPartialModels.AIR_VENT_OPENED : CCBPartialModels.AIR_VENT_CLOSED;
            if (louver == null) {
                louver = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(louverModel)).createInstance();
                louvers[directionIndex] = louver;
                relight(louver);
            }
            else if (modelChanged) {
                instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(louverModel)).stealInstance(louver);
            }
            if (innerLouver == null) {
                innerLouver = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(louverModel)).createInstance();
                innerLouvers[directionIndex] = innerLouver;
                relight(innerLouver);
            }
            else if (modelChanged) {
                instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(louverModel)).stealInstance(innerLouver);
            }

            orientLouver(louver, direction, LOUVER_SURFACE_OFFSET);
            orientLouver(innerLouver, direction, -LOUVER_SURFACE_OFFSET);
        }

        visibleMask = nextVisibleMask;
        openedMask = nextOpenedMask;
    }

    protected void orientLouver(TransformedInstance louver, Direction direction, float surfaceOffset) {
        Direction louverFacing = direction.getOpposite();
        louver.setIdentityTransform().translate(getVisualPosition()).translate(direction.getStepX() * surfaceOffset, direction.getStepY() * surfaceOffset, direction.getStepZ() * surfaceOffset).rotateYCentered(AngleHelper.rad(AngleHelper.horizontalAngle(louverFacing))).rotateXCentered(AngleHelper.rad(AngleHelper.verticalAngle(louverFacing))).setChanged();
    }

    @Override
    public void update(float partialTick) {
        syncLouvers();
    }

    @Override
    protected void _delete() {
        deleteLouvers(louvers);
        deleteLouvers(innerLouvers);
    }

    @Override
    public void beginFrame(Context context) {
        if (!isVisible(context.frustum()) || doDistanceLimitThisFrame(context)) {
            return;
        }

        syncLouvers();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(louvers);
        relight(innerLouvers);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        collectCrumblingInstances(consumer, louvers);
        collectCrumblingInstances(consumer, innerLouvers);
    }
}
