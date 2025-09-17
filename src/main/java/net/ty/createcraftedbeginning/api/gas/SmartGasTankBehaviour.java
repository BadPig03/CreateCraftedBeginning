package net.ty.createcraftedbeginning.api.gas;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public class SmartGasTankBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<SmartGasTankBehaviour> TYPE = new BehaviourType<>();
    public static final BehaviourType<SmartGasTankBehaviour> INPUT = new BehaviourType<>("Input");
    public static final BehaviourType<SmartGasTankBehaviour> OUTPUT = new BehaviourType<>("Output");

    private static final int SYNC_RATE = 8;
    private final BehaviourType<SmartGasTankBehaviour> behaviourType;
    protected int syncCooldown;
    protected boolean queuedSync;
    protected TankSegment[] tanks;
    protected IGasHandler capability;
    protected boolean extractionAllowed;
    protected boolean insertionAllowed;
    protected Runnable gasUpdateCallback;

    public SmartGasTankBehaviour(BehaviourType<SmartGasTankBehaviour> type, SmartBlockEntity be, int tanks, int tankCapacity, boolean enforceVariety) {
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
        gasUpdateCallback = () -> {
        };
    }

    @Contract("_, _ -> new")
    public static @NotNull SmartGasTankBehaviour single(SmartBlockEntity be, int capacity) {
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

    public void sendDataImmediately() {
        syncCooldown = 0;
        queuedSync = false;
        updateGases();
    }

    public void sendDataLazily() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }
        updateGases();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    protected void updateGases() {
        gasUpdateCallback.run();
        blockEntity.sendData();
        blockEntity.setChanged();
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

    public boolean isEmpty() {
        for (TankSegment tankSegment : tanks) {
            if (!tankSegment.tank.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void forEach(Consumer<TankSegment> action) {
        for (TankSegment tankSegment : tanks) {
            action.accept(tankSegment);
        }
    }

    public IGasHandler getCapability() {
        return capability;
    }

    @Override
    public BehaviourType<?> getType() {
        return behaviourType;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (getWorld().isClientSide) {
            return;
        }

        forEach(TankSegment::onGasStackChanged);
    }

    @Override
    public void tick() {
        super.tick();

        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync) {
                updateGases();
            }
        }
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        MutableInt index = new MutableInt(0);
        NBTHelper.iterateCompoundList(nbt.getList(getType().getName() + "Tanks", Tag.TAG_COMPOUND), c -> {
            if (index.intValue() >= tanks.length) {
                return;
            }
            tanks[index.intValue()].readNBT(c, registries, clientPacket);
            index.increment();
        });
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        ListTag tanksNBT = new ListTag();
        forEach(ts -> tanksNBT.add(ts.writeNBT(registries)));
        nbt.put(getType().getName() + "Tanks", tanksNBT);
    }

    @Override
    public void unload() {
        super.unload();
        if (blockEntity.getLevel() == null) {
            return;
        }

        blockEntity.getLevel().invalidateCapabilities(getPos());
    }

    public class InternalGasHandler extends GasCombinedTankWrapper {
        public InternalGasHandler(IGasHandler[] handlers, boolean enforceVariety) {
            super(handlers);
            if (enforceVariety) {
                enforceVariety();
            }
        }

        @Override
        public long fill(@NotNull GasStack resource, GasAction action) {
            if (!insertionAllowed) {
                return 0;
            }
            return super.fill(resource, action);
        }

        @Override
        public GasStack drain(@NotNull GasStack resource, GasAction action) {
            if (!extractionAllowed) {
                return GasStack.EMPTY;
            }
            return super.drain(resource, action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            if (!extractionAllowed) {
                return GasStack.EMPTY;
            }
            return super.drain(maxDrain, action);
        }

        @SuppressWarnings("UnusedReturnValue")
        public long forceFill(GasStack resource, GasAction action) {
            return super.fill(resource, action);
        }

        public GasStack forceDrain(GasStack resource, GasAction action) {
            return super.drain(resource, action);
        }

        public GasStack forceDrain(long maxDrain, GasAction action) {
            return super.drain(maxDrain, action);
        }
    }

    public class TankSegment {
        protected SmartGasTank tank;

        public TankSegment(long capacity) {
            tank = new SmartGasTank(capacity, f -> onGasStackChanged());
        }

        public void onGasStackChanged() {
            if (!blockEntity.hasLevel()) {
                return;
            }
            if (!getWorld().isClientSide) {
                sendDataLazily();
            }
        }

        public CompoundTag writeNBT(HolderLookup.Provider registries) {
            CompoundTag compound = new CompoundTag();
            compound.put("TankContent", tank.writeToNBT(registries, new CompoundTag()));
            return compound;
        }

        public void readNBT(@NotNull CompoundTag compound, HolderLookup.Provider registries, boolean ignored) {
            tank.readFromNBT(registries, compound.getCompound("TankContent"));
        }
    }
}