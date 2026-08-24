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
    private final AirtightTankMultiblockController multiblockController;
    private final AirtightTankGasStorage gasStorage;

    protected AbstractAirtightTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        multiblockController = new AirtightTankMultiblockController(this);
        gasStorage = new AirtightTankGasStorage(this);
    }

    static int configuredMaxLength() {
        return Math.max(1, CCBConfig.server().airtights.maxAirtightTankLength.get());
    }

    static int configuredMaxWidth() {
        return Math.max(1, CCBConfig.server().airtights.maxAirtightTankWidth.get());
    }

    protected static int calculateCoords(BlockPos pos, Axis axis) {
        return axis.choose(pos.getX(), pos.getY(), pos.getZ());
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

    @Override
    public void sendData() {
        multiblockController.sendData();
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

    public GasTank getTankInventory() {
        return gasStorage.getTankInventory();
    }

    public IGasHandler getCapability() {
        return gasStorage.getCapability();
    }

    protected abstract void updateMultiBlockState();

    protected final void initializeTank(GasTank tankInventory) {
        gasStorage.initialize(tankInventory);
    }

    protected final void updateConnectivity() {
        multiblockController.updateConnectivity();
    }

    protected final void onGasStackChanged(GasStack ignored) {
        gasStorage.onGasStackChanged(ignored);
    }

    protected final void invalidateRenderBounds() {
        invalidateRenderBoundingBox();
    }

    protected abstract void resetTankBeforeControllerRemoval(boolean keepFluids);

    protected abstract void resetStandaloneBlockState();

    protected abstract long capacityPerBlock();

    void tickController() {
    }

    void afterMultiUpdated() {
    }

    void afterControllerStateCleared(boolean keepFluids) {
    }

    final AirtightTankMultiblockController multiblockController() {
        return multiblockController;
    }

    final void refreshCapability() {
        gasStorage.refreshCapability();
    }

    final void sendDataImmediately() {
        super.sendData();
    }

    final void invalidateGasCapabilities() {
        invalidateCapabilities();
    }
}
