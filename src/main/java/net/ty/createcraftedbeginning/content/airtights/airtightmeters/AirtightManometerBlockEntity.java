package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.telemetry.GasPressureTelemetryTarget;
import net.ty.createcraftedbeginning.gas.visual.GasPipeReadout.Pressure;
import net.ty.createcraftedbeginning.gas.visual.GasPipeReadoutSource;
import net.ty.createcraftedbeginning.gas.visual.GasPressureDisplayScale;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightManometerBlockEntity extends AbstractAirtightMeterBlockEntity implements GasPressureTelemetryTarget, GasPipeReadoutSource {
    private static final float NEEDLE_MIN_ANGLE = -85;
    private static final float NEEDLE_MAX_ANGLE = 85;
    private static final long NO_PRESSURE_READING = -1;
    private static final long PRESSURE_DISPLAY_STEP_PA = GasPressure.REFERENCE_PRESSURE_PA / 100;
    private static final String COMPOUND_KEY_MIN_PRESSURE = "MeterMinPressure";
    private static final String COMPOUND_KEY_MAX_PRESSURE = "MeterMaxPressure";
    private static final String COMPOUND_KEY_AVERAGE_MAX_PRESSURE = "MeterAverageMaxPressure";
    private static final String COMPOUND_KEY_AVERAGE_PRESSURE_DIFFERENCE = "MeterAveragePressureDifference";

    private final AirtightManometerDisplay display;
    private final MeterReadingWindow maxWindow = new MeterReadingWindow();
    private final MeterReadingWindow differenceWindow = new MeterReadingWindow();
    private long averageMaxPressurePa = NO_PRESSURE_READING;
    private long averageDifferencePa = NO_PRESSURE_READING;
    private long minPressurePa = NO_PRESSURE_READING;
    private long maxPressurePa = NO_PRESSURE_READING;

    public AirtightManometerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, NEEDLE_MIN_ANGLE);
        display = new AirtightManometerDisplay(this);
    }

    private static long pressureDisplayBucket(long pressurePa) {
        if (pressurePa < GasPressure.VACUUM_PA) {
            return NO_PRESSURE_READING;
        }

        return roundNonNegative(pressurePa);
    }

    private static long pressureDifferenceDisplayBucket(long minPressurePa, long maxPressurePa) {
        if (minPressurePa < GasPressure.VACUUM_PA || maxPressurePa < minPressurePa) {
            return NO_PRESSURE_READING;
        }

        return pressureDisplayBucket(maxPressurePa - minPressurePa);
    }

    private static long roundNonNegative(long value) {
        long quotient = value / PRESSURE_DISPLAY_STEP_PA;
        long remainder = value % PRESSURE_DISPLAY_STEP_PA;
        if (remainder >= 500L) {
            return quotient + 1;
        }

        return quotient;
    }

    @Override
    public Pressure getGasPipeReadout() {
        return new Pressure(minPressurePa, maxPressurePa, averageMaxPressurePa, averageDifferencePa);
    }

    @Override
    protected float getNeedleTargetAngle() {
        return Mth.lerp(getPressureProgress(), NEEDLE_MIN_ANGLE, NEEDLE_MAX_ANGLE);
    }

    @Override
    public int getComparatorOutput() {
        return calculateComparatorSignal(getPressureProgress());
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return display.addToGoggleTooltip(tooltip);
    }

    @Override
    public void acceptPressureTelemetry(long minPressurePa, long maxPressurePa) {
        long nextMinPressure = Math.max(GasPressure.VACUUM_PA, minPressurePa);
        long nextMaxPressure = Math.max(nextMinPressure, maxPressurePa);
        if (this.minPressurePa == nextMinPressure && this.maxPressurePa == nextMaxPressure) {
            return;
        }

        boolean hadReading = hasPressureReading();
        boolean wasOverpressure = GasPressureLimits.isOverpressure(this.maxPressurePa);
        long previousMinDisplay = pressureDisplayBucket(this.minPressurePa);
        long previousMaxDisplay = pressureDisplayBucket(this.maxPressurePa);
        long previousDifferenceDisplay = pressureDifferenceDisplayBucket(this.minPressurePa, this.maxPressurePa);
        this.minPressurePa = nextMinPressure;
        this.maxPressurePa = nextMaxPressure;
        if (!hadReading) {
            averageMaxPressurePa = nextMaxPressure;
            averageDifferencePa = nextMaxPressure - nextMinPressure;
        }
        if (wasOverpressure != GasPressureLimits.isOverpressure(nextMaxPressure)) {
            markTelemetryDirty(true);
        }

        if (!(previousMinDisplay != pressureDisplayBucket(nextMinPressure) || previousMaxDisplay != pressureDisplayBucket(nextMaxPressure) || previousDifferenceDisplay != pressureDifferenceDisplayBucket(nextMinPressure, nextMaxPressure))) {
            return;
        }

        markTelemetryDirty(!hadReading);
    }

    @Override
    public void clearPressureTelemetry() {
        if (!hasPressureReading()) {
            return;
        }

        minPressurePa = NO_PRESSURE_READING;
        maxPressurePa = NO_PRESSURE_READING;
        averageMaxPressurePa = NO_PRESSURE_READING;
        averageDifferencePa = NO_PRESSURE_READING;
        maxWindow.clear();
        differenceWindow.clear();
        markTelemetryDirty(true);
    }

    @Override
    protected void updateDisplayReading() {
        if (!hasPressureReading()) {
            return;
        }

        long previousMax = averageMaxPressurePa;
        long previousDifference = averageDifferencePa;
        averageMaxPressurePa = maxWindow.sample(maxPressurePa);
        averageDifferencePa = differenceWindow.sample(maxPressurePa - minPressurePa);
        if (pressureDisplayBucket(previousMax) == pressureDisplayBucket(averageMaxPressurePa) && pressureDisplayBucket(previousDifference) == pressureDisplayBucket(averageDifferencePa)) {
            return;
        }

        markTelemetryDirty(false);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (!clientPacket || !hasPressureReading()) {
            return;
        }

        compoundTag.putLong(COMPOUND_KEY_MIN_PRESSURE, minPressurePa);
        compoundTag.putLong(COMPOUND_KEY_MAX_PRESSURE, maxPressurePa);
        compoundTag.putLong(COMPOUND_KEY_AVERAGE_MAX_PRESSURE, averageMaxPressurePa);
        compoundTag.putLong(COMPOUND_KEY_AVERAGE_PRESSURE_DIFFERENCE, averageDifferencePa);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!clientPacket) {
            return;
        }

        long readMinPressure = NbtValues.getLongOrDefault(compoundTag, COMPOUND_KEY_MIN_PRESSURE, NO_PRESSURE_READING);
        long readMaxPressure = NbtValues.getLongOrDefault(compoundTag, COMPOUND_KEY_MAX_PRESSURE, NO_PRESSURE_READING);
        if (readMinPressure < GasPressure.VACUUM_PA || readMaxPressure < readMinPressure) {
            minPressurePa = NO_PRESSURE_READING;
            maxPressurePa = NO_PRESSURE_READING;
            averageMaxPressurePa = NO_PRESSURE_READING;
            averageDifferencePa = NO_PRESSURE_READING;
            return;
        }

        minPressurePa = readMinPressure;
        maxPressurePa = readMaxPressure;
        averageMaxPressurePa = NbtValues.getLongOrDefault(compoundTag, COMPOUND_KEY_AVERAGE_MAX_PRESSURE, NO_PRESSURE_READING);
        averageDifferencePa = NbtValues.getLongOrDefault(compoundTag, COMPOUND_KEY_AVERAGE_PRESSURE_DIFFERENCE, NO_PRESSURE_READING);
    }

    public boolean hasPressureReading() {
        return minPressurePa >= GasPressure.VACUUM_PA && maxPressurePa >= minPressurePa;
    }

    public long getMinPressurePa() {
        return minPressurePa;
    }

    public long getMaxPressurePa() {
        return maxPressurePa;
    }

    public long getPressureDifferencePa() {
        if (!hasPressureReading()) {
            return GasPressure.VACUUM_PA;
        }

        return Math.max(GasPressure.VACUUM_PA, maxPressurePa - minPressurePa);
    }

    long getAverageMaxPressurePa() {
        return averageMaxPressurePa;
    }

    long getAverageDifferencePa() {
        return averageDifferencePa;
    }

    private float getPressureProgress() {
        if (!hasPressureReading()) {
            return 0;
        }

        return GasPressureDisplayScale.fractionForPressure(averageMaxPressurePa);
    }
}
