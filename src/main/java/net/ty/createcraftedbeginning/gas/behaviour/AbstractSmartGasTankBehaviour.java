package net.ty.createcraftedbeginning.gas.behaviour;

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
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.gas.storage.SmartGasTank;
import net.ty.createcraftedbeginning.gas.storage.handler.CombinedGasStorageHandler;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
abstract class AbstractSmartGasTankBehaviour extends BlockEntityBehaviour {
    private static final String COMPOUND_KEY_TANK_CONTENT = "TankContent";
    private static final String COMPOUND_KEY_TANKS_SUFFIX = "Tanks";

    private static final int SYNC_RATE = 8;

    private final BehaviourType<?> behaviourType;

    GasStorageHandler capability;
    Runnable tankUpdateCallback;
    boolean extractionAllowed;
    boolean insertionAllowed;
    private int syncCooldown;
    private boolean queuedSync;
    private final List<PendingOverflowRelease> pendingOverflowReleases = new ArrayList<>();

    AbstractSmartGasTankBehaviour(BehaviourType<?> type, SmartBlockEntity blockEntity) {
        super(blockEntity);
        insertionAllowed = true;
        extractionAllowed = true;
        behaviourType = type;
        tankUpdateCallback = () -> {};
    }

    @Override
    public BehaviourType<?> getType() {
        return behaviourType;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (getWorld().isClientSide) {
            pendingOverflowReleases.clear();
            return;
        }

        releasePendingOverflow();
        for (TankSegmentBase tank : getTankSegments()) {
            tank.onTankStateChanged();
        }
    }

    @Override
    public void tick() {
        super.tick();
        releasePendingOverflow();
        if (syncCooldown <= 0) {
            return;
        }

        syncCooldown--;
        if (syncCooldown != 0 || !queuedSync) {
            return;
        }

        updateTankState();
    }

    @Override
    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!clientPacket) {
            pendingOverflowReleases.clear();
        }

        Level level = blockEntity.getLevel();
        boolean recoverOverflow = !clientPacket && (level == null || !level.isClientSide);
        TankSegmentBase[] tankSegments = getTankSegments();
        MutableInt tankIndex = new MutableInt(0);
        ListTag tankData = compoundTag.getList(getTankDataKey(), Tag.TAG_COMPOUND);
        NBTHelper.iterateCompoundList(tankData, tankTag -> {
            if (tankIndex.intValue() >= tankSegments.length) {
                return;
            }

            tankSegments[tankIndex.intValue()].read(tankTag, provider, recoverOverflow);
            tankIndex.increment();
        });
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        ListTag tankData = new ListTag();
        for (TankSegmentBase tankSegment : getTankSegments()) {
            tankData.add(tankSegment.write(provider));
        }
        compoundTag.put(getTankDataKey(), tankData);
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

    public void sendDataImmediately() {
        syncCooldown = 0;
        queuedSync = false;
        updateTankState();
    }

    public boolean isEmpty() {
        for (int tankIndex = 0; tankIndex < capability.getTanks(); tankIndex++) {
            if (capability.getGasInTank(tankIndex).isEmpty()) {
                continue;
            }

            return false;
        }
        return true;
    }

    public GasStorageHandler getCapability() {
        return capability;
    }

    abstract TankSegmentBase[] getTankSegments();

    void sendDataLazily() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }

        updateTankState();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    private void releasePendingOverflow() {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide || pendingOverflowReleases.isEmpty()) {
            return;
        }

        for (PendingOverflowRelease pendingRelease : pendingOverflowReleases) {
            GasReleaseService.release(level, GasReleaseRequest.radial(pendingRelease.gas(), getPos(), GasReleaseCause.OVERFLOW, pendingRelease.sourcePressurePa()));
        }
        pendingOverflowReleases.clear();
    }

    private String getTankDataKey() {
        return getType().getName() + COMPOUND_KEY_TANKS_SUFFIX;
    }

    private void updateTankState() {
        tankUpdateCallback.run();
        blockEntity.sendData();
        blockEntity.setChanged();
    }

    abstract class TankSegmentBase {
        abstract SmartGasTank getTank();

        void onTankStateChanged() {
            Level level = getWorld();
            if (level == null || level.isClientSide) {
                return;
            }

            sendDataLazily();
        }

        private CompoundTag write(Provider provider) {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.put(COMPOUND_KEY_TANK_CONTENT, getTank().write(provider, new CompoundTag()));
            return compoundTag;
        }

        private void read(CompoundTag compoundTag, Provider provider, boolean recoverOverflow) {
            if (!compoundTag.contains(COMPOUND_KEY_TANK_CONTENT)) {
                return;
            }

            SmartGasTank tank = getTank();
            GasStack overflow = tank.readClampedToLimits(provider, compoundTag.getCompound(COMPOUND_KEY_TANK_CONTENT));
            if (!recoverOverflow || overflow.isEmpty()) {
                return;
            }

            long loadedAmount = BoundedMath.saturatedAdd(tank.getStoredAmount(), overflow.getAmount());
            long sourcePressurePa = GasPressure.pressure(loadedAmount, tank.getVolume());
            pendingOverflowReleases.add(new PendingOverflowRelease(overflow, sourcePressurePa));
        }
    }

    private record PendingOverflowRelease(GasStack gas, long sourcePressurePa) {
    }

    class InternalGasHandlerBase extends CombinedGasStorageHandler {
        InternalGasHandlerBase(GasStorageHandler[] handlers, boolean enforceVariety) {
            super(handlers);
            if (!enforceVariety) {
                return;
            }

            enforceVariety();
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (!extractionAllowed) {
                return GasStack.EMPTY;
            }

            return drainAllowed(resource, action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            if (!extractionAllowed) {
                return GasStack.EMPTY;
            }

            return drainAllowed(maxDrain, action);
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (!insertionAllowed) {
                return 0;
            }

            return fillAllowed(resource, action);
        }

        @Override
        protected boolean canDrainPressureCompartment(int tank) {
            return extractionAllowed;
        }

        @Override
        protected boolean canFillPressureCompartment(int tank) {
            return insertionAllowed;
        }

        public long forceFill(GasStack resource, GasAction action) {
            return fillAllowed(resource, action);
        }

        @SuppressWarnings("unused")
        public GasStack forceDrain(GasStack resource, GasAction action) {
            return drainAllowed(resource, action);
        }

        @SuppressWarnings("unused")
        public GasStack forceDrain(long maxDrain, GasAction action) {
            return drainAllowed(maxDrain, action);
        }

        private GasStack drainAllowed(GasStack resource, GasAction action) {
            return super.drain(resource, action);
        }

        private GasStack drainAllowed(long maxDrain, GasAction action) {
            return super.drain(maxDrain, action);
        }

        private long fillAllowed(GasStack resource, GasAction action) {
            return super.fill(resource, action);
        }
    }
}
