package net.ty.createcraftedbeginning.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility.PipeReadout;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightFlowmeterBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightManometerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump.AirtightRegulatorPumpBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightvalve.AirtightValveBlockEntity;
import net.ty.createcraftedbeginning.foundation.NbtValues;
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
    private static final String DATA_KEY = "JadeGasPipeTelemetry";
    private static final String MIN_PRESSURE_KEY = "MinPressure";
    private static final String MAX_PRESSURE_KEY = "MaxPressure";
    private static final String FLOW_RATE_KEY = "FlowRate";
    private static final String MAX_PRESSURE_BOOST_KEY = "MaxPressureBoost";
    private static final String MAX_PRESSURE_RISE_KEY = "MaxPressureRise";
    private static final String OUTLET_SET_PRESSURE_KEY = "OutletSetPressure";
    private static final String VALVE_OPEN_KEY = "ValveOpen";

    static @Nullable CompoundTag createServerData(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null) {
            return null;
        }

        CompoundTag telemetryData = new CompoundTag();
        switch (AirtightTelemetryVisibility.pipeReadout(blockEntity)) {
            case FLOW -> telemetryData.putLong(FLOW_RATE_KEY, ((AirtightFlowmeterBlockEntity) blockEntity).getFlowRate());
            case PRESSURE -> {
                AirtightManometerBlockEntity manometer = (AirtightManometerBlockEntity) blockEntity;
                if (!manometer.hasPressureReading()) {
                    return telemetryData;
                }

                telemetryData.putLong(MIN_PRESSURE_KEY, manometer.getMinPressurePa());
                telemetryData.putLong(MAX_PRESSURE_KEY, manometer.getMaxPressurePa());
            }
            case PUMP_CONFIGURATION -> telemetryData.putLong(MAX_PRESSURE_BOOST_KEY, ((AirtightPumpBlockEntity) blockEntity).getPumpMaxPressureBoostPa());
            case REGULATOR_CONFIGURATION -> {
                AirtightRegulatorPumpBlockEntity regulatorPump = (AirtightRegulatorPumpBlockEntity) blockEntity;
                telemetryData.putLong(MAX_PRESSURE_RISE_KEY, regulatorPump.getMaxPressureRisePa());
                telemetryData.putLong(OUTLET_SET_PRESSURE_KEY, regulatorPump.getOutletSetPressurePa());
            }
            case VALVE_STATE -> telemetryData.putBoolean(VALVE_OPEN_KEY, ((AirtightValveBlockEntity) blockEntity).isOpen());
            case NONE -> { return null; }
        }
        return telemetryData;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        PipeReadout readout = AirtightTelemetryVisibility.pipeReadout(accessor.getBlockEntity());
        if (readout == PipeReadout.NONE) {
            return;
        }

        CompoundTag serverData = accessor.getServerData();
        if (!serverData.contains(DATA_KEY)) {
            return;
        }

        CompoundTag data = NbtValues.getCompoundOrEmpty(serverData, DATA_KEY);
        switch (readout) {
            case FLOW -> {
                long flowRate = NbtValues.getLongOrDefault(data, FLOW_RATE_KEY, 0);
                tooltip.add(Component.translatable("jade.gas.flow_rate", GasUnitFormat.formatRate(flowRate)));
            }
            case PRESSURE -> {
                long minPressure = NbtValues.getLongOrDefault(data, MIN_PRESSURE_KEY, NO_PRESSURE_READING);
                long maxPressure = NbtValues.getLongOrDefault(data, MAX_PRESSURE_KEY, NO_PRESSURE_READING);
                boolean hasPressureReading = minPressure >= GasPressure.VACUUM_PA && maxPressure >= minPressure;
                Component maxPressureValue = Component.literal(hasPressureReading ? GasPressure.formatAtm(maxPressure) : "--");
                if (hasPressureReading && GasPressureLimits.isOverpressure(maxPressure)) {
                    maxPressureValue = maxPressureValue.copy().withStyle(ChatFormatting.RED);
                }
                tooltip.add(Component.translatable("jade.gas.max_pressure", maxPressureValue));
                tooltip.add(Component.translatable("jade.gas.pressure_difference", hasPressureReading ? GasPressure.formatAtm(maxPressure - minPressure) : "--"));
            }
            case PUMP_CONFIGURATION -> {
                if (data.contains(MAX_PRESSURE_BOOST_KEY)) {
                    long maxPressureBoost = NbtValues.getLongOrDefault(data, MAX_PRESSURE_BOOST_KEY, GasPressure.VACUUM_PA);
                    tooltip.add(Component.translatable("jade.gas.max_pressure_boost", GasPressure.formatAtm(maxPressureBoost)));
                }
            }
            case REGULATOR_CONFIGURATION -> {
                if (data.contains(MAX_PRESSURE_RISE_KEY)) {
                    long maxPressureRise = NbtValues.getLongOrDefault(data, MAX_PRESSURE_RISE_KEY, GasPressure.VACUUM_PA);
                    tooltip.add(Component.translatable("jade.gas.max_pressure_rise", GasPressure.formatAtm(maxPressureRise)));
                }
                if (data.contains(OUTLET_SET_PRESSURE_KEY)) {
                    long outletSetPressure = NbtValues.getLongOrDefault(data, OUTLET_SET_PRESSURE_KEY, GasPressure.VACUUM_PA);
                    Component outletSetPressureValue = Component.literal(GasPressure.formatAtm(outletSetPressure));
                    if (GasPressureLimits.isOverpressure(outletSetPressure)) {
                        outletSetPressureValue = outletSetPressureValue.copy().withStyle(ChatFormatting.RED);
                    }
                    tooltip.add(Component.translatable("jade.gas.outlet_set_pressure", outletSetPressureValue));
                }
            }
            case VALVE_STATE -> {
                if (data.contains(VALVE_OPEN_KEY)) {
                    boolean valveOpen = NbtValues.getBooleanOrDefault(data, VALVE_OPEN_KEY, false);
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

        compoundTag.put(DATA_KEY, telemetryData);
    }

    @Override
    public ResourceLocation getUid() {
        return JadePlugin.GAS_PIPE_TELEMETRY_TOOLTIP;
    }
}
