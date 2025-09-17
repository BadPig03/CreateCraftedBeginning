package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasTank;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.mixin.MountedStorageManagerAccessor;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class PortableGasInterfaceBlockEntity extends PortableStorageInterfaceBlockEntity {
    protected IGasHandler capability;

    public PortableGasInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        capability = createEmptyHandler();
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasCapabilities.GasHandler.BLOCK, CCBBlockEntities.PORTABLE_GAS_INTERFACE.get(), (be, context) -> be.capability);
    }

    @Override
    public void startTransferringTo(@NotNull Contraption contraption, float distance) {
        if (contraption.getStorage() instanceof MountedStorageManagerAccessor accessor) {
            capability = new InterfaceGasHandler(accessor.getGases());
            invalidateCapability();
            super.startTransferringTo(contraption, distance);
        }
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

    public boolean isConnected() {
        int timeUnit = getTransferTimeout();
        return transferTimer >= ANIMATION && transferTimer <= timeUnit + ANIMATION;
    }

    public float getExtensionDistance(float partialTicks) {
		return (float) (Math.pow(connectionAnimation.getValue(partialTicks), 2) * distance / 2);
	}

    public Entity getConnectedEntity() {
        return connectedEntity;
    }

    public float getDistance() {
        return distance;
    }

    public int getTransferTimer() {
        return transferTimer;
    }

    @Contract(" -> new")
    private @NotNull IGasHandler createEmptyHandler() {
        return new InterfaceGasHandler(new GasTank(0));
    }

    public class InterfaceGasHandler implements IGasHandler {
        private final IGasHandler wrapped;

        public InterfaceGasHandler(IGasHandler wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public int getTanks() {
            return wrapped.getTanks();
        }

        @Override
        public GasStack getGasInTank(int tank) {
            return wrapped.getGasInTank(tank);
        }

        @Override
        public long getTankCapacity(int tank) {
            return wrapped.getTankCapacity(tank);
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return wrapped.isGasValid(tank, stack);
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (!isConnected()) {
                return 0;
            }
            long fill = wrapped.fill(resource, action);
            if (fill > 0 && action.execute()) {
                keepAlive();
            }
            return fill;
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (!canTransfer()) {
                return GasStack.EMPTY;
            }
            GasStack drain = wrapped.drain(resource, action);
            if (!drain.isEmpty() && action.execute()) {
                keepAlive();
            }
            return drain;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            if (!canTransfer()) {
                return GasStack.EMPTY;
            }
            GasStack drain = wrapped.drain(maxDrain, action);
            if (!drain.isEmpty() && action.execute()) {
                keepAlive();
            }
            return drain;
        }

        public void keepAlive() {
            onContentTransferred();
        }
    }
}
