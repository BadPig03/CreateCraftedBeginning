package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageAccess;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PortableGasInterfaceBlockEntity extends PortableStorageInterfaceBlockEntity implements ThresholdSwitchObservable {
    private final PortableGasInterfaceDisplay display;
    private GasStorageHandler capability;

    public PortableGasInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        capability = createEmptyHandler();
        display = new PortableGasInterfaceDisplay(this);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasCapabilities.BLOCK, CCBBlockEntities.PORTABLE_GAS_INTERFACE.get(), (blockEntity, context) -> blockEntity.capability);
    }

    @Override
    public void startTransferringTo(Contraption contraption, float distance) {
        if (connectedEntity == contraption.entity || !(contraption.getStorage() instanceof MountedGasStorageAccess mountedStorage)) {
            return;
        }

        capability = new InterfaceGasHandler(mountedStorage.ccb$getGasStorage());
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

    @Override
    public int getMaxValue() {
        return display.getMaxValue();
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return display.getCurrentValue();
    }

    @Override
    public MutableComponent format(int value) {
        return display.format(value);
    }

    GasStorageHandler getGasCapability() {
        return capability;
    }

    boolean canAccessGasStorage(GasHandler gasHandler) {
        return capability == gasHandler && canTransfer();
    }

    boolean isConnected() {
        int transferTimeout = getTransferTimeout();
        return transferTimer >= ANIMATION && transferTimer <= transferTimeout + ANIMATION;
    }

    void onFacingChanged() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (connectedEntity != null || keepAlive > 0) {
            keepAlive = 0;
            stopTransferring();
            transferTimer = ANIMATION - 1;
            sendData();
        }
        level.getEntitiesOfClass(AbstractContraptionEntity.class, new AABB(worldPosition).inflate(3)).forEach(AbstractContraptionEntity::refreshPSIs);
    }

    float getConnectionAnimationValue(float partialTicks) {
        return connectionAnimation.getValue(partialTicks);
    }

    float getExtensionDistance(float partialTicks) {
        return display.getExtensionDistance(partialTicks);
    }

    float getDistance() {
        return distance;
    }

    @Nullable Entity getConnectedEntity() {
        return connectedEntity;
    }

    int getTransferTimer() {
        return transferTimer;
    }

    void onGasContentTransferred() {
        onContentTransferred();
    }

    private GasStorageHandler createEmptyHandler() {
        return new InterfaceGasHandler(new GasTank(0));
    }

    private class InterfaceGasHandler extends PortableGasInterfaceGasHandler {
        private InterfaceGasHandler(GasStorageHandler wrapped) {
            super(PortableGasInterfaceBlockEntity.this, wrapped);
        }
    }
}
