package net.ty.createcraftedbeginning.content.airtights.airvents;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
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
    private final TransformedInstance[] louvers = new TransformedInstance[Direction.values().length];
    private int visibleMask = -1;
    private int openedMask = -1;

    public AirVentVisual(VisualizationContext context, AirVentBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        syncLouvers();
    }

    private void syncLouvers() {
        int nextVisibleMask = blockEntity.getVisibleLouverMask();
        int nextOpenedMask = blockEntity.getOpenedLouverMask();
        if (nextVisibleMask == visibleMask && nextOpenedMask == openedMask) {
            return;
        }

        for (Direction direction : Direction.values()) {
            int index = direction.get3DDataValue();
            int mask = 1 << index;
            TransformedInstance louver = louvers[index];
            if ((nextVisibleMask & mask) == 0) {
                if (louver != null) {
                    louver.delete();
                    louvers[index] = null;
                }
                continue;
            }

            boolean opened = (nextOpenedMask & mask) != 0;
            PartialModel model = opened ? CCBPartialModels.AIR_VENT_OPENED : CCBPartialModels.AIR_VENT_CLOSED;
            if (louver == null) {
                louver = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(model)).createInstance();
                louvers[index] = louver;
                relight(louver);
            }
            else if (((openedMask & mask) == 0) == opened) {
                instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(model)).stealInstance(louver);
            }

            orientLouver(louver, direction);
        }

        visibleMask = nextVisibleMask;
        openedMask = nextOpenedMask;
    }

    private void orientLouver(TransformedInstance louver, Direction direction) {
        Direction facing = direction.getOpposite();
        louver.setIdentityTransform().translate(getVisualPosition()).translate(direction.getStepX() * LOUVER_SURFACE_OFFSET, direction.getStepY() * LOUVER_SURFACE_OFFSET, direction.getStepZ() * LOUVER_SURFACE_OFFSET).rotateYCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing))).rotateXCentered(AngleHelper.rad(AngleHelper.verticalAngle(facing))).setChanged();
    }

    @Override
    public void update(float partialTick) {
        syncLouvers();
    }

    @Override
    protected void _delete() {
        for (TransformedInstance louver : louvers) {
            if (louver == null) {
                continue;
            }

            louver.delete();
        }
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
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance louver : louvers) {
            if (louver == null) {
                continue;
            }

            consumer.accept(louver);
        }
    }
}
