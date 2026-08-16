package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressStructuralShaftVisual extends KineticBlockEntityVisual<AirtightForgingPressStructuralShaftBlockEntity> implements SimpleTickableVisual {
    @Nullable
    protected final RotatingInstance rotatingModel;

    public AirtightForgingPressStructuralShaftVisual(VisualizationContext context, AirtightForgingPressStructuralShaftBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        AirtightForgingPressStructuralPosition position = blockEntity.getBlockState().getValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION);
        if (position == AirtightForgingPressStructuralPosition.TOP_CENTER) {
            rotatingModel = null;
            return;
        }

        Direction shaftDirection = Direction.fromAxisAndDirection(position.getAxis(), position.getAxisDirection());
        rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(CCBPartialModels.SHAFT_HALF_UP)).createInstance().rotateToFace(Direction.UP, shaftDirection).setup(blockEntity).setPosition(getVisualPosition());
        rotatingModel.setChanged();
    }

    @Override
    public void update(float partialTick) {
        if (rotatingModel == null) {
            return;
        }

        rotatingModel.setup(blockEntity).setChanged();
    }

    @Override
    protected void _delete() {
        if (rotatingModel == null) {
            return;
        }

        rotatingModel.delete();
    }

    @Override
    public void tick(Context context) {
        if (rotatingModel == null) {
            return;
        }

        applyOverstressEffect(blockEntity, rotatingModel);
    }

    @Override
    public void updateLight(float partialTick) {
        if (rotatingModel == null) {
            return;
        }

        relight(rotatingModel);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        if (rotatingModel == null) {
            return;
        }

        consumer.accept(rotatingModel);
    }
}
