package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleVisual extends AbstractBlockEntityVisual<AirtightReactorKettleBlockEntity> implements SimpleDynamicVisual {
    private final TransformedInstance mixer;
    private final TransformedInstance[] leftWindows = new TransformedInstance[Iterate.horizontalDirections.length];
    private final TransformedInstance[] rightWindows = new TransformedInstance[Iterate.horizontalDirections.length];

    private float lastMixerAngle = Float.NaN;
    private float lastMixerOffset = Float.NaN;
    private float lastWindowDistance = Float.NaN;

    public AirtightReactorKettleVisual(VisualizationContext context, AirtightReactorKettleBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        mixer = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_MIXER)).createInstance();
        for (int i = 0; i < Iterate.horizontalDirections.length; i++) {
            leftWindows[i] = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_LEFT_WINDOW)).createInstance();
            rightWindows[i] = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_RIGHT_WINDOW)).createInstance();
        }
        animate(partialTick);
    }

    private void animate(float partialTick) {
        float mixerAngle = blockEntity.getMixerRotation().getValue(partialTick) * Mth.DEG_TO_RAD;
        float mixerOffset = blockEntity.getMixerOffset(partialTick);
        if (mixerAngle != lastMixerAngle || mixerOffset != lastMixerOffset) {
            mixer.setIdentityTransform().translate(getVisualPosition()).translateY(-mixerOffset).rotateYCentered(mixerAngle).setChanged();
            lastMixerAngle = mixerAngle;
            lastMixerOffset = mixerOffset;
        }

        float windowDistance = blockEntity.getWindowDistance().getValue(partialTick);
        if (windowDistance == lastWindowDistance) {
            return;
        }

        for (int i = 0; i < Iterate.horizontalDirections.length; i++) {
            Direction direction = Iterate.horizontalDirections[i];
            Vec3i normal = direction.getNormal();
            Vec3i leftDistance = direction.getClockWise().getNormal();
            Vec3i rightDistance = direction.getCounterClockWise().getNormal();

            leftWindows[i].setIdentityTransform().translate(getVisualPosition()).translate(normal.getX(), normal.getY(), normal.getZ()).translate(leftDistance.getX() * windowDistance, leftDistance.getY() * windowDistance, leftDistance.getZ() * windowDistance).rotateYCenteredDegrees(AngleHelper.horizontalAngle(direction)).setChanged();
            rightWindows[i].setIdentityTransform().translate(getVisualPosition()).translate(normal.getX(), normal.getY(), normal.getZ()).translate(rightDistance.getX() * windowDistance, rightDistance.getY() * windowDistance, rightDistance.getZ() * windowDistance).rotateYCenteredDegrees(AngleHelper.horizontalAngle(direction)).setChanged();
        }
        lastWindowDistance = windowDistance;
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
        relight(mixer);
        for (int i = 0; i < leftWindows.length; i++) {
            relight(leftWindows[i], rightWindows[i]);
        }
    }

    @Override
    protected void _delete() {
        mixer.delete();
        for (int i = 0; i < leftWindows.length; i++) {
            leftWindows[i].delete();
            rightWindows[i].delete();
        }
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(mixer);
        for (int i = 0; i < leftWindows.length; i++) {
            consumer.accept(leftWindows[i]);
            consumer.accept(rightWindows[i]);
        }
    }
}
