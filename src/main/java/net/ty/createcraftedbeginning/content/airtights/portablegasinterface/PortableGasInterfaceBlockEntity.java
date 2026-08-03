package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IMountedStorageManagerWithGas;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PortableGasInterfaceBlockEntity extends PortableStorageInterfaceBlockEntity implements ThresholdSwitchObservable {
    protected IGasHandler capability;

    public PortableGasInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        capability = createEmptyHandler();
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.PORTABLE_GAS_INTERFACE.get(), (blockEntity, context) -> blockEntity.capability);
    }

    @Contract(" -> new")
    private IGasHandler createEmptyHandler() {
        return new InterfaceGasHandler(new GasTank(0));
    }

    @Override
    public void startTransferringTo(Contraption contraption, float distance) {
        if (!(contraption.getStorage() instanceof IMountedStorageManagerWithGas withGas)) {
            return;
        }

        capability = new InterfaceGasHandler(withGas.ccb$getGases());
        invalidateCapability();
        super.startTransferringTo(contraption, distance);
    }

    @Override
    protected void stopTransferring() {
        capability = createEmptyHandler();
        invalidateCapability();
        super.stopTransferring();
    }

    @Override
    protected void invalidateCapability() {
        invalidateCapabilities();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    public boolean isConnected() {
        int timeout = getTransferTimeout();
        return transferTimer >= ANIMATION && transferTimer <= timeout + ANIMATION;
    }

    public float getExtensionDistance(float partialTicks) {
        float animation = connectionAnimation.getValue(partialTicks);
        return Mth.square(animation) * distance * 0.5f;
    }

    @Nullable
    public Entity getConnectedEntity() {
        return connectedEntity;
    }

    public float getDistance() {
        return distance;
    }

    public int getTransferTimer() {
        return transferTimer;
    }

    @Override
    public int getMaxValue() {
        return GasAmountUtils.toWholeBucketsClamped(capability.getTankCapacity(0));
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return GasAmountUtils.toWholeBucketsClamped(capability.getGasInTank(0).getAmount());
    }

    @Override
    public MutableComponent format(int value) {
        return GasAmountUtils.formatWholeBuckets(value);
    }

    public class InterfaceGasHandler implements IGasHandler {
        private final IGasHandler wrapped;

        public InterfaceGasHandler(IGasHandler wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return wrapped.isGasValid(tank, stack);
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (!canTransfer()) {
                return GasStack.EMPTY;
            }

            GasStack drained = wrapped.drain(resource, action);
            keepAliveIfTransferred(!drained.isEmpty(), action);
            return drained;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            if (!canTransfer()) {
                return GasStack.EMPTY;
            }

            GasStack drained = wrapped.drain(maxDrain, action);
            keepAliveIfTransferred(!drained.isEmpty(), action);
            return drained;
        }

        @Override
        public GasStack getGasInTank(int tank) {
            return wrapped.getGasInTank(tank);
        }

        @Override
        public int getTanks() {
            return wrapped.getTanks();
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (!isConnected()) {
                return 0;
            }

            long filled = wrapped.fill(resource, action);
            keepAliveIfTransferred(filled > 0, action);
            return filled;
        }

        @Override
        public long getTankCapacity(int tank) {
            return wrapped.getTankCapacity(tank);
        }

        private void keepAliveIfTransferred(boolean transferred, GasAction action) {
            if (!transferred || !action.execute()) {
                return;
            }

            keepAlive();
        }

        public void keepAlive() {
            onContentTransferred();
        }
    }
}
