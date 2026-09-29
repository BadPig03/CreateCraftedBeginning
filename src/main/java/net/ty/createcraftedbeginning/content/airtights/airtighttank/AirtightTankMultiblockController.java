package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.gas.multiblock.GasTankMultiblockConnectivity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightTankMultiblockController {
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

    AirtightTankMultiblockController(AbstractAirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    void initialize() {
        sendData();
        Level level = owner.getLevel();
        if (level == null || !level.isClientSide) {
            return;
        }

        owner.invalidateRenderBounds();
    }

    boolean tick() {
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

    void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }

        owner.sendDataImmediately();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    void updateConnectivity() {
        updateConnectivity = false;
        Level level = owner.getLevel();
        if (level == null || level.isClientSide || !owner.isController()) {
            return;
        }

        GasTankMultiblockConnectivity.formMultiblock(owner, level);
    }

    BlockPos getController() {
        if (!isController()) {
            return controllerPos;
        }

        return owner.getBlockPos();
    }

    @SuppressWarnings("unchecked")
    <T extends BlockEntity & IMultiBlockEntityContainer> @Nullable T getControllerBE() {
        Level level = owner.getLevel();
        if (isController() || level == null) {
            return (T) owner;
        }

        BlockPos controllerPosition = controllerPos;
        if (controllerPosition == null || !level.isLoaded(controllerPosition)) {
            return null;
        }

        BlockEntity controllerEntity = level.getBlockEntity(controllerPosition);
        if (controllerEntity == null || controllerEntity.getType() != owner.getType() || !(controllerEntity instanceof AbstractAirtightTankBlockEntity controllerTank)) {
            return null;
        }

        return (T) controllerTank;
    }

    boolean isController() {
        return controllerPos == null || owner.getBlockPos().equals(controllerPos);
    }

    void setController(BlockPos newControllerPos) {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide && !owner.isVirtual() || newControllerPos.equals(controllerPos)) {
            return;
        }

        controllerPos = newControllerPos;
        owner.refreshCapability();
        owner.notifyUpdate();
    }

    void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    void notifyMultiUpdated() {
        if (owner.getLevel() == null) {
            return;
        }

        owner.updateMultiBlockState();
        owner.onTankStateChanged();
        owner.afterMultiUpdated();
        owner.setChanged();
    }

    void removeController(boolean keepFluids) {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) {
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

    @Nullable BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    void setLastKnownPos(@Nullable BlockPos lastKnownPos) {
        this.lastKnownPos = lastKnownPos;
    }

    @Nullable BlockPos getControllerPos() {
        return controllerPos;
    }

    void setControllerPos(@Nullable BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    boolean isUpdateConnectivity() {
        return updateConnectivity;
    }

    void setUpdateConnectivity(boolean updateConnectivity) {
        this.updateConnectivity = updateConnectivity;
    }

    void requestCapabilityRefresh() {
        updateCapability = true;
    }

    int getWidth() {
        return width;
    }

    void setWidth(int width) {
        this.width = Mth.clamp(width, 1, AbstractAirtightTankBlockEntity.configuredMaxWidth());
    }

    int getHeight() {
        return height;
    }

    void setHeight(int height) {
        this.height = Mth.clamp(height, 1, AbstractAirtightTankBlockEntity.configuredMaxLength());
    }

    private boolean recoverOrphanedMember() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide || owner.isController()) {
            return false;
        }

        BlockPos controllerPosition = controllerPos;
        if (controllerPosition == null || !level.isLoaded(controllerPosition)) {
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
}
