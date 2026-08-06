package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gas.gases.GasConnectivityHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTankMultiBlockEntityContainer;
import net.ty.createcraftedbeginning.config.CCBConfig;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractAirtightTankBlockEntity extends SmartBlockEntity implements IGasTankMultiBlockEntityContainer {
    protected static final String COMPOUND_KEY_UPDATE_CONNECTIVITY = "UpdateConnectivity";
    protected static final String COMPOUND_KEY_LAST_KNOWN_POS = "LastKnownPos";
    protected static final String COMPOUND_KEY_CONTROLLER_POS = "Controller";
    protected static final String COMPOUND_KEY_TANK_CONTENT = "TankContent";
    protected static final String COMPOUND_KEY_WIDTH = "Width";
    protected static final String COMPOUND_KEY_HEIGHT = "Height";
    private static final int SYNC_RATE = 4;
    protected IGasHandler gasCapability;
    protected GasTank tankInventory;
    protected BlockPos controllerPos;
    protected BlockPos lastKnownPos;
    protected boolean updateConnectivity;
    protected boolean updateCapability;
    protected int width;
    protected int height;
    protected int syncCooldown;
    protected boolean queuedSync;

    protected AbstractAirtightTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        width = 1;
        height = 1;
    }

    protected static int configuredMaxLength() {
        return Math.max(1, CCBConfig.server().airtights.maxAirtightTankLength.get());
    }

    protected static int configuredMaxWidth() {
        return Math.max(1, CCBConfig.server().airtights.maxAirtightTankWidth.get());
    }

    protected static int calculateCoords(BlockPos pos, Axis axis) {
        return axis.choose(pos.getX(), pos.getY(), pos.getZ());
    }

    @Nullable
    protected static BlockPos readOptionalBlockPos(CompoundTag tag, String key) {
        return tag.contains(key) ? NBTHelper.readBlockPos(tag, key) : null;
    }

    protected static int readDimension(CompoundTag tag, String key, int maxValue) {
        if (!tag.contains(key)) {
            return 1;
        }
        return Mth.clamp(tag.getInt(key), 1, maxValue);
    }

    protected final void initializeTank(GasTank tankInventory) {
        this.tankInventory = tankInventory;
        refreshCapability();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
        sendData();
        if (level != null && level.isClientSide) {
            invalidateRenderBoundingBox();
        }
    }

    @Override
    public void tick() {
        super.tick();
        tickSyncCooldown();

        if (lastKnownPos == null) {
            lastKnownPos = getBlockPos();
        }
        else if (!lastKnownPos.equals(worldPosition)) {
            onPositionChanged();
            return;
        }

        if (updateCapability) {
            updateCapability = false;
            refreshCapability();
        }
        if (updateConnectivity) {
            updateConnectivity();
        }
        if (isController()) {
            tickController();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    protected void tickController() {
    }

    private void tickSyncCooldown() {
        if (syncCooldown <= 0) {
            return;
        }

        syncCooldown--;
        if (syncCooldown == 0 && queuedSync) {
            sendData();
        }
    }

    @Override
    public void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }

        super.sendData();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    public void updateConnectivity() {
        updateConnectivity = false;
        if (level == null || level.isClientSide || !isController()) {
            return;
        }

        GasConnectivityHandler.formMulti(this, level);
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    protected final void refreshCapability() {
        gasCapability = handlerForCapability();
        invalidateCapabilities();
    }

    private IGasHandler handlerForCapability() {
        if (isController()) {
            return tankInventory;
        }

        AbstractAirtightTankBlockEntity controller = getControllerBE();
        return controller != null ? controller.handlerForCapability() : new GasTank(0);
    }

    protected final void onGasStackChanged(GasStack ignored) {
        if (!isController() || level == null || level.isClientSide) {
            return;
        }

        notifyUpdate();
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controllerPos;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity & IMultiBlockEntityContainer> @Nullable T getControllerBE() {
        if (isController() || level == null) {
            return (T) this;
        }

        BlockPos controller = controllerPos;
        if (controller == null || !level.isLoaded(controller)) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(controller);
        if (blockEntity == null || blockEntity.getType() != getType()) {
            return null;
        }
        return blockEntity instanceof AbstractAirtightTankBlockEntity tank ? (T) tank : null;
    }

    @Override
    public boolean isController() {
        return controllerPos == null || worldPosition.equals(controllerPos);
    }

    @Override
    public void setController(BlockPos controller) {
        if (level == null || level.isClientSide && !isVirtual() || controller.equals(controllerPos)) {
            return;
        }

        controllerPos = controller;
        refreshCapability();
        notifyUpdate();
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public final void notifyMultiUpdated() {
        if (level == null) {
            return;
        }

        updateMultiBlockState();
        onGasStackChanged(tankInventory.getGasStack());
        afterMultiUpdated();
        setChanged();
    }

    @Override
    public Axis getMainConnectionAxis() {
        return Axis.Y;
    }

    @Override
    public int getMaxLength(Axis longAxis, int width) {
        return configuredMaxLength();
    }

    @Override
    public int getMaxWidth() {
        return configuredMaxWidth();
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = Mth.clamp(height, 1, configuredMaxLength());
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = Mth.clamp(width, 1, configuredMaxWidth());
    }

    protected abstract void updateMultiBlockState();

    protected void afterMultiUpdated() {
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if (!isController()) {
            return super.createRenderBoundingBox();
        }

        Axis axis = getMainConnectionAxis();
        int xSize = axis == Axis.X ? height : width;
        int ySize = axis == Axis.Y ? height : width;
        int zSize = axis == Axis.Z ? height : width;
        return super.createRenderBoundingBox().expandTowards(xSize - 1, ySize - 1, zSize - 1);
    }

    public GasTank getTankInventory() {
        return tankInventory;
    }

    public IGasHandler getCapability() {
        return gasCapability;
    }

    @Override
    public IGasTank getTank(int tank) {
        return tankInventory;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public GasStack getGas(int tank) {
        return tankInventory.getGasStack().copy();
    }

    @Override
    public long getTankSize(int tank) {
        return capacityPerBlock();
    }

    protected abstract long capacityPerBlock();
}
