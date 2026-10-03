package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AbstractAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.ChamberGasTank;
import net.ty.createcraftedbeginning.gas.multiblock.GasTankMultiblockPart;
import net.ty.createcraftedbeginning.gas.storage.CreativeGasReservoir;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeAirtightTankBlockEntity extends AbstractAirtightTankBlockEntity implements IHaveGoggleInformation, ChamberGasTank, CreativeGasContainer, ThresholdSwitchObservable {
    private final CreativeAirtightTankStorageController storageController;
    private final CreativeAirtightTankDisplay display;
    private final CreativeAirtightTankSerialization serialization;
    private CreativeAirtightTankPressureBehaviour pressureBehaviour;

    public CreativeAirtightTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        storageController = new CreativeAirtightTankStorageController(this);
        display = new CreativeAirtightTankDisplay(this);
        serialization = new CreativeAirtightTankSerialization(this, storageController);
        initializeTank(new CreativeGasReservoir(getVolumePerBlock(), CreativeAirtightTankPressureBehaviour.MAX_PRESSURE_PA, this::onTankStateChanged));
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasCapabilities.BLOCK, CCBBlockEntities.CREATIVE_AIRTIGHT_TANK.get(), (tank, ignoredDirection) -> tank.getCapability());
    }

    static long getVolumePerBlock() {
        return Integer.MAX_VALUE * GasUnits.LITERS_PER_KILOLITER;
    }

    @Override
    public CreativeGasReservoir getTankInventory() {
        return (CreativeGasReservoir) super.getTankInventory();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        pressureBehaviour = new CreativeAirtightTankPressureBehaviour(this);
        behaviours.add(pressureBehaviour);
    }

    @Override
    public void removeController(boolean keepFluids) {
        super.removeController(keepFluids);
    }

    @Override
    protected void updateMultiBlockState() {
        if (level == null) {
            return;
        }

        BlockState tankState = getBlockState();
        if (!(tankState.getBlock() instanceof CreativeAirtightTankBlock)) {
            return;
        }

        Axis connectionAxis = getMainConnectionAxis();
        int controllerCoordinate = calculateCoords(getController(), connectionAxis);
        int blockCoordinate = calculateCoords(getBlockPos(), connectionAxis);
        tankState = tankState.setValue(CreativeAirtightTankBlock.BOTTOM, controllerCoordinate == blockCoordinate);
        tankState = tankState.setValue(CreativeAirtightTankBlock.TOP, controllerCoordinate + getHeight() - 1 == blockCoordinate);
        level.setBlock(worldPosition, tankState, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
    }

    @Override
    protected void resetTankBeforeControllerRemoval(boolean keepFluids) {
        storageController.resetReservoirLimits();
    }

    @Override
    protected void resetStandaloneBlockState() {
        if (level == null) {
            return;
        }

        BlockState tankState = getBlockState();
        if (!(tankState.getBlock() instanceof CreativeAirtightTankBlock)) {
            return;
        }

        tankState = tankState.setValue(CreativeAirtightTankBlock.TOP, true).setValue(CreativeAirtightTankBlock.BOTTOM, true);
        level.setBlock(worldPosition, tankState, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
    }

    @Override
    protected long volumePerBlock() {
        return getVolumePerBlock();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        serialization.write(compoundTag, provider, clientPacket);
    }

    @Override
    public void writeSafe(CompoundTag compoundTag, Provider provider) {
        serialization.writeSafe(compoundTag);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        serialization.read(compoundTag, provider, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return display.addToGoggleTooltip(tooltip);
    }

    @Override
    public void setTankBlockCount(int tank, int blocks) {
        storageController.resetReservoirLimits();
    }

    @Override
    public void mergeTankStateFrom(GasTankMultiblockPart source) {
        storageController.mergeTankStateFrom(source);
    }

    @Override
    public void clearTankStateAfterMerge(int tank) {
        storageController.clearTankState();
    }

    @Override
    public GasStack prepareTankStateForSplit(int tank, boolean controllerRemoved) {
        return storageController.prepareTankStateForSplit();
    }

    @Override
    public void applySplitTankState(int tank, GasStack state) {
        storageController.applySplitTankState(state);
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

    @Override
    public boolean isCreative(Level level, BlockState blockState, BlockPos blockPos) {
        return true;
    }

    @Override
    public void setExtraData(@Nullable Object data) {
        if (!(data instanceof Number pressure)) {
            return;
        }

        loadLocalFixedPressurePa(pressure.longValue());
    }

    @Override
    @Nullable
    public Object getExtraData() {
        return getFixedPressurePa();
    }

    void updateTankConnectivity() {
        updateConnectivity();
    }

    void setContainedGas(GasStack gasStack) {
        storageController.setContainedGas(gasStack);
    }

    long getFixedPressurePa() {
        CreativeAirtightTankBlockEntity controller = getControllerBE();
        if (controller == null) {
            return localReservoir().getFixedPressurePa();
        }

        return controller.localReservoir().getFixedPressurePa();
    }

    void syncPressureBehaviour() {
        if (pressureBehaviour == null) {
            return;
        }

        pressureBehaviour.syncFromController();
    }

    void setLocalFixedPressurePa(long pressurePa) {
        localReservoir().setFixedPressurePa(CreativeAirtightTankPressureBehaviour.normalizePressurePa(pressurePa));
        syncPressureBehaviour();
    }

    void loadLocalFixedPressurePa(long pressurePa) {
        localReservoir().restoreFixedPressurePa(CreativeAirtightTankPressureBehaviour.normalizePressurePa(pressurePa));
        syncPressureBehaviour();
    }

    void mirrorLocalFixedPressurePa(long pressurePa) {
        localReservoir().restoreFixedPressurePa(pressurePa);
    }

    void updateClientStructureState() {
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
        }
        if (isController()) {
            storageController.resetReservoirLimits();
        }
        invalidateRenderBounds();
    }

    private CreativeGasReservoir localReservoir() {
        return getTankInventory();
    }
}
