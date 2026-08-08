package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.MultiFace;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.coolantshandlers.CoolantEfficiency;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorUtils.CompressionPlan;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorUtils.WorkState;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.CCBClientBridge;
import net.ty.createcraftedbeginning.recipe.PressurizationRecipe;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirCompressorBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation, ThresholdSwitchObservable, IGasInventoryIdentifierProvider {
    private static final int SYNC_RATE = 4;
    private SmartGasTankBehaviour inputTankBehaviour;
    private SmartGasTankBehaviour outputTankBehaviour;
    private CCBAdvancementBehaviour advancementBehaviour;

    private boolean queuedPressurization;
    private boolean queuedSync;
    private CoolantEfficiency coolantEfficiency = CoolantEfficiency.NONE;
    private int ponderCounter;
    private int storedHeat;
    private int syncCooldown;
    private OverheatState overheatState = OverheatState.NORMAL;
    private WorkState workState = WorkState.EMPTY;

    public AirCompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(AirCompressorUtils.LAZY_TICK_RATE);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIR_COMPRESSOR.get(), (compressor, side) -> {
            Direction inputSide = AirCompressorBlock.getInputSide(compressor.getBlockState());
            if (side == inputSide) {
                return compressor.inputTankBehaviour.getCapability();
            }

            if (side == inputSide.getOpposite()) {
                return compressor.outputTankBehaviour.getCapability();
            }
            return null;
        });
    }

    private static void addTankTooltip(List<Component> tooltip, String titleKey, GasStack gas, long maxCapacity) {
        CCBLang.translate(titleKey).style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (gas.isEmpty()) {
            CCBLang.gasName(GasStack.EMPTY).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            GasAmountUtils.precise(maxCapacity).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
            return;
        }

        CCBLang.gasName(gas).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        GasAmountUtils.precise(gas.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmountUtils.precise(maxCapacity).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }

    private boolean isInputGasInvalid() {
        if (level == null) {
            return false;
        }

        GasStack input = inputTankBehaviour.getPrimaryHandler().getGasStack();
        return !input.isEmpty() && PressurizationRecipe.findRecipe(level, input).isEmpty();
    }

    public void increaseHeat() {
        if (overheatState == OverheatState.MELTDOWN) {
            return;
        }

        int previousStoredHeat = storedHeat;
        setStoredHeat(AirCompressorUtils.getNextStateHeat(overheatState));
        markDirty(previousStoredHeat, true);
    }

    public void setCoolantEfficiency(CoolantEfficiency newEfficiency) {
        if (coolantEfficiency == newEfficiency) {
            return;
        }

        coolantEfficiency = newEfficiency;
        if (level == null || level.isClientSide) {
            return;
        }

        setChanged();
    }

    public int getStoredHeat() {
        return storedHeat;
    }

    private void setStoredHeat(int heat) {
        storedHeat = AirCompressorUtils.clampStoredHeat(heat);
        setOverheatState(AirCompressorUtils.getOverheatState(storedHeat));
    }

    public int getAnalogOutputSignal() {
        return overheatState.getAnalogOutputSignal();
    }

    private void setOverheatState(OverheatState newState) {
        if (overheatState == newState) {
            return;
        }

        overheatState = newState;
        notifyUpdate();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        queuedPressurization = true;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    public void loadFromItem(ItemStack stack) {
        setStoredHeat(AirCompressorUtils.readStoredHeat(stack));
    }

    public void saveToItem(ItemStack stack) {
        AirCompressorUtils.saveToItem(stack, overheatState, storedHeat);
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
        AirCompressorUtils.writeData(tag, overheatState, storedHeat, coolantEfficiency, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.read(tag, provider, clientPacket);
        OverheatState savedState = AirCompressorUtils.readOverheatState(tag);
        overheatState = savedState;
        if (clientPacket) {
            return;
        }

        storedHeat = AirCompressorUtils.readStoredHeat(tag, savedState);
        overheatState = AirCompressorUtils.getOverheatState(storedHeat);
        coolantEfficiency = AirCompressorUtils.readCoolantEfficiency(tag);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.FEELING_THE_PRESSURE, CCBAdvancements.A_CLOSE_CALL);
        behaviours.add(advancementBehaviour);

        long maxCapacity = AirCompressorUtils.getMaxCapacity();
        inputTankBehaviour = new SmartGasTankBehaviour(SmartGasTankBehaviour.INPUT, this, 1, maxCapacity, false);
        outputTankBehaviour = new SmartGasTankBehaviour(SmartGasTankBehaviour.OUTPUT, this, 1, maxCapacity, false).forbidInsertion();
        behaviours.add(inputTankBehaviour);
        behaviours.add(outputTankBehaviour);
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = false;
        if (isInputGasInvalid()) {
            CCBLang.translate("gui.invalid_ingredient").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.air_compressor.invalid_gas", inputTankBehaviour.getPrimaryHandler().getGasStack().getHoverName());
            added = true;
        }
        if (overStressed && CCBClientBridge.isOverstressedTooltipEnabled()) {
            if (added) {
                tooltip.add(CommonComponents.EMPTY);
            }
            CCBLang.translate("gui.overstressed").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.network_overstressed");
            return true;
        }

        if (isSpeedRequirementFulfilled() || getSpeed() == 0) {
            return added;
        }

        if (added) {
            tooltip.add(CommonComponents.EMPTY);
        }
        CCBLang.translate("gui.speed_requirement").style(ChatFormatting.GOLD).forGoggles(tooltip);
        CCBLang.addToGoggles(tooltip, "gui.not_fast_enough", Component.translatable(CCBBlocks.AIR_COMPRESSOR_BLOCK.get().getDescriptionId()));
        return true;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.air_compressor").forGoggles(tooltip);
        CCBLang.translate("gui.air_compressor.overheat_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(overheatState.getTranslationKey()).style(overheatState.getDisplayColor()).forGoggles(tooltip, 1);
        if (isPlayerSneaking) {
            addTankDetails(tooltip);
        }
        if (!StressImpact.isEnabled()) {
            return true;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.stress_impact").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(calculateStressApplied() * Mth.abs(getTheoreticalSpeed())).translate("gui.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        return true;
    }

    private void tickClient(Level level) {
        int threshold = AirCompressorUtils.getNextOverheatThreshold();
        if (level instanceof PonderLevel ponderLevel) {
            ponderCounter = (ponderCounter + 1) % threshold;
            overheatState.spawnParticlesInPonderLevel(ponderLevel, worldPosition, ponderCounter);
        }
        overheatState.tick(this);
    }

    private void tickServer(Level level) {
        boolean shouldPressurize = queuedPressurization;
        queuedPressurization = false;
        if (overheatState == OverheatState.MELTDOWN) {
            AirCompressorUtils.updateOperatingBlockState(level, worldPosition, getBlockState(), false);
            overheatState.tick(this);
            return;
        }

        CompressionPlan plan = AirCompressorUtils.createCompressionPlan(level, inputTankBehaviour.getPrimaryHandler().getGasStack());
        boolean operating = plan != null && AirCompressorUtils.canOperate(plan, overStressed, getSpeed(), overheatState, inputTankBehaviour, outputTankBehaviour);
        if (shouldPressurize && operating) {
            workState = AirCompressorUtils.pressurize(workState, plan, inputTankBehaviour, outputTankBehaviour);
            operating = AirCompressorUtils.canOperate(plan, overStressed, getSpeed(), overheatState, inputTankBehaviour, outputTankBehaviour);
        }

        AirCompressorUtils.updateOperatingBlockState(level, worldPosition, getBlockState(), operating);
        if (operating) {
            workState = AirCompressorUtils.accumulateWork(workState, plan, getSpeed(), overheatState);
        }

        OverheatState previousState = overheatState;
        int previousStoredHeat = storedHeat;
        storedHeat = AirCompressorUtils.updateStoredHeat(storedHeat, getSpeed(), operating, coolantEfficiency, level);
        OverheatState newState = AirCompressorUtils.getOverheatState(storedHeat);
        if (previousState == OverheatState.SEVERE && newState.ordinal() < OverheatState.SEVERE.ordinal()) {
            advancementBehaviour.awardPlayer(CCBAdvancements.A_CLOSE_CALL);
        }
        setOverheatState(newState);
        if (newState == OverheatState.MELTDOWN) {
            AirCompressorUtils.updateOperatingBlockState(level, worldPosition, getBlockState(), false);
        }
        markDirty(previousStoredHeat, previousState != newState);
    }

    private void addTankDetails(List<Component> tooltip) {
        tooltip.add(CommonComponents.EMPTY);
        GasStack inputGas = inputTankBehaviour.getPrimaryHandler().getGasStack();
        GasStack outputGas = outputTankBehaviour.getPrimaryHandler().getGasStack();
        long maxCapacity = AirCompressorUtils.getMaxCapacity();
        addTankTooltip(tooltip, "gui.air_compressor.input_capacity", inputGas, maxCapacity);

        tooltip.add(CommonComponents.EMPTY);
        addTankTooltip(tooltip, "gui.air_compressor.output_capacity", outputGas, maxCapacity);
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

        if (!force && previousStoredHeat == storedHeat) {
            return;
        }

        setChanged();
    }

    public void updateCoolant(BlockPos coolantPos) {
        if (level == null) {
            return;
        }

        setCoolantEfficiency(AirCompressorUtils.getCoolantEfficiency(level, coolantPos));
    }

    @Override
    public int getMaxValue() {
        long totalCapacity = inputTankBehaviour.getPrimaryHandler().getCapacity() + outputTankBehaviour.getPrimaryHandler().getCapacity();
        return GasAmountUtils.toWholeBucketsClamped(totalCapacity);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        long totalAmount = inputTankBehaviour.getPrimaryHandler().getGasAmount() + outputTankBehaviour.getPrimaryHandler().getGasAmount();
        return GasAmountUtils.toWholeBucketsClamped(totalAmount);
    }

    @Override
    public MutableComponent format(int value) {
        return GasAmountUtils.formatWholeBuckets(value);
    }
}