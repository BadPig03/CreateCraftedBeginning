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
import net.ty.createcraftedbeginning.api.gas.gases.handlers.CreativeSmartGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CreativeSmartGasTankBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<CreativeSmartGasTankBehaviour> TYPE = new BehaviourType<>();
    public static final BehaviourType<CreativeSmartGasTankBehaviour> INPUT = new BehaviourType<>("CreativeGasInput");
    public static final BehaviourType<CreativeSmartGasTankBehaviour> OUTPUT = new BehaviourType<>("CreativeGasOutput");

    private static final String COMPOUND_KEY_TANK_CONTENT = "TankContent";
    private static final int SYNC_RATE = 8;

    private final BehaviourType<CreativeSmartGasTankBehaviour> behaviourType;

    protected int syncCooldown;
    protected boolean queuedSync;
    protected TankSegment[] tanks;
    protected IGasHandler capability;
    protected boolean extractionAllowed;
    protected boolean insertionAllowed;
    protected Runnable gasUpdateCallback;

    /**
     * Creates a new {@code CreativeSmartGasTankBehaviour} instance.
     *
     * @param type           the type to use
     * @param be             the block entity that participates in the operation
     * @param tanks          the tanks value to use
     * @param tankCapacity   the tank capacity value to use
     * @param enforceVariety whether enforce variety is enabled
     */
    public CreativeSmartGasTankBehaviour(BehaviourType<CreativeSmartGasTankBehaviour> type, SmartBlockEntity be, int tanks, long tankCapacity, boolean enforceVariety) {
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
    public static CreativeSmartGasTankBehaviour single(SmartBlockEntity be, long capacity) {
        return new CreativeSmartGasTankBehaviour(TYPE, be, 1, capacity, false);
    }

    /**
     * Adds a condition for when gas updates.
     *
     * @param gasUpdateCallback the gas update callback to use
     * @return this instance
     */
    public CreativeSmartGasTankBehaviour whenGasUpdates(Runnable gasUpdateCallback) {
        this.gasUpdateCallback = gasUpdateCallback;
        return this;
    }

    /**
     * Allows insertion.
     *
     * @return this instance
     */
    public CreativeSmartGasTankBehaviour allowInsertion() {
        insertionAllowed = true;
        return this;
    }

    /**
     * Allows extraction.
     *
     * @return this instance
     */
    public CreativeSmartGasTankBehaviour allowExtraction() {
        extractionAllowed = true;
        return this;
    }

    /**
     * Forbids insertion.
     *
     * @return this instance
     */
    public CreativeSmartGasTankBehaviour forbidInsertion() {
        insertionAllowed = false;
        return this;
    }

    /**
     * Forbids extraction.
     *
     * @return this instance
     */
    public CreativeSmartGasTankBehaviour forbidExtraction() {
        extractionAllowed = false;
        return this;
    }

    /**
     * Sends pending synchronization data immediately.
     */
    public void sendDataImmediately() {
        syncCooldown = 0;
        queuedSync = false;
        updateGases();
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
    public CreativeSmartGasTank getPrimaryHandler() {
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

        /**
         * {@inheritDoc}
         */
        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return extractionAllowed ? super.drain(resource, GasAction.SIMULATE) : GasStack.EMPTY;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return extractionAllowed ? super.drain(maxDrain, GasAction.SIMULATE) : GasStack.EMPTY;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long fill(GasStack resource, GasAction action) {
            return insertionAllowed ? resource.getAmount() : 0;
        }

        /**
         * Inserts gas without applying the normal transfer restrictions.
         *
         * @param resource the gas resource to insert or extract
         * @param action   the action that determines whether the operation is simulated or executed
         * @return the amount of gas that was accepted
         */
        public long forceFill(GasStack resource, GasAction action) {
            return resource.getAmount();
        }

        /**
         * Extracts gas without applying the normal transfer restrictions.
         *
         * @param resource the gas resource to insert or extract
         * @param action   the action that determines whether the operation is simulated or executed
         * @return the gas that was extracted
         */
        public GasStack forceDrain(GasStack resource, GasAction action) {
            return super.drain(resource, GasAction.SIMULATE);
        }

        /**
         * Extracts gas without applying the normal transfer restrictions.
         *
         * @param maxDrain the maximum amount that may be extracted
         * @param action   the action that determines whether the operation is simulated or executed
         * @return the gas that was extracted
         */
        public GasStack forceDrain(long maxDrain, GasAction action) {
            return super.drain(maxDrain, GasAction.SIMULATE);
        }
    }

    public class TankSegment {
        protected CreativeSmartGasTank tank;

        /**
         * Creates a new {@code TankSegment} instance.
         *
         * @param capacity the capacity to use
         */
        public TankSegment(long capacity) {
            tank = new CreativeSmartGasTank(capacity, f -> onGasStackChanged());
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