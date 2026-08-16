package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
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
        if (!level.isClientSide && operation.type == BASIN_RECIPE && !operation.executed && operation.getProcessingTicks() <= GasInjectionChamberBlockEntity.INJECTION_EXECUTION_TICK) {
            executeBasinInjection();
        }
        if (operation.getProcessingTicks() >= 0) {
            return;
        }

        chamber.clearOperationState();
        chamber.setChanged();
    }

    public ProcessingResult onItemEntered(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return beltProcessor.onItemEntered(transported, handler);
    }

    public ProcessingResult onItemHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return beltProcessor.onItemHeld(transported, handler);
    }

    public boolean isFanProcessingOperationStillValid(ResourceLocation typeId) {
        return beltProcessor.isFanProcessingOperationStillValid(typeId);
    }

    private void executeBasinInjection() {
        Level level = chamber.getLevel();
        if (level == null) {
            return;
        }

        int color = operation.gas.getHint();
        boolean executed;
        chamber.getGasTankBehaviour().beginMutation();
        try {
            executed = basinProcessor.executeRecipeOperation();
        } finally {
            chamber.getGasTankBehaviour().endMutation();
        }

        if (executed) {
            operation.executed = true;
            visual.queueCloud(color);
        }
        else {
            operation.setProcessingTicks(-1);
            chamber.clearOperationState();
            chamber.scheduleBasinCheck();
        }

        chamber.getGasTankBehaviour().sendDataImmediately();
        if (!executed) {
            return;
        }

        CCBSoundEvents.INJECTING.playOnServer(level, chamber.getBlockPos(), 0.75f, 0.9f + 0.2f * level.random.nextFloat());
    }
}
