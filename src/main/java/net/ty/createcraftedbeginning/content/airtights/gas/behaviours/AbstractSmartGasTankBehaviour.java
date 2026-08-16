package net.ty.createcraftedbeginning.content.airtights.gas.behaviours;

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

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractSmartGasTankBehaviour extends BlockEntityBehaviour {
    protected static final String COMPOUND_KEY_TANK_CONTENT = "TankContent";

    private static final int SYNC_RATE = 8;

    protected final BehaviourType<?> behaviourType;

    protected int syncCooldown;
    protected boolean queuedSync;
    protected IGasHandler capability;
    protected boolean extractionAllowed;
    protected boolean insertionAllowed;
    protected Runnable gasUpdateCallback;

    protected AbstractSmartGasTankBehaviour(BehaviourType<?> type, SmartBlockEntity be) {
        super(be);
        insertionAllowed = true;
        extractionAllowed = true;
        behaviourType = type;
        gasUpdateCallback = () -> {};
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

    public boolean isEmpty() {
        for (int tank = 0; tank < capability.getTanks(); tank++) {
            if (!capability.getGasInTank(tank).isEmpty()) {
                return false;
            }
        }
        return true;
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

        for (TankSegmentBase tank : getTankSegmentsForLifecycle()) {
            tank.onGasStackChanged();
        }
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
    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        TankSegmentBase[] tanks = getTankSegmentsForLifecycle();
        MutableInt index = new MutableInt(0);
        ListTag tankData = compoundTag.getList(getType().getName() + "Tanks", Tag.TAG_COMPOUND);
        NBTHelper.iterateCompoundList(tankData, tankTag -> {
            if (index.intValue() >= tanks.length) {
                return;
            }

            tanks[index.intValue()].read(tankTag, provider);
            index.increment();
        });
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        ListTag tankData = new ListTag();
        for (TankSegmentBase tank : getTankSegmentsForLifecycle()) {
            tankData.add(tank.write(provider));
        }
        compoundTag.put(getType().getName() + "Tanks", tankData);
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

    protected abstract TankSegmentBase[] getTankSegmentsForLifecycle();

    protected abstract class TankSegmentBase {
        protected abstract SmartGasTank getTank();

        public void onGasStackChanged() {
            Level level = getWorld();
            if (level == null || level.isClientSide) {
                return;
            }

            sendDataLazily();
        }

        public CompoundTag write(Provider provider) {
            CompoundTag tag = new CompoundTag();
            tag.put(COMPOUND_KEY_TANK_CONTENT, getTank().write(provider, new CompoundTag()));
            return tag;
        }

        public void read(CompoundTag compoundTag, Provider provider) {
            if (!compoundTag.contains(COMPOUND_KEY_TANK_CONTENT)) {
                return;
            }

            getTank().read(provider, compoundTag.getCompound(COMPOUND_KEY_TANK_CONTENT));
        }
    }

    protected class InternalGasHandlerBase extends CombinedGasTankWrapper {
        protected InternalGasHandlerBase(IGasHandler[] handlers, boolean enforceVariety) {
            super(handlers);
            if (!enforceVariety) {
                return;
            }

            enforceVariety();
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return extractionAllowed ? drainAllowed(resource, action) : GasStack.EMPTY;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return extractionAllowed ? drainAllowed(maxDrain, action) : GasStack.EMPTY;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            return insertionAllowed ? fillAllowed(resource, action) : 0;
        }

        protected GasStack drainAllowed(GasStack resource, GasAction action) {
            return super.drain(resource, action);
        }

        protected GasStack drainAllowed(long maxDrain, GasAction action) {
            return super.drain(maxDrain, action);
        }

        protected long fillAllowed(GasStack resource, GasAction action) {
            return super.fill(resource, action);
        }

        public long forceFill(GasStack resource, GasAction action) {
            return fillAllowed(resource, action);
        }

        @SuppressWarnings("unused")
        public GasStack forceDrain(GasStack resource, GasAction action) {
            return drainAllowed(resource, action);
        }

        public GasStack forceDrain(long maxDrain, GasAction action) {
            return drainAllowed(maxDrain, action);
        }
    }
}
