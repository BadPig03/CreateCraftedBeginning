package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.MultiFace;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.coolantshandlers.CoolantEfficiency;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorController.ServerTickResult;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirCompressorBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation, ThresholdSwitchObservable, IGasInventoryIdentifierProvider {
    private static final int SYNC_RATE = 4;

    private final AirCompressorController controller;
    private final AirCompressorState state = new AirCompressorState();

    private SmartGasTankBehaviour inputTankBehaviour;
    private SmartGasTankBehaviour outputTankBehaviour;
    private CCBAdvancementBehaviour advancementBehaviour;

    private boolean queuedSync;
    private int ponderCounter;
    private int syncCooldown;

    public AirCompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        controller = new AirCompressorController(this::updateOperatingBlockState);
        setLazyTickRate(AirCompressorProcessing.LAZY_TICK_RATE);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIR_COMPRESSOR.get(), (compressor, queriedSide) -> {
            Direction inputSide = AirCompressorBlock.getInputSide(compressor.getBlockState());
            if (queriedSide == inputSide) {
                return compressor.inputTankBehaviour.getCapability();
            }

            if (queriedSide == inputSide.getOpposite()) {
                return compressor.outputTankBehaviour.getCapability();
            }
            return null;
        });
    }

    public void increaseHeat() {
        if (state.getOverheatState() == OverheatState.MELTDOWN) {
            return;
        }

        int previousStoredHeat = state.getStoredHeat();
        OverheatState previousOverheatState = state.getOverheatState();
        state.setStoredHeat(AirCompressorThermal.getNextStateHeat(previousOverheatState));
        notifyIfOverheatStateChanged(previousOverheatState);
        markDirty(previousStoredHeat, true);
    }

    public void setCoolantEfficiency(CoolantEfficiency newCoolantEfficiency) {
        if (!state.setCoolantEfficiency(newCoolantEfficiency)) {
            return;
        }

        if (level == null || level.isClientSide) {
            return;
        }

        setChanged();
    }

    public int getStoredHeat() {
        return state.getStoredHeat();
    }

    public int getAnalogOutputSignal() {
        return state.getOverheatState().getAnalogOutputSignal();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        controller.queuePressurization();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    public void loadFromItem(ItemStack stack) {
        OverheatState previousOverheatState = state.getOverheatState();
        state.loadFromItem(stack);
        notifyIfOverheatStateChanged(previousOverheatState);
    }

    public void saveToItem(ItemStack stack) {
        state.saveToItem(stack);
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

    @Override
    public void tick() {
        super.tick();
        tickSyncCooldown();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            tickClient(level);
            return;
        }

        tickServer(level);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level == null || level.isClientSide && !isVirtual()) {
            return;
        }

        if (Mth.abs(speed) < SpeedLevel.MEDIUM.getSpeedValue()) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.FEELING_THE_PRESSURE);
    }

    @Override
    protected void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.write(tag, provider, clientPacket);
        state.write(tag, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.read(tag, provider, clientPacket);
        state.read(tag, clientPacket);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.FEELING_THE_PRESSURE, CCBAdvancements.A_CLOSE_CALL);
        behaviours.add(advancementBehaviour);

        long tankCapacity = AirCompressorProcessing.getTankCapacity();
        inputTankBehaviour = new SmartGasTankBehaviour(SmartGasTankBehaviour.INPUT, this, 1, tankCapacity, false);
        outputTankBehaviour = new SmartGasTankBehaviour(SmartGasTankBehaviour.OUTPUT, this, 1, tankCapacity, false).forbidInsertion();
        behaviours.add(inputTankBehaviour);
        behaviours.add(outputTankBehaviour);
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return AirCompressorTooltip.addHoveringInformation(tooltip, level, inputTankBehaviour.getPrimaryHandler().getGasStack(), overStressed, isSpeedRequirementFulfilled(), getSpeed());
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        GasStack inputGas = inputTankBehaviour.getPrimaryHandler().getGasStack();
        GasStack outputGas = outputTankBehaviour.getPrimaryHandler().getGasStack();
        AirCompressorTooltip.addGoggleInformation(tooltip, isPlayerSneaking, state.getOverheatState(), inputGas, outputGas, calculateStressApplied(), getTheoreticalSpeed());
        return true;
    }

    private void tickClient(Level level) {
        int overheatThreshold = AirCompressorThermal.getNextOverheatThreshold();
        OverheatState overheatState = state.getOverheatState();
        if (level instanceof PonderLevel ponderLevel) {
            ponderCounter = (ponderCounter + 1) % overheatThreshold;
            overheatState.spawnParticlesInPonderLevel(ponderLevel, worldPosition, ponderCounter);
        }
        overheatState.tick(this);
    }

    private void tickServer(Level level) {
        ServerTickResult tickResult = controller.tickServer(level, state, overStressed, getSpeed(), inputTankBehaviour, outputTankBehaviour);
        if (tickResult.initiallyMeltdown()) {
            state.getOverheatState().tick(this);
            return;
        }

        if (tickResult.closeCall()) {
            advancementBehaviour.awardPlayer(CCBAdvancements.A_CLOSE_CALL);
        }
        boolean overheatStateChanged = tickResult.overheatStateChanged(state);
        if (overheatStateChanged) {
            notifyUpdate();
        }
        if (tickResult.enteredMeltdown()) {
            updateOperatingBlockState(level, false);
        }
        markDirty(tickResult.previousStoredHeat(), overheatStateChanged);
    }

    private void updateOperatingBlockState(Level level, boolean operating) {
        BlockState blockState = getBlockState();
        if (level.isClientSide || !blockState.hasProperty(AirCompressorBlock.ACTIVE) || blockState.getValue(AirCompressorBlock.ACTIVE) == operating) {
            return;
        }

        level.setBlock(worldPosition, blockState.setValue(AirCompressorBlock.ACTIVE, operating), Block.UPDATE_CLIENTS);
    }

    @Override
    public @Nullable InventoryIdentifier getGasInventoryIdentifier(Direction queriedSide) {
        Direction inputSide = AirCompressorBlock.getInputSide(getBlockState());
        if (queriedSide == inputSide) {
            return new MultiFace(worldPosition, Set.of(inputSide));
        }

        Direction outputSide = inputSide.getOpposite();
        if (queriedSide == outputSide) {
            return new MultiFace(worldPosition, Set.of(outputSide));
        }
        return null;
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

    private void markDirty(int previousStoredHeat, boolean force) {
        if (level == null || level.isClientSide) {
            return;
        }

        if (!force && previousStoredHeat == state.getStoredHeat()) {
            return;
        }

        setChanged();
    }

    private void notifyIfOverheatStateChanged(OverheatState previousOverheatState) {
        if (previousOverheatState == state.getOverheatState()) {
            return;
        }

        notifyUpdate();
    }

    public void updateCoolant(BlockPos coolantPos) {
        if (level == null) {
            return;
        }

        setCoolantEfficiency(AirCompressorThermal.getCoolantEfficiency(level, coolantPos));
    }

    @Override
    public int getMaxValue() {
        long totalTankCapacity = inputTankBehaviour.getPrimaryHandler().getCapacity() + outputTankBehaviour.getPrimaryHandler().getCapacity();
        return GasAmountUtils.toWholeBucketsClamped(totalTankCapacity);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        long totalGasAmount = inputTankBehaviour.getPrimaryHandler().getGasAmount() + outputTankBehaviour.getPrimaryHandler().getGasAmount();
        return GasAmountUtils.toWholeBucketsClamped(totalGasAmount);
    }

    @Override
    public MutableComponent format(int value) {
        return GasAmountUtils.formatWholeBuckets(value);
    }
}
