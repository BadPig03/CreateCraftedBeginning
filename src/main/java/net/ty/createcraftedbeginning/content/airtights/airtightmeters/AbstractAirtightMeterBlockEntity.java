package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AbstractAirtightPipeBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasPipeBlock;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractAirtightMeterBlockEntity extends AbstractAirtightPipeBlockEntity implements IHaveGoggleInformation {
    private static final float NEEDLE_CHASE_SPEED = 0.2F;
    private static final int MAX_COMPARATOR_SIGNAL = 15;
    private static final int TELEMETRY_SYNC_INTERVAL = 4;

    private final LerpedFloat needleAngle;
    private int lastComparatorOutput = -1;
    private boolean telemetryDirty;
    private boolean telemetryImmediate;

    protected AbstractAirtightMeterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, float initialNeedleAngle) {
        super(type, pos, state);
        getAdvancementBehaviour().add(CCBAdvancements.VISUAL_MONITORING);
        needleAngle = LerpedFloat.linear().startWithValue(initialNeedleAngle);
    }

    @Override
    protected GasTransportBehaviour createTransportBehaviour() {
        return new AirtightMeterTransportBehaviour(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            needleAngle.chase(getNeedleTargetAngle(), NEEDLE_CHASE_SPEED, Chaser.EXP);
            needleAngle.tickChaser();
            return;
        }

        updateComparatorOutput();
        flushTelemetrySync();
    }

    @Override
    public boolean allowsGasTransport(Level level, BlockState state, BlockPos pos, Direction direction) {
        return AxisGasPipeBlock.isOpenAt(state, direction);
    }

    public abstract int getComparatorOutput();

    protected static int calculateComparatorSignal(float progress) {
        return Mth.clamp(Math.round(Mth.clamp(progress, 0, 1) * MAX_COMPARATOR_SIGNAL), 0, MAX_COMPARATOR_SIGNAL);
    }

    protected abstract float getNeedleTargetAngle();

    protected final void markTelemetryDirty(boolean immediate) {
        telemetryDirty = true;
        telemetryImmediate |= immediate;
    }

    float getNeedleAngle(float partialTicks) {
        return needleAngle.getValue(partialTicks);
    }

    private void updateComparatorOutput() {
        if (level == null) {
            return;
        }

        int comparatorOutput = getComparatorOutput();
        if (comparatorOutput == lastComparatorOutput) {
            return;
        }

        lastComparatorOutput = comparatorOutput;
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private void flushTelemetrySync() {
        if (!telemetryDirty || !telemetryImmediate && !isTelemetrySyncTick()) {
            return;
        }

        notifyUpdate();
        telemetryDirty = false;
        telemetryImmediate = false;
    }

    private boolean isTelemetrySyncTick() {
        if (level == null) {
            return false;
        }

        long phaseKey = worldPosition.getX() + worldPosition.getY() + worldPosition.getZ();
        int phase = Math.floorMod(phaseKey, TELEMETRY_SYNC_INTERVAL);
        return Math.floorMod(level.getGameTime(), TELEMETRY_SYNC_INTERVAL) == phase;
    }
}
