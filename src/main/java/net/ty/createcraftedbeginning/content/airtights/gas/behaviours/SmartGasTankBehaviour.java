package net.ty.createcraftedbeginning.content.airtights.gas.behaviours;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.SmartGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
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

    public SmartGasTankBehaviour(BehaviourType<SmartGasTankBehaviour> type, SmartBlockEntity be, int tankCount, long tankCapacity, boolean enforceVariety) {
        super(type, be);
        tanks = new TankSegment[tankCount];
        IGasHandler[] handlers = new IGasHandler[tankCount];
        for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
            TankSegment tankSegment = new TankSegment(tankCapacity);
            tanks[tankIndex] = tankSegment;
            handlers[tankIndex] = tankSegment.tank;
        }
        capability = new InternalGasHandler(handlers, enforceVariety);
    }

    @Contract("_, _ -> new")
    public static SmartGasTankBehaviour single(SmartBlockEntity be, long capacity) {
        return new SmartGasTankBehaviour(TYPE, be, 1, capacity, false);
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
    TankSegment[] getTankSegmentsForLifecycle() {
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

    public SmartGasTankBehaviour whenGasUpdates(Runnable gasUpdateCallback) {
        this.gasUpdateCallback = gasUpdateCallback;
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
            throw new IllegalStateException("Unbalanced gas tank mutation scope");
        }

        mutationDepth--;
        if (mutationDepth != 0) {
            return false;
        }

        boolean contentsChanged = mutationDirty;
        mutationDirty = false;
        return contentsChanged;
    }

    public void replaceContents(GasStack[] replacementContents, int contentsOffset) {
        if (contentsOffset < 0 || contentsOffset + tanks.length > replacementContents.length) {
            throw new IllegalArgumentException("Invalid gas tank snapshot");
        }
        for (int tankIndex = 0; tankIndex < tanks.length; tankIndex++) {
            SmartGasTank tank = tanks[tankIndex].tank;
            GasStack replacementGas = replacementContents[contentsOffset + tankIndex].copy();
            GasStack currentGas = tank.getGasStack();
            if (replacementGas.getAmount() > tank.getCapacity()) {
                throw new IllegalArgumentException("Gas snapshot exceeds tank capacity");
            }

            boolean isUnchanged = replacementGas.isEmpty() && currentGas.isEmpty() || replacementGas.getAmount() == currentGas.getAmount() && GasStack.isSameGasSameComponents(replacementGas, currentGas);
            if (isUnchanged) {
                continue;
            }

            tank.setGasStack(replacementGas);
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
        private InternalGasHandler(IGasHandler[] handlers, boolean enforceVariety) {
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

            GasStack[] contentsSnapshot = copyContents();
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
            } finally {
                boolean contentsChanged;
                try {
                    if (!shouldKeepChanges) {
                        replaceContents(contentsSnapshot, 0);
                    }
                } finally {
                    contentsChanged = endMutation();
                }

                if (!shouldKeepChanges) {
                    mutationDirty = wasMutationDirty;
                }
                else if (contentsChanged) {
                    sendDataImmediately();
                }
            }
        }

        private GasStack[] copyContents() {
            GasStack[] contentsSnapshot = new GasStack[getTanks()];
            for (int tankIndex = 0; tankIndex < contentsSnapshot.length; tankIndex++) {
                contentsSnapshot[tankIndex] = getGasInTank(tankIndex).copy();
            }
            return contentsSnapshot;
        }

        private boolean fillAll(List<GasStack> resources) {
            for (GasStack gasStack : resources) {
                if (gasStack == null || gasStack.isEmpty()) {
                    continue;
                }

                long filledAmount = forceFill(gasStack.copy(), GasAction.EXECUTE);
                if (filledAmount != gasStack.getAmount()) {
                    return false;
                }
            }
            return true;
        }
    }

    public class TankSegment extends TankSegmentBase {
        private final SmartGasTank tank;

        private TankSegment(long capacity) {
            tank = new SmartGasTank(capacity, ignoredGas -> onGasStackChanged());
        }

        @Override
        SmartGasTank getTank() {
            return tank;
        }
    }
}
