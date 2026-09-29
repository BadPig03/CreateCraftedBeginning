package net.ty.createcraftedbeginning.gas.network.solver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.FacePressure;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.FacePressureSamples;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.PressureLayout;
import net.ty.createcraftedbeginning.gas.telemetry.GasPressureTelemetryTarget;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class GasPipePressureAccumulator {
    private static final int FACE_COUNT = Direction.values().length;
    private static final long NO_PRESSURE_READING = Long.MIN_VALUE;
    private final List<BlockPos> targetPositions;
    private final Map<BlockPos, Integer> targetIndicesByPosition;
    private @Nullable PressureLayout layout;
    private int[] targetIndexByPipe = new int[0];
    private int[] expectedFaceMasks = new int[0];
    private long[] currentGasPressureByFace = new long[0];
    private long[] minPressureByTarget = new long[0];
    private long[] maxPressureByTarget = new long[0];
    private int[] observedFaceMasks = new int[0];
    private int[] touchedFaceSlots = new int[0];
    private int touchedFaceCount;

    public GasPipePressureAccumulator(GasPipeTelemetryTargets telemetryTargets) {
        targetPositions = telemetryTargets.pressureTargetPositions();
        targetIndicesByPosition = telemetryTargets.pressureTargetIndicesByPosition();
    }

    public int record(List<FacePressure> pressures) {
        if (!isEnabled() || pressures.isEmpty()) {
            return 0;
        }

        if (!(pressures instanceof FacePressureSamples samples)) {
            throw new IllegalStateException("Pressure solver returned non-compact face samples.");
        }

        PressureLayout sampleLayout = samples.layout();
        ensureLayout(sampleLayout);
        int collectedSamples = 0;
        for (int portIndex = 0; portIndex < samples.portCount(); portIndex++) {
            if (!samples.hasPressureAtPort(portIndex)) {
                continue;
            }

            int pipeIndex = sampleLayout.pipeIndexForPort(portIndex);
            int targetIndex = targetIndexByPipe[pipeIndex];
            if (targetIndex < 0) {
                continue;
            }

            int faceOrdinal = sampleLayout.faceOrdinalForPort(portIndex);
            int slot = targetIndex * FACE_COUNT + faceOrdinal;
            if (currentGasPressureByFace[slot] == NO_PRESSURE_READING) {
                touchedFaceSlots[touchedFaceCount++] = slot;
            }
            currentGasPressureByFace[slot] = GasPressure.round(GasPressureLimits.clampToHardLimit(samples.pressurePaAtPort(portIndex)));
            collectedSamples++;
        }
        return collectedSamples;
    }

    public void finishGas() {
        for (int touchedIndex = 0; touchedIndex < touchedFaceCount; touchedIndex++) {
            int slot = touchedFaceSlots[touchedIndex];
            long pressurePa = currentGasPressureByFace[slot];
            int targetIndex = slot / FACE_COUNT;
            int faceOrdinal = slot % FACE_COUNT;
            minPressureByTarget[targetIndex] = Math.min(minPressureByTarget[targetIndex], pressurePa);
            maxPressureByTarget[targetIndex] = Math.max(maxPressureByTarget[targetIndex], pressurePa);
            observedFaceMasks[targetIndex] |= 1 << faceOrdinal;
            currentGasPressureByFace[slot] = NO_PRESSURE_READING;
        }
        touchedFaceCount = 0;
    }

    public void apply(Level level) {
        if (!isEnabled()) {
            return;
        }

        for (int targetIndex = 0; targetIndex < targetPositions.size(); targetIndex++) {
            BlockEntity blockEntity = level.getBlockEntity(targetPositions.get(targetIndex));
            if (!(blockEntity instanceof GasPressureTelemetryTarget telemetryTarget)) {
                continue;
            }

            long minPressure = layout == null ? Long.MAX_VALUE : minPressureByTarget[targetIndex];
            int expectedFaceMask = layout == null ? 0 : expectedFaceMasks[targetIndex];
            if (minPressure == Long.MAX_VALUE || expectedFaceMask == 0 || (observedFaceMasks[targetIndex] & expectedFaceMask) != expectedFaceMask) {
                telemetryTarget.clearPressureTelemetry();
                continue;
            }

            telemetryTarget.acceptPressureTelemetry(minPressure, maxPressureByTarget[targetIndex]);
        }
    }

    boolean isEnabled() {
        return !targetPositions.isEmpty();
    }

    private void ensureLayout(PressureLayout sampleLayout) {
        if (layout == sampleLayout) {
            return;
        }

        if (layout != null) {
            throw new IllegalStateException("A pressure accumulator cannot mix layouts within one network solve.");
        }

        layout = sampleLayout;
        int targetCount = targetPositions.size();
        targetIndexByPipe = new int[sampleLayout.pipeCount()];
        Arrays.fill(targetIndexByPipe, -1);
        expectedFaceMasks = new int[targetCount];
        for (int pipeIndex = 0; pipeIndex < sampleLayout.pipeCount(); pipeIndex++) {
            Integer targetIndex = targetIndicesByPosition.get(sampleLayout.pipePos(pipeIndex));
            if (targetIndex == null) {
                continue;
            }

            targetIndexByPipe[pipeIndex] = targetIndex;
            expectedFaceMasks[targetIndex] = sampleLayout.connectedFaceMask(pipeIndex);
        }

        int faceSlotCount = targetCount * FACE_COUNT;
        currentGasPressureByFace = new long[faceSlotCount];
        Arrays.fill(currentGasPressureByFace, NO_PRESSURE_READING);
        minPressureByTarget = new long[targetCount];
        Arrays.fill(minPressureByTarget, Long.MAX_VALUE);
        maxPressureByTarget = new long[targetCount];
        Arrays.fill(maxPressureByTarget, GasPressure.VACUUM_PA);
        observedFaceMasks = new int[targetCount];
        touchedFaceSlots = new int[faceSlotCount];
    }

}
