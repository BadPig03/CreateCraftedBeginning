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
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.telemetry.GasPressureTelemetryTarget;
import net.ty.createcraftedbeginning.gas.visual.GasPressureDisplayScale;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightManometerBlockEntity extends AbstractAirtightMeterBlockEntity implements GasPressureTelemetryTarget {
    private static final float NEEDLE_MIN_ANGLE = -85;
    private static final float NEEDLE_MAX_ANGLE = 85;
    private static final long NO_PRESSURE_READING = -1;
    private static final long PRESSURE_DISPLAY_STEP_PA = GasPressure.REFERENCE_PRESSURE_PA / 100;
    private static final String MIN_PRESSURE_KEY = "MeterMinPressure";
    private static final String MAX_PRESSURE_KEY = "MeterMaxPressure";

    private final AirtightManometerDisplay display;
    private long minPressurePa = NO_PRESSURE_READING;
    private long maxPressurePa = NO_PRESSURE_READING;

    public AirtightManometerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, NEEDLE_MIN_ANGLE);
        display = new AirtightManometerDisplay(this);
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
        long previousMinDisplay = pressureDisplayBucket(this.minPressurePa);
        long previousMaxDisplay = pressureDisplayBucket(this.maxPressurePa);
        long previousDifferenceDisplay = pressureDifferenceDisplayBucket(this.minPressurePa, this.maxPressurePa);
        this.minPressurePa = nextMinPressure;
        this.maxPressurePa = nextMaxPressure;
        if (!(previousMinDisplay != pressureDisplayBucket(nextMinPressure) || previousMaxDisplay != pressureDisplayBucket(nextMaxPressure) || previousDifferenceDisplay != pressureDifferenceDisplayBucket(nextMinPressure, nextMaxPressure))) {
            return;
        }

        markTelemetryDirty(!hadReading);
        getAdvancementBehaviour().awardPlayer(CCBAdvancements.VISUAL_MONITORING);
    }

    @Override
    public void clearPressureTelemetry() {
        if (!hasPressureReading()) {
            return;
        }

        minPressurePa = NO_PRESSURE_READING;
        maxPressurePa = NO_PRESSURE_READING;
        markTelemetryDirty(true);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (!clientPacket || !hasPressureReading()) {
            return;
        }

        compoundTag.putLong(MIN_PRESSURE_KEY, minPressurePa);
        compoundTag.putLong(MAX_PRESSURE_KEY, maxPressurePa);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!clientPacket) {
            return;
        }

        long readMinPressure = NbtValues.getLongOrDefault(compoundTag, MIN_PRESSURE_KEY, NO_PRESSURE_READING);
        long readMaxPressure = NbtValues.getLongOrDefault(compoundTag, MAX_PRESSURE_KEY, NO_PRESSURE_READING);
        if (readMinPressure < GasPressure.VACUUM_PA || readMaxPressure < readMinPressure) {
            minPressurePa = NO_PRESSURE_READING;
            maxPressurePa = NO_PRESSURE_READING;
            return;
        }

        minPressurePa = readMinPressure;
        maxPressurePa = readMaxPressure;
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

    private float getPressureProgress() {
        if (!hasPressureReading()) {
            return 0;
        }

        return GasPressureDisplayScale.fractionForPressure(maxPressurePa);
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
}
