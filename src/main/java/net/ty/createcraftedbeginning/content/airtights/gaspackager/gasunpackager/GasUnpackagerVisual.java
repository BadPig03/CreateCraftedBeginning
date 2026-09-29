package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasunpackager;

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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasUnpackagerVisual extends AbstractBlockEntityVisual<GasUnpackagerBlockEntity> implements SimpleDynamicVisual {
    private final TransformedInstance hatch;
    private final TransformedInstance tray;

    private float lastTrayOffset = Float.NaN;
    private PartialModel lastHatchPartial;

    public GasUnpackagerVisual(VisualizationContext context, GasUnpackagerBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = blockState.getValue(GasUnpackagerBlock.FACING).getOpposite();

        lastHatchPartial = GasUnpackagerRenderer.getHatchModel(blockEntity);
        hatch = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(lastHatchPartial)).createInstance();
        tray = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(GasUnpackagerRenderer.getTrayModel())).createInstance();
        hatch.setIdentityTransform().translate(getVisualPosition()).translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.5)).rotateYCenteredDegrees(AngleHelper.horizontalAngle(facing)).rotateXCenteredDegrees(AngleHelper.verticalAngle(facing)).setChanged();

        animate(partialTick);
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
    }

    @Override
    public void updateLight(float partialTick) {
        relight(hatch, tray);
    }

    @Override
    protected void _delete() {
        hatch.delete();
        tray.delete();
    }

    @Override
    public void beginFrame(Context context) {
        animate(context.partialTick());
    }

    private void animate(float partialTick) {
        PartialModel hatchPartial = GasUnpackagerRenderer.getHatchModel(blockEntity);
        if (hatchPartial != lastHatchPartial) {
            instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(hatchPartial)).stealInstance(hatch);
            lastHatchPartial = hatchPartial;
        }

        float trayOffset = blockEntity.getTrayOffset(partialTick);
        if (Float.compare(trayOffset, lastTrayOffset) == 0) {
            return;
        }

        Direction facing = blockState.getValue(GasUnpackagerBlock.FACING).getOpposite();
        tray.setIdentityTransform().translate(getVisualPosition()).translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(trayOffset)).rotateYCenteredDegrees(facing.toYRot()).setChanged();
        lastTrayOffset = trayOffset;
    }
}
