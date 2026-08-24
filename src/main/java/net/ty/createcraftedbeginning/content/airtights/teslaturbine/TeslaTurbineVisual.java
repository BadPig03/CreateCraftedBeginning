package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineVisual extends KineticBlockEntityVisual<TeslaTurbineBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {
    private final Axis axis;
    private final RotatingInstance shaft;
    private final List<RotatingInstance> rotors = new ArrayList<>();
    private int rotorCount = -1;

    public TeslaTurbineVisual(VisualizationContext context, TeslaTurbineBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        axis = blockState.getValue(TeslaTurbineBlock.AXIS);
        shaft = createRotatingInstance(AllPartialModels.SHAFT);
        syncRotors();
    }

    private RotatingInstance createRotatingInstance(PartialModel partialModel) {
        RotatingInstance instance = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(partialModel)).createInstance().rotateToFace(Direction.UP, axis).setup(blockEntity).setPosition(getVisualPosition());
        instance.setChanged();
        return instance;
    }

    private void syncRotors() {
        int desiredRotorCount = blockEntity.getBlockState().getValue(TeslaTurbineBlock.ROTOR);
        if (desiredRotorCount == rotorCount) {
            return;
        }

        for (RotatingInstance rotor : rotors) {
            rotor.delete();
        }
        rotors.clear();
        rotorCount = desiredRotorCount;
        if (rotorCount == 0) {
            return;
        }

        Direction positiveAxis = Direction.get(AxisDirection.POSITIVE, axis);
        float rotorSpacing = 14.0f / (rotorCount + 1);
        for (int rotorIndex = 0; rotorIndex < rotorCount; rotorIndex++) {
            float rotorOffset = (rotorSpacing * (rotorIndex + 1) - 7) / 16;
            RotatingInstance rotor = createRotatingInstance(CCBPartialModels.TESLA_TURBINE_ROTOR).nudge(positiveAxis.getStepX() * rotorOffset, positiveAxis.getStepY() * rotorOffset, positiveAxis.getStepZ() * rotorOffset);
            rotor.setChanged();
            rotors.add(rotor);
        }
    }

    @Override
    public void update(float partialTick) {
        shaft.setup(blockEntity).setChanged();
        for (RotatingInstance rotor : rotors) {
            rotor.setup(blockEntity).setChanged();
        }
        syncRotors();
    }

    @Override
    protected void _delete() {
        shaft.delete();
        for (RotatingInstance rotor : rotors) {
            rotor.delete();
        }
        rotors.clear();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        applyOverstressEffect(blockEntity, shaft);
        for (RotatingInstance rotor : rotors) {
            applyOverstressEffect(blockEntity, rotor);
        }
    }

    @Override
    public void beginFrame(DynamicVisual.@NotNull Context context) {
        syncRotors();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(shaft);
        for (RotatingInstance rotor : rotors) {
            relight(rotor);
        }
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
        for (RotatingInstance rotor : rotors) {
            consumer.accept(rotor);
        }
    }
}
