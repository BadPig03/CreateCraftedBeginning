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
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
abstract class AbstractSmartGasTankBehaviour extends BlockEntityBehaviour {
    private static final String COMPOUND_KEY_TANK_CONTENT = "TankContent";
    private static final String COMPOUND_KEY_TANKS_SUFFIX = "Tanks";

    private static final int SYNC_RATE = 8;

    private final BehaviourType<?> behaviourType;
    IGasHandler capability;
    boolean extractionAllowed;
    boolean insertionAllowed;
    Runnable gasUpdateCallback;
    private int syncCooldown;
    private boolean queuedSync;

    AbstractSmartGasTankBehaviour(BehaviourType<?> type, SmartBlockEntity be) {
        super(be);
        insertionAllowed = true;
        extractionAllowed = true;
        behaviourType = type;
        gasUpdateCallback = () -> {};
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
        TankSegmentBase[] tankSegments = getTankSegmentsForLifecycle();
        MutableInt tankIndex = new MutableInt(0);
        ListTag tankData = CCBNbtUtils.getList(compoundTag, getTankDataKey(), Tag.TAG_COMPOUND);
        NBTHelper.iterateCompoundList(tankData, tankTag -> {
            if (tankIndex.intValue() >= tankSegments.length) {
                return;
            }

            tankSegments[tankIndex.intValue()].read(tankTag, provider);
            tankIndex.increment();
        });
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        ListTag tankData = new ListTag();
        for (TankSegmentBase tankSegment : getTankSegmentsForLifecycle()) {
            tankData.add(tankSegment.write(provider));
        }
        CCBNbtUtils.putTag(compoundTag, getTankDataKey(), tankData);
    }

    private String getTankDataKey() {
        return getType().getName() + COMPOUND_KEY_TANKS_SUFFIX;
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
        updateGases();
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

    public IGasHandler getCapability() {
        return capability;
    }

    abstract TankSegmentBase[] getTankSegmentsForLifecycle();

    void sendDataLazily() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }

        updateGases();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    private void updateGases() {
        gasUpdateCallback.run();
        blockEntity.sendData();
        blockEntity.setChanged();
    }

    abstract class TankSegmentBase {
        abstract SmartGasTank getTank();

        void onGasStackChanged() {
            Level level = getWorld();
            if (level == null || level.isClientSide) {
                return;
            }

            sendDataLazily();
        }

        private CompoundTag write(Provider provider) {
            CompoundTag tankTag = new CompoundTag();
            CCBNbtUtils.putTag(tankTag, COMPOUND_KEY_TANK_CONTENT, getTank().write(provider, new CompoundTag()));
            return tankTag;
        }

        private void read(CompoundTag compoundTag, Provider provider) {
            if (!CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_TANK_CONTENT)) {
                return;
            }

            getTank().read(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_TANK_CONTENT));
        }
    }

    class InternalGasHandlerBase extends CombinedGasTankWrapper {
        InternalGasHandlerBase(IGasHandler[] handlers, boolean enforceVariety) {
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
