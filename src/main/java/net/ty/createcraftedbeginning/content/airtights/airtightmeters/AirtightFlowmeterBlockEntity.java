package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.telemetry.GasFlowTelemetryTarget;
import net.ty.createcraftedbeginning.gas.visual.GasPipeReadout.Flow;
import net.ty.createcraftedbeginning.gas.visual.GasPipeReadoutSource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightFlowmeterBlockEntity extends AbstractAirtightMeterBlockEntity implements GasFlowTelemetryTarget, GasPipeReadoutSource {
    private static final long MAX_FLOW_RATE = 16000;
    private static final float NEEDLE_MIN_ANGLE = -85;
    private static final float NEEDLE_MAX_ANGLE = 85;
    private static final String COMPOUND_KEY_FLOW_RATE = "MeterFlowRate";
    private static final String COMPOUND_KEY_AVERAGE_FLOW_RATE = "MeterAverageFlowRate";

    private final AirtightFlowmeterDisplay display;
    private final MeterReadingWindow window = MeterReadingWindow.startingAtZero();
    private long averageFlowRate;
    private long flowRate;

    public AirtightFlowmeterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, NEEDLE_MIN_ANGLE);
        display = new AirtightFlowmeterDisplay(this);
    }

    private static float flowProgress(long flowRate) {
        return Mth.clamp((float) flowRate / MAX_FLOW_RATE, 0, 1);
    }

    private static long flowDisplayBucket(long flowRate) {
        if (flowRate < GasUnits.GU_PER_KGU) {
            return flowRate;
        }

        if (flowRate < GasUnits.GU_PER_MGU) {
            return GasUnits.GU_PER_KGU + roundNonNegative(flowRate, GasUnits.GU_PER_KGU / 100);
        }

        if (flowRate < GasUnits.GU_PER_GGU) {
            return GasUnits.GU_PER_MGU + roundNonNegative(flowRate, GasUnits.GU_PER_MGU / 100);
        }

        return GasUnits.GU_PER_GGU + roundNonNegative(flowRate, GasUnits.GU_PER_GGU / 100);
    }

    private static long roundNonNegative(long value, long divisor) {
        long quotient = value / divisor;
        long remainder = value % divisor;
        if (remainder >= (divisor + 1) / 2) {
            return quotient + 1;
        }

        return quotient;
    }

    @Override
    public Flow getGasPipeReadout() {
        return new Flow(flowRate, averageFlowRate);
    }

    @Override
    protected float getNeedleTargetAngle() {
        return Mth.lerp(flowProgress(averageFlowRate), NEEDLE_MIN_ANGLE, NEEDLE_MAX_ANGLE);
    }

    @Override
    public int getComparatorOutput() {
        return calculateComparatorSignal(flowProgress(averageFlowRate));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return display.addToGoggleTooltip(tooltip);
    }

    @Override
    public void acceptFlowTelemetry(long flowRate) {
        long previous = this.flowRate;
        this.flowRate = Math.max(0, flowRate);
        syncChangedRate(previous, this.flowRate);
    }

    @Override
    protected void updateDisplayReading() {
        long oldAverage = averageFlowRate;
        averageFlowRate = window.sample(flowRate);
        syncChangedRate(oldAverage, averageFlowRate);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (!clientPacket) {
            return;
        }

        if (flowRate > 0) {
            compoundTag.putLong(COMPOUND_KEY_FLOW_RATE, flowRate);
        }
        if (averageFlowRate == 0) {
            return;
        }

        compoundTag.putLong(COMPOUND_KEY_AVERAGE_FLOW_RATE, averageFlowRate);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!clientPacket) {
            return;
        }

        flowRate = Math.max(0, NbtValues.getLongOrDefault(compoundTag, COMPOUND_KEY_FLOW_RATE, 0));
        averageFlowRate = Math.max(0, NbtValues.getLongOrDefault(compoundTag, COMPOUND_KEY_AVERAGE_FLOW_RATE, 0));
    }

    public long getFlowRate() {
        return flowRate;
    }

    long getAverageFlowRate() {
        return averageFlowRate;
    }

    private void syncChangedRate(long previous, long current) {
        if (previous == current || flowDisplayBucket(previous) == flowDisplayBucket(current)) {
            return;
        }

        markTelemetryDirty(previous == 0 || current == 0);
    }
}
