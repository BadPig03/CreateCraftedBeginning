package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import com.simibubi.create.content.logistics.packager.PackagerBlock;
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
public class GasRepackagerVisual extends AbstractBlockEntityVisual<GasRepackagerBlockEntity> implements SimpleDynamicVisual {
    public final TransformedInstance hatch;
    public final TransformedInstance tray;

    public float lastTrayOffset = Float.NaN;
    public PartialModel lastHatchPartial;

    public GasRepackagerVisual(VisualizationContext ctx, GasRepackagerBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
        Direction facing = blockState.getValue(PackagerBlock.FACING).getOpposite();
        lastHatchPartial = GasRepackagerRenderer.getHatchModel(blockEntity);
        hatch = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(lastHatchPartial)).createInstance();
        tray = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(GasRepackagerRenderer.getTrayModel(blockState))).createInstance();
        hatch.setIdentityTransform().translate(getVisualPosition()).translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.5)).rotateYCenteredDegrees(AngleHelper.horizontalAngle(facing)).rotateXCenteredDegrees(AngleHelper.verticalAngle(facing)).setChanged();
        animate(partialTick);
    }

    public void animate(float partialTick) {
        PartialModel hatchPartial = GasRepackagerRenderer.getHatchModel(blockEntity);
        if (hatchPartial != lastHatchPartial) {
            instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(hatchPartial)).stealInstance(hatch);
            lastHatchPartial = hatchPartial;
        }

        float trayOffset = blockEntity.getTrayOffset(partialTick);
        if (trayOffset == lastTrayOffset) {
            return;
        }

        Direction facing = blockState.getValue(PackagerBlock.FACING).getOpposite();
        tray.setIdentityTransform().translate(getVisualPosition()).translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(trayOffset)).rotateYCenteredDegrees(facing.toYRot()).setChanged();
        lastTrayOffset = trayOffset;
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
    public void beginFrame(Context ctx) {
        animate(ctx.partialTick());
    }
}
