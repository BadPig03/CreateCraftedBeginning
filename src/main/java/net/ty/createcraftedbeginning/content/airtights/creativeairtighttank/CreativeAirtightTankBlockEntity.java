package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.CreativeSmartGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AbstractAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.IChamberGasTank;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasTankMultiBlockEntityContainer;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeAirtightTankBlockEntity extends AbstractAirtightTankBlockEntity implements IHaveGoggleInformation, IChamberGasTank, ICreativeGasContainer, ThresholdSwitchObservable {

    public CreativeAirtightTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        initializeTank(new CreativeSmartGasTank(getCapacityPerTank(), this::onGasStackChanged));
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.CREATIVE_AIRTIGHT_TANK.get(), (be, context) -> {
            if (be.gasCapability == null) {
                be.refreshCapability();
            }
            return be.gasCapability;
        });
    }

    public static long getCapacityPerTank() {
        return Integer.MAX_VALUE * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (isController()) {
            compoundTag.put(COMPOUND_KEY_TANK_CONTENT, tankInventory.write(provider, new CompoundTag()));
            compoundTag.putInt(COMPOUND_KEY_WIDTH, width);
            compoundTag.putInt(COMPOUND_KEY_HEIGHT, height);
        }
        else {
            compoundTag.put(COMPOUND_KEY_CONTROLLER_POS, NbtUtils.writeBlockPos(controllerPos));
        }
        if (clientPacket) {
            return;
        }

        compoundTag.putBoolean(COMPOUND_KEY_UPDATE_CONNECTIVITY, updateConnectivity);
        if (lastKnownPos == null) {
            return;
        }

        compoundTag.put(COMPOUND_KEY_LAST_KNOWN_POS, NbtUtils.writeBlockPos(lastKnownPos));
    }

    @Override
    public void writeSafe(CompoundTag compoundTag, Provider provider) {
        if (!isController()) {
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_WIDTH, width);
        compoundTag.putInt(COMPOUND_KEY_HEIGHT, height);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        BlockPos previousController = controllerPos;
        int previousWidth = width;
        int previousHeight = height;

        if (!clientPacket) {
            readServerData(compoundTag);
        }

        controllerPos = readOptionalBlockPos(compoundTag, COMPOUND_KEY_CONTROLLER_POS);
        if (isController()) {
            readControllerData(compoundTag, provider);
        }

        updateCapability = true;
        if (!clientPacket) {
            return;
        }

        boolean controllerChanged = !Objects.equals(previousController, controllerPos);
        if (!controllerChanged && previousWidth == width && previousHeight == height) {
            return;
        }

        updateClientState();
    }

    private void readServerData(CompoundTag tag) {
        updateConnectivity = tag.getBoolean(COMPOUND_KEY_UPDATE_CONNECTIVITY);
        lastKnownPos = readOptionalBlockPos(tag, COMPOUND_KEY_LAST_KNOWN_POS);
    }

    private void readControllerData(CompoundTag tag, Provider provider) {
        width = readDimension(tag, COMPOUND_KEY_WIDTH, configuredMaxWidth());
        height = readDimension(tag, COMPOUND_KEY_HEIGHT, configuredMaxLength());

        tankInventory.setCapacity(getCapacityPerTank());
        if (!tag.contains(COMPOUND_KEY_TANK_CONTENT)) {
            return;
        }

        tankInventory.read(provider, tag.getCompound(COMPOUND_KEY_TANK_CONTENT));
    }

    private void updateClientState() {
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
        }
        if (isController()) {
            tankInventory.setCapacity(getCapacityPerTank());
        }
        invalidateRenderBoundingBox();
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
        state = state.setValue(CreativeAirtightTankBlock.TOP, controllerCoords + height - 1 == posCoords);
        level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
    }

    @Override
    protected long capacityPerBlock() {
        return getCapacityPerTank();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }

        CreativeAirtightTankBlockEntity controller = getControllerBE();
        if (controller == null) {
            return false;
        }

        IGasHandler handler = controller.gasCapability;
        if (handler == null) {
            return false;
        }

        CCBLang.translate("gui.gas_container").forGoggles(tooltip);
        GasStack gas = handler.getGasInTank(0);
        if (gas.isEmpty()) {
            CCBLang.translate("gui.gas_container.empty").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }

        CCBLang.gasName(gas).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        CCBLang.translate("gui.gas_container.infinity").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        tankInventory.setCapacity(getCapacityPerTank());
    }

    @Override
    public void mergeTankStateFrom(IGasTankMultiBlockEntityContainer source) {
        if (!source.hasTank()) {
            return;
        }

        GasStack sourceGas = source.getGas(0);
        CreativeSmartGasTank tank = (CreativeSmartGasTank) tankInventory;
        if (tank.getGasStack().isEmpty() && !sourceGas.isEmpty()) {
            tank.setContainedGas(sourceGas);
        }
        source.clearTankStateAfterMerge(0);
    }

    @Override
    public void clearTankStateAfterMerge(int tank) {
        ((CreativeSmartGasTank) tankInventory).setContainedGas(GasStack.EMPTY);
    }

    @Override
    public GasStack prepareTankStateForSplit(int tank, boolean controllerRemoved) {
        setTankSize(tank, 1);
        return getGas(tank);
    }

    @Override
    public void applySplitTankState(int tank, GasStack state) {
        ((CreativeSmartGasTank) tankInventory).setContainedGas(state);
    }

    @Override
    public int getMaxValue() {
        CreativeAirtightTankBlockEntity controller = getControllerBE();
        if (controller == null) {
            return 0;
        }
        return GasAmountUtils.toWholeBucketsClamped(getCapacityPerTank());
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        CreativeAirtightTankBlockEntity controller = getControllerBE();
        if (controller == null) {
            return 0;
        }

        IGasHandler handler = controller.gasCapability;
        GasStack gas = handler.getGasInTank(0);
        if (gas.isEmpty()) {
            return 0;
        }
        return GasAmountUtils.toWholeBucketsClamped(getCapacityPerTank());
    }

    @Override
    public MutableComponent format(int value) {
        return GasAmountUtils.formatWholeBuckets(value);
    }

    @Override
    public void removeController(boolean keepFluids) {
        if (level == null || level.isClientSide) {
            return;
        }

        updateConnectivity = true;
        tankInventory.setCapacity(getCapacityPerTank());
        controllerPos = null;
        width = 1;
        height = 1;
        BlockState state = getBlockState();
        if (state.getBlock() instanceof CreativeAirtightTankBlock) {
            state = state.setValue(CreativeAirtightTankBlock.TOP, true).setValue(CreativeAirtightTankBlock.BOTTOM, true);
            level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
        }
        refreshCapability();
        notifyUpdate();
    }

    @Override
    public boolean isCreative(Level level, BlockState blockState, BlockPos blockPos) {
        return true;
    }
}