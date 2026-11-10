package net.ty.createcraftedbeginning.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.visual.GasPipeReadout;
import net.ty.createcraftedbeginning.gas.visual.GasPipeReadout.Flow;
import net.ty.createcraftedbeginning.gas.visual.GasPipeReadout.Pressure;
import net.ty.createcraftedbeginning.gas.visual.GasPipeReadout.PumpConfiguration;
import net.ty.createcraftedbeginning.gas.visual.GasPipeReadout.RegulatorConfiguration;
import net.ty.createcraftedbeginning.gas.visual.GasPipeReadout.ValveState;
import net.ty.createcraftedbeginning.gas.visual.GasPipeReadoutSource;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum GasPipeTelemetryTooltipProvider implements IServerDataProvider<BlockAccessor>, IComponentProvider<BlockAccessor> {
    INSTANCE;

    private static final long NO_PRESSURE_READING = -1;
    private static final String COMPOUND_KEY_DATA = "JadeGasPipeTelemetry";
    private static final String COMPOUND_KEY_MIN_PRESSURE = "MinPressure";
    private static final String COMPOUND_KEY_MAX_PRESSURE = "MaxPressure";
    private static final String COMPOUND_KEY_FLOW_RATE = "FlowRate";
    private static final String COMPOUND_KEY_MAX_PRESSURE_BOOST = "MaxPressureBoost";
    private static final String COMPOUND_KEY_MAX_PRESSURE_RISE = "MaxPressureRise";
    private static final String COMPOUND_KEY_OUTLET_SET_PRESSURE = "OutletSetPressure";
    private static final String COMPOUND_KEY_VALVE_OPEN = "ValveOpen";

    static @Nullable CompoundTag createServerData(@Nullable BlockEntity blockEntity) {
        GasPipeReadout readout = GasPipeReadoutSource.readout(blockEntity);
        if (readout == null) {
            return null;
        }

        CompoundTag telemetryData = new CompoundTag();
        switch (readout) {
            case Flow flow -> telemetryData.putLong(COMPOUND_KEY_FLOW_RATE, flow.flowRate());
            case Pressure pressure -> {
                if (!pressure.hasReading()) {
                    return telemetryData;
                }

                telemetryData.putLong(COMPOUND_KEY_MIN_PRESSURE, pressure.minPressurePa());
                telemetryData.putLong(COMPOUND_KEY_MAX_PRESSURE, pressure.maxPressurePa());
            }
            case PumpConfiguration pump -> telemetryData.putLong(COMPOUND_KEY_MAX_PRESSURE_BOOST, pump.maxPressureBoostPa());
            case RegulatorConfiguration regulator -> {
                telemetryData.putLong(COMPOUND_KEY_MAX_PRESSURE_RISE, regulator.maxPressureRisePa());
                telemetryData.putLong(COMPOUND_KEY_OUTLET_SET_PRESSURE, regulator.outletSetPressurePa());
            }
            case ValveState valve -> telemetryData.putBoolean(COMPOUND_KEY_VALVE_OPEN, valve.open());
        }
        return telemetryData;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        GasPipeReadout readout = GasPipeReadoutSource.readout(accessor.getBlockEntity());
        if (readout == null) {
            return;
        }

        CompoundTag serverData = accessor.getServerData();
        if (!serverData.contains(COMPOUND_KEY_DATA)) {
            return;
        }

        CompoundTag data = NbtValues.getCompoundOrEmpty(serverData, COMPOUND_KEY_DATA);
        switch (readout) {
            case Flow ignored -> {
                long flowRate = NbtValues.getLongOrDefault(data, COMPOUND_KEY_FLOW_RATE, 0);
                tooltip.add(Component.translatable("jade.gas.flow_rate", GasUnitFormat.formatRate(flowRate)));
            }
            case Pressure ignored -> {
                long minPressure = NbtValues.getLongOrDefault(data, COMPOUND_KEY_MIN_PRESSURE, NO_PRESSURE_READING);
                long maxPressure = NbtValues.getLongOrDefault(data, COMPOUND_KEY_MAX_PRESSURE, NO_PRESSURE_READING);
                boolean hasPressureReading = minPressure >= GasPressure.VACUUM_PA && maxPressure >= minPressure;
                Component maxPressureValue = Component.literal(hasPressureReading ? GasPressure.formatAtm(maxPressure) : "--");
                if (hasPressureReading && GasPressureLimits.isOverpressure(maxPressure)) {
                    maxPressureValue = maxPressureValue.copy().withStyle(ChatFormatting.RED);
                }
                tooltip.add(Component.translatable("jade.gas.max_pressure", maxPressureValue));
                tooltip.add(Component.translatable("jade.gas.pressure_difference", hasPressureReading ? GasPressure.formatAtm(maxPressure - minPressure) : "--"));
            }
            case PumpConfiguration ignored -> {
                if (data.contains(COMPOUND_KEY_MAX_PRESSURE_BOOST)) {
                    long maxPressureBoost = NbtValues.getLongOrDefault(data, COMPOUND_KEY_MAX_PRESSURE_BOOST, GasPressure.VACUUM_PA);
                    tooltip.add(Component.translatable("jade.gas.max_pressure_boost", GasPressure.formatAtm(maxPressureBoost)));
                }
            }
            case RegulatorConfiguration ignored -> {
                if (data.contains(COMPOUND_KEY_MAX_PRESSURE_RISE)) {
                    long maxPressureRise = NbtValues.getLongOrDefault(data, COMPOUND_KEY_MAX_PRESSURE_RISE, GasPressure.VACUUM_PA);
                    tooltip.add(Component.translatable("jade.gas.max_pressure_rise", GasPressure.formatAtm(maxPressureRise)));
                }
                if (data.contains(COMPOUND_KEY_OUTLET_SET_PRESSURE)) {
                    long outletSetPressure = NbtValues.getLongOrDefault(data, COMPOUND_KEY_OUTLET_SET_PRESSURE, GasPressure.VACUUM_PA);
                    Component outletSetPressureValue = Component.literal(GasPressure.formatAtm(outletSetPressure));
                    if (GasPressureLimits.isOverpressure(outletSetPressure)) {
                        outletSetPressureValue = outletSetPressureValue.copy().withStyle(ChatFormatting.RED);
                    }
                    tooltip.add(Component.translatable("jade.gas.outlet_set_pressure", outletSetPressureValue));
                }
            }
            case ValveState ignored -> {
                if (data.contains(COMPOUND_KEY_VALVE_OPEN)) {
                    boolean valveOpen = NbtValues.getBooleanOrDefault(data, COMPOUND_KEY_VALVE_OPEN, false);
                    tooltip.add(Component.translatable("jade.gas.valve_state", Component.translatable(valveOpen ? "jade.gas.valve_open" : "jade.gas.valve_closed")));
                }
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor accessor) {
        CompoundTag telemetryData = createServerData(accessor.getBlockEntity());
        if (telemetryData == null) {
            return;
        }

        compoundTag.put(COMPOUND_KEY_DATA, telemetryData);
    }

    @Override
    public ResourceLocation getUid() {
        return JadePlugin.GAS_PIPE_TELEMETRY_TOOLTIP;
    }
}
