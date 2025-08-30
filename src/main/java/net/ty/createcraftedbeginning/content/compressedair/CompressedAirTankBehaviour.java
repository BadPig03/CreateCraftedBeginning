package net.ty.createcraftedbeginning.content.compressedair;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public class CompressedAirTankBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<CompressedAirTankBehaviour> TYPE = new BehaviourType<>();
    public static final BehaviourType<CompressedAirTankBehaviour> INPUT = new BehaviourType<>("Input");
    public static final BehaviourType<CompressedAirTankBehaviour> OUTPUT = new BehaviourType<>("Output");

    private static final int SYNC_RATE = 8;
    private final BehaviourType<CompressedAirTankBehaviour> behaviourType;
    protected int syncCooldown;
    protected boolean queuedSync;
    protected TankSegment[] tanks;
    protected IFluidHandler capability;
    protected boolean extractionAllowed;
    protected boolean insertionAllowed;
    protected Runnable fluidUpdateCallback;

    public CompressedAirTankBehaviour(BehaviourType<CompressedAirTankBehaviour> type, SmartBlockEntity be, int tanks, int tankCapacity, boolean enforceVariety) {
        super(be);
        insertionAllowed = true;
        extractionAllowed = true;
        behaviourType = type;
        this.tanks = new TankSegment[tanks];
        IFluidHandler[] handlers = new IFluidHandler[tanks];
        for (int i = 0; i < tanks; i++) {
            TankSegment tankSegment = new TankSegment(tankCapacity);
            this.tanks[i] = tankSegment;
            handlers[i] = tankSegment.tank;
        }
        capability = new InternalFluidHandler(handlers, enforceVariety);
        fluidUpdateCallback = () -> {};
    }

    public static CompressedAirTankBehaviour single(SmartBlockEntity be, int capacity) {
        return new CompressedAirTankBehaviour(TYPE, be, 1, capacity, false);
    }

    public CompressedAirTankBehaviour whenFluidUpdates(Runnable fluidUpdateCallback) {
        this.fluidUpdateCallback = fluidUpdateCallback;
        return this;
    }

    public CompressedAirTankBehaviour allowInsertion() {
        insertionAllowed = true;
        return this;
    }

    public CompressedAirTankBehaviour allowExtraction() {
        extractionAllowed = true;
        return this;
    }

    public CompressedAirTankBehaviour forbidInsertion() {
        insertionAllowed = false;
        return this;
    }

    public CompressedAirTankBehaviour forbidExtraction() {
        extractionAllowed = false;
        return this;
    }

    public void sendDataImmediately() {
        syncCooldown = 0;
        queuedSync = false;
        updateFluids();
    }

    public void sendDataLazily() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }
        updateFluids();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    protected void updateFluids() {
        fluidUpdateCallback.run();
        blockEntity.sendData();
        blockEntity.setChanged();
    }

    public CompressedAirOnlyFluidTank getPrimaryHandler() {
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

    public IFluidHandler getCapability() {
        return capability;
    }

    @Override
    public BehaviourType<?> getType() {
        return behaviourType;
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void tick() {
        super.tick();

        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync) {
                updateFluids();
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

        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        level.invalidateCapabilities(getPos());
    }

    public class InternalFluidHandler extends CombinedTankWrapper {
        public InternalFluidHandler(IFluidHandler[] handlers, boolean enforceVariety) {
            super(handlers);
            if (enforceVariety) {
                enforceVariety();
            }
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!insertionAllowed) {
                return 0;
            }
            return super.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (!extractionAllowed) {
                return FluidStack.EMPTY;
            }
            return super.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (!extractionAllowed) {
                return FluidStack.EMPTY;
            }
            return super.drain(maxDrain, action);
        }

        public int forceFill(FluidStack resource, FluidAction action) {
            return super.fill(resource, action);
        }

        public FluidStack forceDrain(FluidStack resource, FluidAction action) {
            return super.drain(resource, action);
        }

        public FluidStack forceDrain(int maxDrain, FluidAction action) {
            return super.drain(maxDrain, action);
        }
    }

    public class TankSegment {
        protected CompressedAirOnlyFluidTank tank;

        public TankSegment(int capacity) {
            tank = new CompressedAirOnlyFluidTank(capacity, f -> onFluidStackChanged());
        }

        public void onFluidStackChanged() {
            if (!blockEntity.hasLevel() || getWorld().isClientSide) {
                return;
            }
            sendDataLazily();
        }


        public CompoundTag writeNBT(HolderLookup.Provider registries) {
            CompoundTag compound = new CompoundTag();
            compound.put("TankContent", tank.writeToNBT(registries, new CompoundTag()));
            return compound;
        }

        public void readNBT(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
            tank.readFromNBT(registries, compound.getCompound("TankContent"));
        }
    }
}
