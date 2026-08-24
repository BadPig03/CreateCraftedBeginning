package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.BASIN_RECIPE;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.NONE;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberController {
    private final GasInjectionChamberBlockEntity chamber;
    private final GasInjectionChamberOperationState operation;
    private final GasInjectionChamberBeltProcessor beltProcessor;
    private final GasInjectionChamberBasinProcessor basinProcessor;
    private final GasInjectionChamberVisualState visual;

    public GasInjectionChamberController(GasInjectionChamberBlockEntity chamber, GasInjectionChamberOperationState operation, GasInjectionChamberBeltProcessor beltProcessor, GasInjectionChamberBasinProcessor basinProcessor, GasInjectionChamberVisualState visual) {
        this.chamber = chamber;
        this.operation = operation;
        this.beltProcessor = beltProcessor;
        this.basinProcessor = basinProcessor;
        this.visual = visual;
    }

    public void tick() {
        operation.capturePreviousProcessingTicks();
        Level level = chamber.getLevel();
        if (level == null) {
            return;
        }

        if (!level.isClientSide && !operation.isRunning() && operation.type == NONE && chamber.consumeBasinCheckScheduled()) {
            basinProcessor.tryStartOperation();
        }

        if (!operation.isRunning()) {
            return;
        }

        operation.decrementProcessingTicks();
        if (!level.isClientSide && operation.type == BASIN_RECIPE && !operation.hasAttemptedExecution() && operation.getProcessingTicks() <= GasInjectionChamberBlockEntity.INJECTION_EXECUTION_TICK) {
            operation.markExecutionAttempted();
            executeBasinInjection();
        }
        if (operation.getProcessingTicks() >= 0) {
            return;
        }

        OperationType completedOperationType = operation.type;
        chamber.clearOperationState();
        if (!level.isClientSide && completedOperationType == BASIN_RECIPE) {
            chamber.scheduleBasinCheck();
        }
        chamber.setChanged();
    }

    public ProcessingResult onItemEntered(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return beltProcessor.onItemEntered(transported, handler);
    }

    public ProcessingResult onItemHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return beltProcessor.onItemHeld(transported, handler);
    }

    private void executeBasinInjection() {
        Level level = chamber.getLevel();
        if (level == null) {
            return;
        }

        int cloudColor = chamber.getGasInTank().getHint();
        boolean injectionSucceeded;
        chamber.getGasTankBehaviour().beginMutation();
        try {
            injectionSucceeded = basinProcessor.executeCurrentState();
        } finally {
            chamber.getGasTankBehaviour().endMutation();
        }

        if (!injectionSucceeded) {
            chamber.getGasTankBehaviour().sendDataImmediately();
            return;
        }

        visual.queueCloud(cloudColor);
        chamber.getGasTankBehaviour().sendDataImmediately();
        CCBSoundEvents.INJECTING.playOnServer(level, chamber.getBlockPos(), 0.75f, 0.9f + 0.2f * level.random.nextFloat());
    }
}
