package net.ty.createcraftedbeginning.content.airtights.gas.behaviours;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.CreativeSmartGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CreativeSmartGasTankBehaviour extends AbstractSmartGasTankBehaviour {
    public static final BehaviourType<CreativeSmartGasTankBehaviour> TYPE = new BehaviourType<>();
    public static final BehaviourType<CreativeSmartGasTankBehaviour> INPUT = new BehaviourType<>("CreativeGasInput");
    public static final BehaviourType<CreativeSmartGasTankBehaviour> OUTPUT = new BehaviourType<>("CreativeGasOutput");

    protected TankSegment[] tanks;

    public CreativeSmartGasTankBehaviour(BehaviourType<CreativeSmartGasTankBehaviour> type, SmartBlockEntity be, int tanks, long tankCapacity, boolean enforceVariety) {
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
    public static CreativeSmartGasTankBehaviour single(SmartBlockEntity be, long capacity) {
        return new CreativeSmartGasTankBehaviour(TYPE, be, 1, capacity, false);
    }

    public CreativeSmartGasTankBehaviour whenGasUpdates(Runnable gasUpdateCallback) {
        this.gasUpdateCallback = gasUpdateCallback;
        return this;
    }

    public CreativeSmartGasTankBehaviour allowInsertion() {
        insertionAllowed = true;
        return this;
    }

    public CreativeSmartGasTankBehaviour allowExtraction() {
        extractionAllowed = true;
        return this;
    }

    public CreativeSmartGasTankBehaviour forbidInsertion() {
        insertionAllowed = false;
        return this;
    }

    public CreativeSmartGasTankBehaviour forbidExtraction() {
        extractionAllowed = false;
        return this;
    }

    @Override
    public BehaviourType<?> getType() {
        return super.getType();
    }

    @Override
    protected TankSegment[] getTankSegmentsForLifecycle() {
        return tanks;
    }

    public CreativeSmartGasTank getPrimaryHandler() {
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

            for (GasStack resource : resources) {
                if (resource == null || resource.isEmpty()) {
                    continue;
                }

                if (fill(resource, GasAction.SIMULATE) == resource.getAmount()) {
                    continue;
                }

                return AtomicFillResult.REJECTED;
            }
            return AtomicFillResult.SUCCESS;
        }

        @Override
        protected GasStack drainAllowed(GasStack resource, GasAction action) {
            return super.drainAllowed(resource, GasAction.SIMULATE);
        }

        @Override
        protected GasStack drainAllowed(long maxDrain, GasAction action) {
            return super.drainAllowed(maxDrain, GasAction.SIMULATE);
        }

        @Override
        protected long fillAllowed(GasStack resource, GasAction action) {
            return resource.getAmount();
        }
    }

    public class TankSegment extends TankSegmentBase {
        protected CreativeSmartGasTank tank;

        public TankSegment(long capacity) {
            tank = new CreativeSmartGasTank(capacity, f -> onGasStackChanged());
        }

        @Override
        protected CreativeSmartGasTank getTank() {
            return tank;
        }
    }
}
