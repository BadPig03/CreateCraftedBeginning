package net.ty.createcraftedbeginning.api.gas;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
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
        gasUpdateCallback = () -> {
        };
    }

    @Contract("_, _ -> new")
    public static @NotNull SmartGasTankBehaviour single(SmartBlockEntity be, long capacity) {
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

    protected void updateGases() {
        gasUpdateCallback.run();
        blockEntity.sendData();
        blockEntity.setChanged();
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
            if (tankSegment.tank.isEmpty()) {
                continue;
            }

            return false;
        }
        return true;
    }

    public IGasHandler getCapability() {
        return capability;
    }

    public InternalGasHandler getInternalGasHandler() {
        return (InternalGasHandler) capability;
    }

    public void forEach(Consumer<TankSegment> action) {
        for (TankSegment tankSegment : tanks) {
            action.accept(tankSegment);
        }
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

        if (syncCooldown <= 0) {
            return;
        }

        syncCooldown--;
        if (syncCooldown != 0 || !queuedSync) {
            return;
        }

        updateGases();
    }

    @Override
    public void read(CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        super.read(compoundTag, registries, clientPacket);
        MutableInt index = new MutableInt(0);
        NBTHelper.iterateCompoundList(compoundTag.getList(getType().getName() + "Tanks", Tag.TAG_COMPOUND), c -> {
            if (index.intValue() >= tanks.length) {
                return;
            }

            tanks[index.intValue()].read(c, registries, clientPacket);
            index.increment();
        });
    }

    @Override
    public void write(CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        super.write(compoundTag, registries, clientPacket);
        ListTag tanksNBT = new ListTag();
        forEach(ts -> tanksNBT.add(ts.write(registries)));
        compoundTag.put(getType().getName() + "Tanks", tanksNBT);
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
            if (!enforceVariety) {
                return;
            }

            enforceVariety();
        }

        @Override
        public long fill(@NotNull GasStack resource, GasAction action) {
            return insertionAllowed ? super.fill(resource, action) : 0;
        }

        @Override
        public GasStack drain(@NotNull GasStack resource, GasAction action) {
            return extractionAllowed ? super.drain(resource, action) : GasStack.EMPTY;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return extractionAllowed ? super.drain(maxDrain, action) : GasStack.EMPTY;
        }

        public long forceFill(GasStack resource, GasAction action) {
            return super.fill(resource, action);
        }

        @SuppressWarnings("UnusedReturnValue")
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
            Level level = getWorld();
            if (level == null || level.isClientSide) {
                return;
            }

            sendDataLazily();
        }

        public CompoundTag write(Provider registries) {
            CompoundTag compound = new CompoundTag();
            compound.put(COMPOUND_KEY_TANK_CONTENT, tank.write(registries, new CompoundTag()));
            return compound;
        }

        public void read(@NotNull CompoundTag compound, Provider registries, boolean ignored) {
            if (!compound.contains(COMPOUND_KEY_TANK_CONTENT)) {
                return;
            }

            tank.read(registries, compound.getCompound(COMPOUND_KEY_TANK_CONTENT));
        }
    }
}