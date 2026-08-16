package net.ty.createcraftedbeginning.content.airtights.airtightengine;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.Translate;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEngineVisual extends KineticBlockEntityVisual<AirtightEngineBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {
    protected final Direction direction;
    protected final Axis axis;
    protected final int rotationSign;
    protected final RotatingInstance cogs;
    protected final TransformedInstance piston;

    public AirtightEngineVisual(VisualizationContext context, AirtightEngineBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        BlockState state = blockEntity.getBlockState();
        axis = state.getValue(AirtightEngineBlock.AXIS);
        direction = AirtightEngineBlock.getFacing(state);
        rotationSign = direction.getAxisDirection() == AxisDirection.NEGATIVE ? 1 : -1;

        cogs = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(CCBPartialModels.AIRTIGHT_ENGINE_COGS)).createInstance();
        applyCogBaseRotation();
        updateCogRotation();

        piston = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.AIRTIGHT_ENGINE_PISTON)).createInstance();
        animatePiston(partialTick);
    }

    protected void applyCogBaseRotation() {
        if (axis == Axis.X) {
            cogs.rotation.rotateZ(-Mth.HALF_PI * rotationSign);
        }
        else if (axis == Axis.Z) {
            cogs.rotation.rotateX(Mth.HALF_PI * rotationSign);
        }
        else if (direction == Direction.UP) {
            cogs.rotation.rotateX(Mth.PI);
        }
    }

    protected void updateCogRotation() {
        cogs.setup(blockEntity).setPosition(getVisualPosition()).setChanged();
    }

    protected void animatePiston(float partialTick) {
        piston.setIdentityTransform().translate(getVisualPosition()).translate(Translate.CENTER);
        if (axis == Axis.X) {
            piston.rotateZ(-Mth.HALF_PI * rotationSign);
        }
        else if (axis == Axis.Z) {
            piston.rotateX(Mth.HALF_PI * rotationSign);
        }
        else if (direction == Direction.UP) {
            piston.rotateX(Mth.PI);
        }
        piston.translateBack(Translate.CENTER);
        piston.translate(0, -0.2f * Mth.sin(blockEntity.getPistonPhase(partialTick)) - 0.2f, 0).setChanged();
    }

    @Override
    public void update(float partialTick) {
        updateCogRotation();
    }

    @Override
    protected void _delete() {
        cogs.delete();
        piston.delete();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        applyOverstressEffect(blockEntity, cogs);
    }

    @Override
    public void beginFrame(DynamicVisual.@NotNull Context context) {
        if (!isVisible(context.frustum()) || doDistanceLimitThisFrame(context)) {
            return;
        }

        animatePiston(context.partialTick());
    }

    @Override
    public void updateLight(float partialTick) {
        relight(cogs, piston);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(cogs);
        consumer.accept(piston);
    }
}
