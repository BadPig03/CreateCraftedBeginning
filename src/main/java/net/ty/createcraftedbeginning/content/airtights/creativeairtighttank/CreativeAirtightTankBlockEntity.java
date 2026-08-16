package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
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
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.CreativeSmartGasTank;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AbstractAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.IChamberGasTank;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasTankMultiBlockEntityContainer;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeAirtightTankBlockEntity extends AbstractAirtightTankBlockEntity implements IHaveGoggleInformation, IChamberGasTank, ICreativeGasContainer, ThresholdSwitchObservable {
    protected final CreativeAirtightTankStorageController storageController;
    protected final CreativeAirtightTankDisplay display;
    protected final CreativeAirtightTankSerialization serialization;

    public CreativeAirtightTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        storageController = new CreativeAirtightTankStorageController(this);
        display = new CreativeAirtightTankDisplay(this);
        serialization = new CreativeAirtightTankSerialization(this, storageController);
        initializeTank(new CreativeSmartGasTank(getCapacityPerTank(), this::onGasStackChanged));
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.CREATIVE_AIRTIGHT_TANK.get(), (be, context) -> be.getCapability());
    }

    public static long getCapacityPerTank() {
        return Integer.MAX_VALUE * GasAmounts.MILLIBUCKETS_PER_BUCKET;
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
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
    public void removeController(boolean keepFluids) {
        super.removeController(keepFluids);
    }

    @Override
    protected void updateMultiBlockState() {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof CreativeAirtightTankBlock)) {
            return;
        }

        Axis axis = getMainConnectionAxis();
        int controllerCoords = calculateCoords(getController(), axis);
        int posCoords = calculateCoords(getBlockPos(), axis);
        state = state.setValue(CreativeAirtightTankBlock.BOTTOM, controllerCoords == posCoords);
        state = state.setValue(CreativeAirtightTankBlock.TOP, controllerCoords + getHeight() - 1 == posCoords);
        level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
    }

    @Override
    protected void resetTankBeforeControllerRemoval(boolean keepFluids) {
        storageController.resetCapacity();
    }

    @Override
    protected void resetStandaloneBlockState() {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof CreativeAirtightTankBlock)) {
            return;
        }

        state = state.setValue(CreativeAirtightTankBlock.TOP, true).setValue(CreativeAirtightTankBlock.BOTTOM, true);
        level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
    }

    @Override
    protected long capacityPerBlock() {
        return getCapacityPerTank();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return display.addToGoggleTooltip(tooltip);
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        storageController.resetCapacity();
    }

    @Override
    public void mergeTankStateFrom(IGasTankMultiBlockEntityContainer source) {
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

    public void updateClientStructureState() {
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
        }
        if (isController()) {
            storageController.resetCapacity();
        }
        invalidateRenderBounds();
    }
}
