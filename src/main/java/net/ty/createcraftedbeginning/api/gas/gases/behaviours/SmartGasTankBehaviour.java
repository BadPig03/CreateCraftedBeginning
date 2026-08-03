package net.ty.createcraftedbeginning.api.gas.gases.behaviours;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.CombinedGasTankWrapper;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.SmartGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class SmartGasTankBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<SmartGasTankBehaviour> TYPE = new BehaviourType<>();
    public static final BehaviourType<SmartGasTankBehaviour> INPUT = new BehaviourType<>("GasInput");
    public static final BehaviourType<SmartGasTankBehaviour> OUTPUT = new BehaviourType<>("GasOutput");

    private static final String COMPOUND_KEY_TANK_CONTENT = "TankContent";
    private static final int SYNC_RATE = 8;

    private final BehaviourType<SmartGasTankBehaviour> behaviourType;

    protected int syncCooldown;
    protected boolean queuedSync;
    protected TankSegment[] tanks;
    protected IGasHandler capability;
    protected boolean extractionAllowed;
    protected boolean insertionAllowed;
    protected Runnable gasUpdateCallback;
    protected int mutationDepth;
    protected boolean mutationDirty;

    /**
     * Creates a new {@code SmartGasTankBehaviour} instance.
     *
     * @param type           the type to use
     * @param be             the block entity that participates in the operation
     * @param tanks          the tanks value to use
     * @param tankCapacity   the tank capacity value to use
     * @param enforceVariety whether enforce variety is enabled
     */
    public SmartGasTankBehaviour(BehaviourType<SmartGasTankBehaviour> type, SmartBlockEntity be, int tanks, long tankCapacity, boolean enforceVariety) {
        super(be);
        insertionAllowed = true;
        extractionAllowed = true;
        behaviourType = type;
        this.tanks = new TankSegment[tanks];
        IGasHandler[] handlers = new IGasHandler[tanks];
        for (int i = 0; i < tanks; i++) {
            TankSegment tankSegment = new TankSegment(tankCapacity);
            this.tanks[i] = tankSegment;
            handlers[i] = tankSegment.tank;
        }
        capability = new InternalGasHandler(handlers, enforceVariety);
        gasUpdateCallback = () -> {};
    }

    /**
     * Creates an ingredient that matches a single gas.
     *
     * @param be       the block entity that participates in the operation
     * @param capacity the capacity to use
     * @return the created value
     */
    @Contract("_, _ -> new")
    public static SmartGasTankBehaviour single(SmartBlockEntity be, long capacity) {
        return new SmartGasTankBehaviour(TYPE, be, 1, capacity, false);
    }

    /**
     * Adds a condition for when gas updates.
     *
     * @param gasUpdateCallback the gas update callback to use
     * @return this instance
     */
    public SmartGasTankBehaviour whenGasUpdates(Runnable gasUpdateCallback) {
        this.gasUpdateCallback = gasUpdateCallback;
        return this;
    }

    /**
     * Allows insertion.
     *
     * @return this instance
     */
    public SmartGasTankBehaviour allowInsertion() {
        insertionAllowed = true;
        return this;
    }

    /**
     * Allows extraction.
     *
     * @return this instance
     */
    public SmartGasTankBehaviour allowExtraction() {
        extractionAllowed = true;
        return this;
    }

    /**
     * Forbids insertion.
     *
     * @return this instance
     */
    public SmartGasTankBehaviour forbidInsertion() {
        insertionAllowed = false;
        return this;
    }

    /**
     * Forbids extraction.
     *
     * @return this instance
     */
    public SmartGasTankBehaviour forbidExtraction() {
        extractionAllowed = false;
        return this;
    }

    /**
     * Sends pending synchronization data immediately.
     */
    public void sendDataImmediately() {
        if (mutationDepth > 0) {
            mutationDirty = true;
            return;
        }

        syncCooldown = 0;
        queuedSync = false;
        updateGases();
    }

    /**
     * Begins the mutation operation.
     */
    public void beginMutation() {
        mutationDepth++;
    }

    /**
     * Completes the mutation operation.
     *
     * @return {@code true} if the condition is satisfied; otherwise {@code false}
     */
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

    /**
     * Replaces the stored contents with the supplied gas stacks.
     *
     * @param contents the contents to use
     * @param offset   the offset value to use
     */
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

    protected void updateGases() {
        gasUpdateCallback.run();
        blockEntity.sendData();
        blockEntity.setChanged();
    }

    /**
     * Schedules pending synchronization data for deferred delivery.
     */
    public void sendDataLazily() {
        if (mutationDepth > 0) {
            mutationDirty = true;
            return;
        }

        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }

        updateGases();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    /**
     * Returns the primary handler.
     *
     * @return the primary handler
     */
    public SmartGasTank getPrimaryHandler() {
        return getPrimaryTank().tank;
    }

    /**
     * Returns the primary tank.
     *
     * @return the primary tank
     */
    public TankSegment getPrimaryTank() {
        return tanks[0];
    }

    /**
     * Returns the tank segments exposed by this handler.
     *
     * @return the tank segments exposed by this handler
     */
    public TankSegment[] getTanks() {
        return tanks;
    }

    /**
     * Checks whether this value is empty.
     *
     * @return {@code true} if this value is empty; otherwise {@code false}
     */
    public boolean isEmpty() {
        for (TankSegment tankSegment : tanks) {
            if (tankSegment.tank.isEmpty()) {
                continue;
            }

            return false;
        }
        return true;
    }

    /**
     * Returns the capability.
     *
     * @return the capability
     */
    public IGasHandler getCapability() {
        return capability;
    }

    /**
     * Returns the internal gas handler.
     *
     * @return the internal gas handler
     */
    public InternalGasHandler getInternalGasHandler() {
        return (InternalGasHandler) capability;
    }

    /**
     * Invokes the supplied consumer for each contained value.
     *
     * @param action the action that determines whether the operation is simulated or executed
     */
    public void forEach(Consumer<TankSegment> action) {
        for (TankSegment tankSegment : tanks) {
            action.accept(tankSegment);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BehaviourType<?> getType() {
        return behaviourType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        super.initialize();
        if (getWorld().isClientSide) {
            return;
        }

        forEach(TankSegment::onGasStackChanged);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void tick() {
        super.tick();
        if (syncCooldown <= 0) {
            return;
        }

        syncCooldown--;
        if (syncCooldown != 0 || !queuedSync) {
            return;
        }

        updateGases();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        MutableInt index = new MutableInt(0);
        ListTag tankData = compoundTag.getList(getType().getName() + "Tanks", Tag.TAG_COMPOUND);
        NBTHelper.iterateCompoundList(tankData, tankTag -> {
            if (index.intValue() >= tanks.length) {
                return;
            }

            tanks[index.intValue()].read(tankTag, provider, clientPacket);
            index.increment();
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        ListTag tankData = new ListTag();
        forEach(tank -> tankData.add(tank.write(provider)));
        compoundTag.put(getType().getName() + "Tanks", tankData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unload() {
        super.unload();
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        level.invalidateCapabilities(getPos());
    }

    public class InternalGasHandler extends CombinedGasTankWrapper {
        /**
         * Creates a new {@code InternalGasHandler} instance.
         *
         * @param handlers       the handlers to use
         * @param enforceVariety whether enforce variety is enabled
         */
        public InternalGasHandler(IGasHandler[] handlers, boolean enforceVariety) {
            super(handlers);
            if (!enforceVariety) {
                return;
            }

            enforceVariety();
        }

        /**
         * {@inheritDoc}
         */
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

                long filled = super.fill(resource.copy(), GasAction.EXECUTE);
                if (filled != resource.getAmount()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return extractionAllowed ? super.drain(resource, action) : GasStack.EMPTY;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return extractionAllowed ? super.drain(maxDrain, action) : GasStack.EMPTY;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long fill(GasStack resource, GasAction action) {
            return insertionAllowed ? super.fill(resource, action) : 0;
        }

        /**
         * Inserts gas without applying the normal transfer restrictions.
         *
         * @param resource the gas resource to insert or extract
         * @param action   the action that determines whether the operation is simulated or executed
         * @return the amount of gas that was accepted
         */
        public long forceFill(GasStack resource, GasAction action) {
            return super.fill(resource, action);
        }

        /**
         * Extracts gas without applying the normal transfer restrictions.
         *
         * @param resource the gas resource to insert or extract
         * @param action   the action that determines whether the operation is simulated or executed
         * @return the gas that was extracted
         */
        public GasStack forceDrain(GasStack resource, GasAction action) {
            return super.drain(resource, action);
        }

        /**
         * Extracts gas without applying the normal transfer restrictions.
         *
         * @param maxDrain the maximum amount that may be extracted
         * @param action   the action that determines whether the operation is simulated or executed
         * @return the gas that was extracted
         */
        public GasStack forceDrain(long maxDrain, GasAction action) {
            return super.drain(maxDrain, action);
        }
    }

    public class TankSegment {
        protected SmartGasTank tank;

        /**
         * Creates a new {@code TankSegment} instance.
         *
         * @param capacity the capacity to use
         */
        public TankSegment(long capacity) {
            tank = new SmartGasTank(capacity, f -> onGasStackChanged());
        }

        /**
         * Handles the gas stack changed event.
         */
        public void onGasStackChanged() {
            Level level = getWorld();
            if (level == null || level.isClientSide) {
                return;
            }

            sendDataLazily();
        }

        /**
         * Writes this object's state to the supplied serialized data.
         *
         * @param provider the provider used to resolve the requested value
         * @return the resulting compound tag
         */
        public CompoundTag write(Provider provider) {
            CompoundTag tag = new CompoundTag();
            tag.put(COMPOUND_KEY_TANK_CONTENT, tank.write(provider, new CompoundTag()));
            return tag;
        }

        /**
         * Reads this object's state from the supplied serialized data.
         *
         * @param compoundTag  the NBT compound to read from or write to
         * @param provider     the provider used to resolve the requested value
         * @param clientPacket the client synchronization packet
         */
        public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
            if (!compoundTag.contains(COMPOUND_KEY_TANK_CONTENT)) {
                return;
            }

            tank.read(provider, compoundTag.getCompound(COMPOUND_KEY_TANK_CONTENT));
        }
    }
}