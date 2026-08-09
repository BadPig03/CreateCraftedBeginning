package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.SmartGasTank;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightTankBlockEntity extends AbstractAirtightTankBlockEntity implements IHaveGoggleInformation, IChamberGasTank, ThresholdSwitchObservable {
    private final AirtightAssemblyDriverCore driverCore;
    private final AirtightTankStorageController storageController;
    private final AirtightTankDisplay display;
    private final AirtightTankSerialization serialization;

    public AirtightTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        driverCore = new AirtightAssemblyDriverCore();
        storageController = new AirtightTankStorageController(this);
        display = new AirtightTankDisplay(this);
        serialization = new AirtightTankSerialization(this, storageController);
        initializeTank(new SmartGasTank(getCapacityPerTank(), this::onGasStackChanged));
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_TANK.get(), (be, context) -> be.getCapability());
    }

    public static long getCapacityPerTank() {
        return CCBConfig.server().airtights.maxAirtightTankCapacityPerBlock.get() * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }

    public static int getConfiguredMaxLength() {
        return configuredMaxLength();
    }

    public static int getConfiguredMaxWidth() {
        return configuredMaxWidth();
    }

    public static BlockPos offsetInMulti(BlockPos origin, Axis axis, int lengthOffset, int uOffset, int vOffset) {
        return switch (axis) {
            case X -> origin.offset(lengthOffset, uOffset, vOffset);
            case Y -> origin.offset(uOffset, lengthOffset, vOffset);
            case Z -> origin.offset(uOffset, vOffset, lengthOffset);
        };
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

    public int getTotalTankSize() {
        return getWidth() * getWidth() * getHeight();
    }

    public void updateTankState() {
        if (level != null && !level.isClientSide && isController()) {
            driverCore.requestStructureEvaluation();
        }
    }

    @Override
    protected void tickController() {
        driverCore.tick(this);
    }

    @Override
    protected void updateMultiBlockState() {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof AirtightTankBlock)) {
            return;
        }

        Axis axis = getMainConnectionAxis();
        int controllerCoords = calculateCoords(getController(), axis);
        int posCoords = calculateCoords(getBlockPos(), axis);
        state = state.setValue(AirtightTankBlock.BOTTOM, controllerCoords == posCoords);
        state = state.setValue(AirtightTankBlock.TOP, controllerCoords + getHeight() - 1 == posCoords);
        level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
    }

    @Override
    protected void afterMultiUpdated() {
        updateTankState();
    }

    @Override
    protected void resetTankBeforeControllerRemoval(boolean keepFluids) {
        storageController.resizeToBlocks(1);
    }

    @Override
    protected void afterControllerStateCleared(boolean keepFluids) {
        driverCore.reset();
    }

    @Override
    protected void resetStandaloneBlockState() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.getBlock() instanceof AirtightTankBlock) {
            state = state.setValue(AirtightTankBlock.TOP, true).setValue(AirtightTankBlock.BOTTOM, true);
            level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override
    protected long capacityPerBlock() {
        return getCapacityPerTank();
    }

    public AirtightAssemblyDriverCore getCore() {
        return driverCore;
    }

    @Override
    public void removeController(boolean keepFluids) {
        super.removeController(keepFluids);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return display.addToGoggleTooltip(tooltip);
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        applyGasTankSize(blocks);
    }

    public void applyGasTankSize(int blocks) {
        storageController.resizeToBlocks(blocks);
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
}
