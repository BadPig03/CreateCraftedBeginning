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
    protected final TransformedInstance mixer;
    protected final TransformedInstance[] leftWindows = new TransformedInstance[Iterate.horizontalDirections.length];
    protected final TransformedInstance[] rightWindows = new TransformedInstance[Iterate.horizontalDirections.length];

    protected float lastMixerAngle = Float.NaN;
    protected float lastMixerOffset = Float.NaN;
    protected float lastWindowDistance = Float.NaN;

    public AirtightReactorKettleVisual(VisualizationContext context, AirtightReactorKettleBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        mixer = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_MIXER)).createInstance();
        for (int windowIndex = 0; windowIndex < Iterate.horizontalDirections.length; windowIndex++) {
            leftWindows[windowIndex] = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_LEFT_WINDOW)).createInstance();
            rightWindows[windowIndex] = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_RIGHT_WINDOW)).createInstance();
        }
        animate(partialTick);
    }

    protected void animate(float partialTick) {
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

        for (int windowIndex = 0; windowIndex < Iterate.horizontalDirections.length; windowIndex++) {
            Direction direction = Iterate.horizontalDirections[windowIndex];
            Vec3i directionNormal = direction.getNormal();
            Vec3i leftOffset = direction.getClockWise().getNormal();
            Vec3i rightOffset = direction.getCounterClockWise().getNormal();

            leftWindows[windowIndex].setIdentityTransform().translate(getVisualPosition()).translate(directionNormal.getX(), directionNormal.getY(), directionNormal.getZ()).translate(leftOffset.getX() * windowDistance, leftOffset.getY() * windowDistance, leftOffset.getZ() * windowDistance).rotateYCenteredDegrees(AngleHelper.horizontalAngle(direction)).setChanged();
            rightWindows[windowIndex].setIdentityTransform().translate(getVisualPosition()).translate(directionNormal.getX(), directionNormal.getY(), directionNormal.getZ()).translate(rightOffset.getX() * windowDistance, rightOffset.getY() * windowDistance, rightOffset.getZ() * windowDistance).rotateYCenteredDegrees(AngleHelper.horizontalAngle(direction)).setChanged();
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
        for (int windowIndex = 0; windowIndex < leftWindows.length; windowIndex++) {
            relight(leftWindows[windowIndex], rightWindows[windowIndex]);
        }
    }

    @Override
    protected void _delete() {
        mixer.delete();
        for (int windowIndex = 0; windowIndex < leftWindows.length; windowIndex++) {
            leftWindows[windowIndex].delete();
            rightWindows[windowIndex].delete();
        }
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(mixer);
        for (int windowIndex = 0; windowIndex < leftWindows.length; windowIndex++) {
            consumer.accept(leftWindows[windowIndex]);
            consumer.accept(rightWindows[windowIndex]);
        }
    }
}
