package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasConnectivityHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightTankMultiblockController {
    private static final int SYNC_RATE = 4;

    private final AbstractAirtightTankBlockEntity owner;
    private BlockPos controllerPos;
    private @Nullable BlockPos lastKnownPos;
    private boolean updateConnectivity;
    private boolean updateCapability;
    private int width = 1;
    private int height = 1;
    private int syncCooldown;
    private boolean queuedSync;

    public AirtightTankMultiblockController(AbstractAirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    public void initialize() {
        sendData();
        if (owner.getLevel() == null || !owner.getLevel().isClientSide) {
            return;
        }

        owner.invalidateRenderBounds();
    }

    public boolean tick() {
        tickSyncCooldown();
        if (lastKnownPos == null) {
            lastKnownPos = owner.getBlockPos();
        }
        else if (!lastKnownPos.equals(owner.getBlockPos())) {
            owner.removeController(true);
            lastKnownPos = owner.getBlockPos();
            return false;
        }

        if (updateCapability) {
            updateCapability = false;
            owner.refreshCapability();
        }
        if (recoverOrphanedMember()) {
            return false;
        }

        if (updateConnectivity) {
            updateConnectivity();
        }
        return owner.isController();
    }

    public void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }

        owner.sendDataImmediately();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    public void updateConnectivity() {
        updateConnectivity = false;
        if (owner.getLevel() == null || owner.getLevel().isClientSide || !owner.isController()) {
            return;
        }

        GasConnectivityHandler.formMulti(owner, owner.getLevel());
    }

    public BlockPos getController() {
        return isController() ? owner.getBlockPos() : controllerPos;
    }

    @SuppressWarnings("unchecked")
    public <T extends BlockEntity & IMultiBlockEntityContainer> @Nullable T getControllerBE() {
        if (isController() || owner.getLevel() == null) {
            return (T) owner;
        }

        BlockPos controllerPosition = controllerPos;
        if (controllerPosition == null || !owner.getLevel().isLoaded(controllerPosition)) {
            return null;
        }

        BlockEntity controllerEntity = owner.getLevel().getBlockEntity(controllerPosition);
        if (controllerEntity == null || controllerEntity.getType() != owner.getType()) {
            return null;
        }
        return controllerEntity instanceof AbstractAirtightTankBlockEntity controllerTank ? (T) controllerTank : null;
    }

    public boolean isController() {
        return controllerPos == null || owner.getBlockPos().equals(controllerPos);
    }

    public void setController(BlockPos newControllerPos) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide && !owner.isVirtual() || newControllerPos.equals(controllerPos)) {
            return;
        }

        controllerPos = newControllerPos;
        owner.refreshCapability();
        owner.notifyUpdate();
    }

    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    public void notifyMultiUpdated() {
        if (owner.getLevel() == null) {
            return;
        }

        owner.updateMultiBlockState();
        owner.onGasStackChanged(owner.getTankInventory().getGasStack());
        owner.afterMultiUpdated();
        owner.setChanged();
    }

    public void removeController(boolean keepFluids) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide) {
            return;
        }

        updateConnectivity = true;
        owner.resetTankBeforeControllerRemoval(keepFluids);
        controllerPos = null;
        width = 1;
        height = 1;
        owner.afterControllerStateCleared(keepFluids);
        owner.resetStandaloneBlockState();
        owner.refreshCapability();
        owner.notifyUpdate();
    }

    private boolean recoverOrphanedMember() {
        if (owner.getLevel() == null || owner.getLevel().isClientSide || owner.isController()) {
            return false;
        }

        BlockPos controllerPosition = controllerPos;
        if (controllerPosition == null || !owner.getLevel().isLoaded(controllerPosition)) {
            return false;
        }

        AbstractAirtightTankBlockEntity controllerTank = owner.getControllerBE();
        if (controllerTank != null && !controllerTank.isRemoved() && controllerTank.isController()) {
            return false;
        }

        owner.removeController(true);
        return true;
    }

    private void tickSyncCooldown() {
        if (syncCooldown <= 0) {
            return;
        }

        syncCooldown--;
        if (syncCooldown != 0 || !queuedSync) {
            return;
        }

        sendData();
    }

    @Nullable
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    public void setLastKnownPos(@Nullable BlockPos lastKnownPos) {
        this.lastKnownPos = lastKnownPos;
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public void setControllerPos(@Nullable BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    public boolean isUpdateConnectivity() {
        return updateConnectivity;
    }

    public void setUpdateConnectivity(boolean updateConnectivity) {
        this.updateConnectivity = updateConnectivity;
    }

    public void requestCapabilityRefresh() {
        updateCapability = true;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = Mth.clamp(width, 1, AbstractAirtightTankBlockEntity.configuredMaxWidth());
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = Mth.clamp(height, 1, AbstractAirtightTankBlockEntity.configuredMaxLength());
    }
}
