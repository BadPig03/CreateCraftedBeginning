package net.ty.createcraftedbeginning.gas.behaviour;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;
import net.ty.createcraftedbeginning.gas.storage.SmartGasTank;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureFillService;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SmartGasTankBehaviour extends AbstractSmartGasTankBehaviour {
    public static final BehaviourType<SmartGasTankBehaviour> TYPE = new BehaviourType<>();
    public static final BehaviourType<SmartGasTankBehaviour> INPUT = new BehaviourType<>("GasInput");
    public static final BehaviourType<SmartGasTankBehaviour> OUTPUT = new BehaviourType<>("GasOutput");

    private final TankSegment[] tanks;

    private int mutationDepth;
    private boolean mutationDirty;

    public SmartGasTankBehaviour(BehaviourType<SmartGasTankBehaviour> type, SmartBlockEntity blockEntity, int tankCount, long tankVolume, boolean enforceVariety) {
        this(type, blockEntity, tankCount, tankVolume, GasPressure.REFERENCE_PRESSURE_PA, enforceVariety);
    }

    public SmartGasTankBehaviour(BehaviourType<SmartGasTankBehaviour> type, SmartBlockEntity blockEntity, int tankCount, long tankVolume, long maxPressurePa, boolean enforceVariety) {
        super(type, blockEntity);
        tanks = new TankSegment[tankCount];
        GasStorageHandler[] handlers = new GasStorageHandler[tankCount];
        for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
            TankSegment tankSegment = new TankSegment(tankVolume, maxPressurePa);
            tanks[tankIndex] = tankSegment;
            handlers[tankIndex] = tankSegment.tank;
        }
        capability = new InternalGasHandler(handlers, enforceVariety);
    }

    @Override
    public BehaviourType<?> getType() {
        return super.getType();
    }

    @Override
    public void sendDataImmediately() {
        if (mutationDepth > 0) {
            mutationDirty = true;
            return;
        }

        super.sendDataImmediately();
    }

    @Override
    TankSegment[] getTankSegments() {
        return tanks;
    }

    @Override
    void sendDataLazily() {
        if (mutationDepth > 0) {
            mutationDirty = true;
            return;
        }

        super.sendDataLazily();
    }

    @Contract("_, _ -> new")
    public static SmartGasTankBehaviour single(SmartBlockEntity blockEntity, long volume) {
        return new SmartGasTankBehaviour(TYPE, blockEntity, 1, volume, false);
    }

    @Contract("_, _, _ -> new")
    public static SmartGasTankBehaviour single(SmartBlockEntity blockEntity, long volume, long maxPressurePa) {
        return new SmartGasTankBehaviour(TYPE, blockEntity, 1, volume, maxPressurePa, false);
    }

    public SmartGasTankBehaviour whenTankUpdates(Runnable tankUpdateCallback) {
        this.tankUpdateCallback = tankUpdateCallback;
        return this;
    }

    public SmartGasTankBehaviour allowInsertion() {
        insertionAllowed = true;
        return this;
    }

    public SmartGasTankBehaviour allowExtraction() {
        extractionAllowed = true;
        return this;
    }

    public SmartGasTankBehaviour forbidInsertion() {
        insertionAllowed = false;
        return this;
    }

    public SmartGasTankBehaviour forbidExtraction() {
        extractionAllowed = false;
        return this;
    }

    public void beginMutation() {
        mutationDepth++;
    }

    public boolean endMutation() {
        if (mutationDepth <= 0) {
            throw new IllegalStateException("Cannot end gas tank mutation without an active scope; got mutation depth " + mutationDepth + '.');
        }

        mutationDepth--;
        if (mutationDepth != 0) {
            return false;
        }

        boolean stateChanged = mutationDirty;
        mutationDirty = false;
        return stateChanged;
    }

    public GasTankState[] snapshotStates() {
        GasTankState[] states = new GasTankState[tanks.length];
        for (int tankIndex = 0; tankIndex < tanks.length; tankIndex++) {
            states[tankIndex] = tanks[tankIndex].tank.snapshot();
        }
        return states;
    }

    public void validateStates(GasTankState[] replacementStates, int statesOffset) {
        if (statesOffset < 0 || statesOffset + tanks.length > replacementStates.length) {
            throw new IllegalArgumentException("Gas tank snapshot must contain " + tanks.length + " states at a non-negative offset; got offset " + statesOffset + " and snapshot length " + replacementStates.length + '.');
        }

        for (int tankIndex = 0; tankIndex < tanks.length; tankIndex++) {
            SmartGasTank tank = tanks[tankIndex].tank;
            GasTankState replacementState = replacementStates[statesOffset + tankIndex];
            if (tank.canContain(replacementState.limits(), replacementState.contents())) {
                continue;
            }

            throw new IllegalArgumentException("Gas tank snapshot state at tank index " + tankIndex + " cannot be contained: volume=" + replacementState.limits().volumeLiters() + " L, maximumPressure=" + replacementState.limits().maxPressurePa() + " Pa, amount=" + replacementState.contents().getAmount() + " GU.");
        }
    }

    public void replaceStates(GasTankState[] replacementStates, int statesOffset) {
        validateStates(replacementStates, statesOffset);
        for (int tankIndex = 0; tankIndex < tanks.length; tankIndex++) {
            tanks[tankIndex].tank.tryApplyState(replacementStates[statesOffset + tankIndex]).requireAccepted();
        }
    }

    public SmartGasTank getPrimaryHandler() {
        return getPrimaryTank().tank;
    }

    public TankSegment[] getTanks() {
        return tanks;
    }

    public InternalGasHandler getInternalGasHandler() {
        return (InternalGasHandler) capability;
    }

    private TankSegment getPrimaryTank() {
        return tanks[0];
    }

    public class InternalGasHandler extends InternalGasHandlerBase {
        private InternalGasHandler(GasStorageHandler[] handlers, boolean enforceVariety) {
            super(handlers, enforceVariety);
        }

        @Override
        public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
            boolean hasGasToFill = resources.stream().anyMatch(gasStack -> gasStack != null && !gasStack.isEmpty());
            if (!hasGasToFill) {
                return AtomicFillResult.SUCCESS;
            }

            if (!insertionAllowed) {
                return AtomicFillResult.REJECTED;
            }

            GasTankState[] stateSnapshot = snapshotStates();
            boolean wasMutationDirty = mutationDirty;
            boolean fillSucceeded;
            boolean shouldKeepChanges = false;
            beginMutation();
            try {
                fillSucceeded = fillAll(resources);
                shouldKeepChanges = fillSucceeded && action.execute();
                if (!fillSucceeded) {
                    return AtomicFillResult.REJECTED;
                }

                return AtomicFillResult.SUCCESS;
            }
            finally {
                boolean stateChanged;
                try {
                    if (!shouldKeepChanges) {
                        replaceStates(stateSnapshot, 0);
                    }
                }
                finally {
                    stateChanged = endMutation();
                }

                if (!shouldKeepChanges) {
                    mutationDirty = wasMutationDirty;
                }
                else if (stateChanged) {
                    sendDataImmediately();
                }
            }
        }

        @Override
        public AtomicFillResult tryFillAtomicallyFromPressure(List<GasStack> resources, long sourcePressurePa, GasAction action) {
            boolean hasGasToFill = resources.stream().anyMatch(gasStack -> gasStack != null && !gasStack.isEmpty());
            if (!hasGasToFill) {
                return AtomicFillResult.SUCCESS;
            }

            if (!insertionAllowed) {
                return AtomicFillResult.REJECTED;
            }

            GasTankState[] stateSnapshot = snapshotStates();
            boolean wasMutationDirty = mutationDirty;
            boolean fillSucceeded;
            boolean shouldKeepChanges = false;
            beginMutation();
            try {
                fillSucceeded = GasPressureFillService.fillAllFromFixedPressure(this, resources, sourcePressurePa);
                shouldKeepChanges = fillSucceeded && action.execute();
                if (fillSucceeded) {
                    return AtomicFillResult.SUCCESS;
                }

                return AtomicFillResult.REJECTED;
            }
            finally {
                boolean stateChanged;
                try {
                    if (!shouldKeepChanges) {
                        replaceStates(stateSnapshot, 0);
                    }
                }
                finally {
                    stateChanged = endMutation();
                }

                if (!shouldKeepChanges) {
                    mutationDirty = wasMutationDirty;
                }
                else if (stateChanged) {
                    sendDataImmediately();
                }
            }
        }

        private boolean fillAll(List<GasStack> resources) {
            for (GasStack gasStack : resources) {
                if (gasStack == null || gasStack.isEmpty()) {
                    continue;
                }

                long filledAmount = forceFill(gasStack.copy(), GasAction.EXECUTE);
                if (filledAmount == gasStack.getAmount()) {
                    continue;
                }

                return false;
            }
            return true;
        }
    }

    public class TankSegment extends TankSegmentBase {
        private final SmartGasTank tank;

        private TankSegment(long volume, long maxPressurePa) {
            tank = new SmartGasTank(volume, maxPressurePa, this::onTankStateChanged);
        }

        @Override
        SmartGasTank getTank() {
            return tank;
        }
    }
}
