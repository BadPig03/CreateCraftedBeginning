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
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IMountedStorageManagerWithGas;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PortableGasInterfaceBlockEntity extends PortableStorageInterfaceBlockEntity implements ThresholdSwitchObservable {
    private final PortableGasInterfaceDisplay display;
    protected IGasHandler capability;

    public PortableGasInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        capability = createEmptyHandler();
        display = new PortableGasInterfaceDisplay(this);
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
        if (connectedEntity == contraption.entity || !(contraption.getStorage() instanceof IMountedStorageManagerWithGas withGas)) {
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

    public void onFacingChanged() {
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

    public boolean isConnected() {
        int timeout = getTransferTimeout();
        return transferTimer >= ANIMATION && transferTimer <= timeout + ANIMATION;
    }

    public float getExtensionDistance(float partialTicks) {
        return display.getExtensionDistance(partialTicks);
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

    IGasHandler getGasCapability() {
        return capability;
    }

    float getConnectionAnimationValue(float partialTicks) {
        return connectionAnimation.getValue(partialTicks);
    }

    boolean canAccessGasStorage(IGasHandler handler) {
        return capability == handler && canTransfer();
    }

    void onGasContentTransferred() {
        onContentTransferred();
    }

    public class InterfaceGasHandler extends PortableGasInterfaceGasHandler {
        public InterfaceGasHandler(IGasHandler wrapped) {
            super(PortableGasInterfaceBlockEntity.this, wrapped);
        }
    }
}
