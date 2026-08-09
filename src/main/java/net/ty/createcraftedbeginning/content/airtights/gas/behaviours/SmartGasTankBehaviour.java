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
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SmartGasTankBehaviour extends AbstractSmartGasTankBehaviour {
    public static final BehaviourType<SmartGasTankBehaviour> TYPE = new BehaviourType<>();
    public static final BehaviourType<SmartGasTankBehaviour> INPUT = new BehaviourType<>("GasInput");
    public static final BehaviourType<SmartGasTankBehaviour> OUTPUT = new BehaviourType<>("GasOutput");

    protected TankSegment[] tanks;
    protected int mutationDepth;
    protected boolean mutationDirty;

    public SmartGasTankBehaviour(BehaviourType<SmartGasTankBehaviour> type, SmartBlockEntity be, int tanks, long tankCapacity, boolean enforceVariety) {
        super(type, be);
        this.tanks = new TankSegment[tanks];
        IGasHandler[] handlers = new IGasHandler[tanks];
        for (int i = 0; i < tanks; i++) {
            TankSegment tankSegment = new TankSegment(tankCapacity);
            this.tanks[i] = tankSegment;
            handlers[i] = tankSegment.tank;
        }
        capability = new InternalGasHandler(handlers, enforceVariety);
    }

    @Contract("_, _ -> new")
    public static SmartGasTankBehaviour single(SmartBlockEntity be, long capacity) {
        return new SmartGasTankBehaviour(TYPE, be, 1, capacity, false);
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

    @Override
    public void sendDataImmediately() {
        if (mutationDepth > 0) {
            mutationDirty = true;
            return;
        }

        super.sendDataImmediately();
    }

    @Override
    public void sendDataLazily() {
        if (mutationDepth > 0) {
            mutationDirty = true;
            return;
        }

        super.sendDataLazily();
    }

    @Override
    public BehaviourType<?> getType() {
        return super.getType();
    }

    @Override
    protected TankSegment[] getTankSegmentsForLifecycle() {
        return tanks;
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

        boolean changed = mutationDirty;
        mutationDirty = false;
        return changed;
    }

    public void replaceContents(GasStack[] contents, int offset) {
        if (offset < 0 || offset + tanks.length > contents.length) {
            throw new IllegalArgumentException("Invalid gas tank snapshot");
        }
        for (int index = 0; index < tanks.length; index++) {
            SmartGasTank tank = tanks[index].tank;
            GasStack replacement = contents[offset + index].copy();
            GasStack current = tank.getGasStack();
            if (replacement.getAmount() > tank.getCapacity()) {
                throw new IllegalArgumentException("Gas snapshot exceeds tank capacity");
            }

            boolean unchanged = replacement.isEmpty() && current.isEmpty() || replacement.getAmount() == current.getAmount() && GasStack.isSameGasSameComponents(replacement, current);
            if (unchanged) {
                continue;
            }

            tank.setGasStack(replacement);
        }
    }

    public SmartGasTank getPrimaryHandler() {
        return getPrimaryTank().tank;
    }

    public TankSegment getPrimaryTank() {
        return tanks[0];
    }

    public TankSegment[] getTanks() {
        return tanks;
    }

    public InternalGasHandler getInternalGasHandler() {
        return (InternalGasHandler) capability;
    }

    public void forEach(Consumer<TankSegment> action) {
        for (TankSegment tankSegment : tanks) {
            action.accept(tankSegment);
        }
    }

    public class InternalGasHandler extends InternalGasHandlerBase {
        public InternalGasHandler(IGasHandler[] handlers, boolean enforceVariety) {
            super(handlers, enforceVariety);
        }

        @Override
        public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
            boolean hasResource = resources.stream().anyMatch(resource -> resource != null && !resource.isEmpty());
            if (!hasResource) {
                return AtomicFillResult.SUCCESS;
            }

            if (!insertionAllowed) {
                return AtomicFillResult.REJECTED;
            }

            GasStack[] snapshot = copyContents();
            boolean dirtyBefore = mutationDirty;
            boolean successful;
            boolean keepChanges = false;
            beginMutation();
            try {
                successful = fillAll(resources);
                keepChanges = successful && action.execute();
                return successful ? AtomicFillResult.SUCCESS : AtomicFillResult.REJECTED;
            } finally {
                boolean changed;
                try {
                    if (!keepChanges) {
                        replaceContents(snapshot, 0);
                    }
                } finally {
                    changed = endMutation();
                }

                if (!keepChanges) {
                    mutationDirty = dirtyBefore;
                }
                else if (changed) {
                    sendDataImmediately();
                }
            }
        }

        private GasStack[] copyContents() {
            GasStack[] contents = new GasStack[getTanks()];
            for (int tank = 0; tank < contents.length; tank++) {
                contents[tank] = getGasInTank(tank).copy();
            }
            return contents;
        }

        private boolean fillAll(List<GasStack> resources) {
            for (GasStack resource : resources) {
                if (resource == null || resource.isEmpty()) {
                    continue;
                }

                long filled = forceFill(resource.copy(), GasAction.EXECUTE);
                if (filled != resource.getAmount()) {
                    return false;
                }
            }
            return true;
        }
    }

    public class TankSegment extends TankSegmentBase {
        protected SmartGasTank tank;

        public TankSegment(long capacity) {
            tank = new SmartGasTank(capacity, f -> onGasStackChanged());
        }

        @Override
        protected SmartGasTank getTank() {
            return tank;
        }
    }
}
