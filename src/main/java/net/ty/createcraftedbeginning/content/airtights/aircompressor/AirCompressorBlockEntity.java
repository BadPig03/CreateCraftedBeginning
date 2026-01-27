package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import com.mojang.serialization.Codec;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates.IOverheatState;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates.MeltdownOverheatState;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates.OverheatManager;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirCompressorBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation {
    private static final int SYNC_RATE = 4;
    private static final int LAZY_TICK_RATE = 5;
    private static final int PRESSURIZATION_RATIO = 10;
    private static final int SLOW_SPEED_HEAT = 1;
    private static final int MEDIUM_SPEED_HEAT = 3;
    private static final int FAST_SPEED_HEAT = 5;

    private static final String COMPOUND_KEY_OVERHEAT_TIME = "OverheatTime";
    private static final String COMPOUND_KEY_COOLANT_EFFICIENCY = "CoolantEfficiency";
    private static final String COMPOUND_KEY_OVERHEAT_STATE = "OverheatState";

    private SmartGasTankBehaviour inputTankBehaviour;
    private SmartGasTankBehaviour outputTankBehaviour;
    private CCBAdvancementBehaviour advancementBehaviour;

    private int overheatTime;
    private CoolantEfficiency coolantEfficiency = CoolantEfficiency.NONE;
    private IOverheatState overheatState = OverheatManager.NORMAL;
    private int ponderLevelCounter;
    private int syncCooldown;
    private boolean queuedSync;

    public AirCompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        overheatTime = getNextOverheatThreshold() / 2;
        ponderLevelCounter = 0;
        setLazyTickRate(LAZY_TICK_RATE);
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIR_COMPRESSOR.get(), (be, context) -> {
            Direction inputDir = AirCompressorBlock.getInputSide(be.getBlockState());
            if (context == inputDir) {
                return be.inputTankBehaviour.getCapability();
            }
            else if (context == inputDir.getOpposite()) {
                return be.outputTankBehaviour.getCapability();
            }
            return null;
        });
    }

    private static int getNextOverheatThreshold() {
        return CCBConfig.server().airtights.nextOverheatThreshold.get();
    }

    private static long getMaxCapacity() {
        return CCBConfig.server().airtights.maxCanisterCapacity.get() * 500L;
    }

    private static int getPressurizationRateMultiplier() {
        return CCBConfig.server().airtights.pressurizationRateMultiplier.get();
    }

    private @NotNull Gas getPressurizedGasType() {
        return inputTankBehaviour.getPrimaryHandler().getGasStack().getGasType().getPressurizedGasType();
    }

    private boolean isInactive() {
        return isNotFastEnough() || isInputNotEnough() || isOutputFull() || isInputGasInvalid() || isOutputMismatched();
    }

    private boolean isInputGasInvalid() {
        return !inputTankBehaviour.getPrimaryHandler().isEmpty() && getPressurizedGasType().isEmpty();
    }

    private boolean isInputNotEnough() {
        float absSpeed = Mth.abs(getSpeed());
        long requiredAmount = (long) (absSpeed * getPressurizationRateMultiplier() / LAZY_TICK_RATE);
        return absSpeed != 0 && inputTankBehaviour.getPrimaryHandler().getGasAmount() < requiredAmount;
    }

    private boolean isNotFastEnough() {
        return Mth.abs(getTheoreticalSpeed()) < SpeedLevel.MEDIUM.getSpeedValue();
    }

    private boolean isOutputFull() {
        return outputTankBehaviour.getPrimaryHandler().getSpace() == 0;
    }

    private boolean isOutputMismatched() {
        Gas pressurizedGasType = getPressurizedGasType();
        if (pressurizedGasType.isEmpty()) {
            return false;
        }

        GasStack outputGas = outputTankBehaviour.getPrimaryHandler().getGasStack();
        return !outputGas.isEmpty() && !outputGas.is(pressurizedGasType);
    }

    private long getMaxAmount() {
        if (isInactive()) {
            return 0;
        }

        float efficiency = overheatState.getEfficiency();
        long rawAmount = (long) (Mth.abs(getSpeed()) * getPressurizationRateMultiplier() * efficiency);
        return rawAmount / PRESSURIZATION_RATIO * PRESSURIZATION_RATIO;
    }

    private void doPressurization() {
        if (isNotFastEnough() || isInputGasInvalid()) {
            return;
        }

        Gas pressurizedGasType = getPressurizedGasType();
        if (pressurizedGasType.isEmpty()) {
            return;
        }
        if (isOutputFull() || isOutputMismatched() || inputTankBehaviour.getPrimaryHandler().isEmpty()) {
            return;
        }

        long drainAmount = Math.min(inputTankBehaviour.getPrimaryHandler().getGasAmount(), getMaxAmount()) / PRESSURIZATION_RATIO * PRESSURIZATION_RATIO;
        if (drainAmount < PRESSURIZATION_RATIO) {
            return;
        }

        long fillAmount = inputTankBehaviour.getInternalGasHandler().forceDrain(drainAmount, GasAction.EXECUTE).getAmount() / PRESSURIZATION_RATIO;
        GasStack fillGasStack = new GasStack(pressurizedGasType, fillAmount);
        outputTankBehaviour.getInternalGasHandler().forceFill(fillGasStack, GasAction.EXECUTE);
    }

    public CoolantEfficiency getCoolantEfficiency() {
        return coolantEfficiency;
    }

    public void setCoolantEfficiency(CoolantEfficiency newEfficiency) {
        if (coolantEfficiency == newEfficiency) {
            return;
        }

        coolantEfficiency = newEfficiency;
        notifyUpdate();
    }

    public void loadFromItem(@NotNull ItemStack stack) {
        String stateName = stack.getOrDefault(CCBDataComponents.AIR_COMPRESSOR_OVERHEAT_STATE, OverheatManager.NORMAL.getSerializedName());
        setOverheatState(OverheatManager.getStateByName(stateName));
    }

    public void saveToItem(@NotNull ItemStack stack) {
        stack.set(CCBDataComponents.AIR_COMPRESSOR_OVERHEAT_STATE, overheatState.getSerializedName());
    }

    public int getAnalogOutputSignal() {
        return overheatState.getAnalogOutputSignal();
    }

    public int getHeatAdded() {
        if (isInactive()) {
            return 0;
        }

        float absSpeed = Mth.abs(getSpeed());
        if (absSpeed >= SpeedLevel.FAST.getSpeedValue()) {
            return FAST_SPEED_HEAT;
        }
        else if (absSpeed >= SpeedLevel.MEDIUM.getSpeedValue()) {
            return MEDIUM_SPEED_HEAT;
        }
        else {
            return SLOW_SPEED_HEAT;
        }
    }

    public void decreaseHeat() {
        if (overheatState.getNextState() instanceof MeltdownOverheatState) {
            advancementBehaviour.awardPlayer(CCBAdvancements.SO_CLOSE);
        }
        setOverheatState(overheatState.getPreviousState());
    }

    public void increaseHeat() {
        setOverheatState(overheatState.getNextState());
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        doPressurization();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
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

    public IOverheatState getOverheatState() {
        return overheatState;
    }

    public void setOverheatState(IOverheatState newState) {
        if (overheatState == newState) {
            return;
        }

        overheatState = newState;
        notifyUpdate();
    }

    @Override
    public void tick() {
        super.tick();
        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync) {
                sendData();
            }
        }

        int nextOverheatThreshold = getNextOverheatThreshold();
        if (level != null && level.isClientSide && level instanceof PonderLevel ponderLevel) {
            ponderLevelCounter = (ponderLevelCounter + 1) % nextOverheatThreshold;
            overheatState.spawnParticlesInPonderLevel(ponderLevel, worldPosition, ponderLevelCounter);
        }

        overheatState.tick(this);
        int netHeat = overheatState.tryAddHeat(this);
        overheatTime += netHeat;
        if (netHeat > 0 && overheatTime > nextOverheatThreshold) {
            increaseHeat();
            overheatTime = nextOverheatThreshold / 2;
        }
        else if (netHeat < 0 && overheatTime < 0) {
            decreaseHeat();
            overheatTime = nextOverheatThreshold / 2;
        }
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

        advancementBehaviour.awardPlayer(CCBAdvancements.UNDER_IMMENSE_PRESSURE);
    }

    @Override
    protected void write(@NotNull CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.putString(COMPOUND_KEY_OVERHEAT_STATE, overheatState.getSerializedName());
        if (clientPacket) {
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_OVERHEAT_TIME, overheatTime);
        compoundTag.putInt(COMPOUND_KEY_COOLANT_EFFICIENCY, coolantEfficiency.ordinal());
    }

    @Override
    protected void read(@NotNull CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (compoundTag.contains(COMPOUND_KEY_OVERHEAT_STATE)) {
            overheatState = OverheatManager.getStateByName(compoundTag.getString(COMPOUND_KEY_OVERHEAT_STATE));
        }
        if (clientPacket) {
            return;
        }

        if (compoundTag.contains(COMPOUND_KEY_OVERHEAT_TIME)) {
            overheatTime = compoundTag.getInt(COMPOUND_KEY_OVERHEAT_TIME);
        }
        if (compoundTag.contains(COMPOUND_KEY_COOLANT_EFFICIENCY)) {
            coolantEfficiency = CoolantEfficiency.values()[compoundTag.getInt(COMPOUND_KEY_COOLANT_EFFICIENCY)];
        }
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        long maxCapacity = getMaxCapacity();
        inputTankBehaviour = new SmartGasTankBehaviour(SmartGasTankBehaviour.INPUT, this, 1, maxCapacity, false);
        outputTankBehaviour = new SmartGasTankBehaviour(SmartGasTankBehaviour.OUTPUT, this, 1, maxCapacity, false).forbidInsertion();
        behaviours.add(inputTankBehaviour);
        behaviours.add(outputTankBehaviour);

        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.UNDER_IMMENSE_PRESSURE, CCBAdvancements.SO_CLOSE);
        behaviours.add(advancementBehaviour);
        super.addBehaviours(behaviours);
    }

    @Override
    public boolean addToTooltip(@NotNull List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = false;
        if (isInputGasInvalid()) {
            CCBLang.translate("gui.goggles.invalid_ingredient").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.goggles.air_compressor.invalid_gas", inputTankBehaviour.getPrimaryHandler().getGasStack().getHoverName());
            added = true;
        }
        if (overStressed && AllConfigs.client().enableOverstressedTooltip.get()) {
            if (added) {
                tooltip.add(CommonComponents.EMPTY);
            }
            CCBLang.translate("gui.goggles.overstressed").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.goggles.network_overstressed");
            return true;
        }
        if (!isSpeedRequirementFulfilled() && getSpeed() != 0) {
            if (added) {
                tooltip.add(CommonComponents.EMPTY);
            }
            CCBLang.translate("gui.goggles.speed_requirement").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.goggles.not_fast_enough", I18n.get(CCBBlocks.AIR_COMPRESSOR_BLOCK.getDefaultState().getBlock().getDescriptionId()));
            added = true;
        }
        return added;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.goggles.air_compressor").forGoggles(tooltip);
        CCBLang.translate("gui.goggles.air_compressor.overheat_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(overheatState.getTranslationKey()).style(overheatState.getDisplayColor()).forGoggles(tooltip, 1);

        if (isPlayerSneaking) {
            tooltip.add(CommonComponents.EMPTY);
            GasStack inputGasStack = inputTankBehaviour.getPrimaryHandler().getGasStack();
            GasStack outputGasStack = outputTankBehaviour.getPrimaryHandler().getGasStack();
            long maxCapacity = getMaxCapacity();
            LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
            CCBLang.translate("gui.goggles.air_compressor.input_capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
            if (inputGasStack.isEmpty()) {
                CCBLang.gasName(GasStack.EMPTY).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
                CCBLang.number(maxCapacity).add(mb).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.gasName(inputGasStack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
                CCBLang.number(inputGasStack.getAmount()).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(maxCapacity).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
            }

            tooltip.add(CommonComponents.EMPTY);
            CCBLang.translate("gui.goggles.air_compressor.output_capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
            if (outputGasStack.isEmpty()) {
                CCBLang.gasName(GasStack.EMPTY).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
                CCBLang.number(maxCapacity).add(mb).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.gasName(outputGasStack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
                CCBLang.number(outputGasStack.getAmount()).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(maxCapacity).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
            }
        }
        if (!StressImpact.isEnabled()) {
            return true;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.goggles.stress_impact").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(calculateStressApplied() * Mth.abs(getTheoreticalSpeed())).translate("gui.goggles.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        return true;
    }

    public enum CoolantEfficiency implements StringRepresentable {
        NONE,
        BASIC,
        ADVANCED,
        EXTREME;

        public static final Codec<CoolantEfficiency> CODEC = StringRepresentable.fromEnum(CoolantEfficiency::values);

        public int getHeatReduced(@NotNull Level level) {
            int passive = level.dimensionType().ultraWarm() ? 0 : 1;
            return Math.max(passive, ordinal() * 2);
        }

        @Override
        public @NotNull String getSerializedName() {
            return Lang.asId(name());
        }
    }
}