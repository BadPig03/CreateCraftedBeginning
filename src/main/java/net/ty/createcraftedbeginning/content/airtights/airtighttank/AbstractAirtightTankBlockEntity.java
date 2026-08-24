package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTank;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasTankMultiBlockEntityContainer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractAirtightTankBlockEntity extends SmartBlockEntity implements IGasTankMultiBlockEntityContainer {
    protected final AirtightTankMultiblockController multiblockController;
    protected final AirtightTankGasStorage gasStorage;

    protected AbstractAirtightTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        multiblockController = new AirtightTankMultiblockController(this);
        gasStorage = new AirtightTankGasStorage(this);
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

    protected final void initializeTank(GasTank tankInventory) {
        gasStorage.initialize(tankInventory);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
        multiblockController.initialize();
    }

    @Override
    public void tick() {
        super.tick();
        if (!multiblockController.tick()) {
            return;
        }

        tickController();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        gasStorage.invalidate();
    }

    protected void tickController() {
    }

    @Override
    public void sendData() {
        multiblockController.sendData();
    }

    public void updateConnectivity() {
        multiblockController.updateConnectivity();
    }

    protected final void refreshCapability() {
        gasStorage.refreshCapability();
    }

    protected final void onGasStackChanged(GasStack ignored) {
        gasStorage.onGasStackChanged(ignored);
    }

    @Override
    public BlockPos getController() {
        return multiblockController.getController();
    }

    @Override
    public <T extends BlockEntity & IMultiBlockEntityContainer> @Nullable T getControllerBE() {
        return multiblockController.getControllerBE();
    }

    @Override
    public boolean isController() {
        return multiblockController.isController();
    }

    @Override
    public void setController(BlockPos controller) {
        multiblockController.setController(controller);
    }

    @Override
    public void removeController(boolean keepFluids) {
        multiblockController.removeController(keepFluids);
    }

    @Override
    public @Nullable BlockPos getLastKnownPos() {
        return multiblockController.getLastKnownPos();
    }

    @Override
    public void preventConnectivityUpdate() {
        multiblockController.preventConnectivityUpdate();
    }

    @Override
    public final void notifyMultiUpdated() {
        multiblockController.notifyMultiUpdated();
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
        return multiblockController.getHeight();
    }

    @Override
    public void setHeight(int height) {
        multiblockController.setHeight(height);
    }

    @Override
    public int getWidth() {
        return multiblockController.getWidth();
    }

    @Override
    public void setWidth(int width) {
        multiblockController.setWidth(width);
    }

    protected abstract void updateMultiBlockState();

    protected void afterMultiUpdated() {
    }

    protected abstract void resetTankBeforeControllerRemoval(boolean keepFluids);

    protected void afterControllerStateCleared(boolean keepFluids) {
    }

    protected abstract void resetStandaloneBlockState();

    @Override
    protected AABB createRenderBoundingBox() {
        if (!isController()) {
            return super.createRenderBoundingBox();
        }

        Axis connectionAxis = getMainConnectionAxis();
        int xSize = connectionAxis == Axis.X ? getHeight() : getWidth();
        int ySize = connectionAxis == Axis.Y ? getHeight() : getWidth();
        int zSize = connectionAxis == Axis.Z ? getHeight() : getWidth();
        return super.createRenderBoundingBox().expandTowards(xSize - 1, ySize - 1, zSize - 1);
    }

    public GasTank getTankInventory() {
        return gasStorage.getTankInventory();
    }

    public IGasHandler getCapability() {
        return gasStorage.getCapability();
    }

    @Override
    public IGasTank getTank(int tank) {
        return getTankInventory();
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public GasStack getGas(int tank) {
        return getTankInventory().getGasStack().copy();
    }

    @Override
    public long getTankSize(int tank) {
        return capacityPerBlock();
    }

    protected abstract long capacityPerBlock();

    public final AirtightTankMultiblockController multiblockController() {
        return multiblockController;
    }

    public final void sendDataImmediately() {
        super.sendData();
    }

    protected final void invalidateRenderBounds() {
        invalidateRenderBoundingBox();
    }

    public final void invalidateGasCapabilities() {
        invalidateCapabilities();
    }
}
