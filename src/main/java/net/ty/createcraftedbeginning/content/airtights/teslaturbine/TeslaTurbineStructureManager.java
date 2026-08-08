package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock.TeslaTurbineStructuralPosition;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineUtils.NozzlePort;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlock;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineStructureManager {
    private static final String COMPOUND_KEY_CLOCKWISE_NOZZLES = "ClockwiseNozzles";
    private static final String COMPOUND_KEY_COUNTER_CLOCKWISE_NOZZLES = "CounterClockwiseNozzles";
    private static final String COMPOUND_KEY_VALID = "Valid";

    private final TeslaTurbineCore core;
    private final TeslaTurbineBlockEntity turbine;

    private boolean structureValid;
    private int attachedClockwiseNozzles;
    private int attachedCounterClockwiseNozzles;
    private int previousClockwiseNozzles = -1;
    private int previousCounterClockwiseNozzles = -1;

    public TeslaTurbineStructureManager(TeslaTurbineCore core, TeslaTurbineBlockEntity turbine) {
        this.core = core;
        this.turbine = turbine;
    }

    private static boolean isStructureValid(BlockPos turbinePos, Axis axis, Level level) {
        for (int u = -1; u <= 1; u++) {
            for (int v = -1; v <= 1; v++) {
                if (u == 0 && v == 0) {
                    continue;
                }

                BlockPos structuralPos = TeslaTurbineUtils.calculateStructurePos(turbinePos, axis, u, v);
                BlockState structuralState = level.getBlockState(structuralPos);
                if (!(structuralState.getBlock() instanceof TeslaTurbineStructuralBlock)) {
                    return false;
                }

                if (structuralState.getValue(TeslaTurbineStructuralBlock.AXIS) != axis) {
                    return false;
                }

                if (structuralState.getValue(TeslaTurbineStructuralBlock.STRUCTURAL_POSITION) != TeslaTurbineStructuralPosition.fromOffset(u, v)) {
                    return false;
                }

                if (!TeslaTurbineStructuralBlock.getMaster(structuralPos, structuralState).equals(turbinePos)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int countNozzles(List<NozzlePort> ports, BlockPos turbinePos, Axis axis, Level level) {
        int nozzles = 0;
        for (NozzlePort port : ports) {
            BlockPos nozzlePos = port.getWorldPosition(turbinePos, axis);
            BlockState nozzleState = level.getBlockState(nozzlePos);
            if (!(nozzleState.getBlock() instanceof TeslaTurbineNozzleBlock)) {
                continue;
            }

            Direction inward = nozzleState.getValue(TeslaTurbineNozzleBlock.FACING).getOpposite();
            BlockPos structuralPos = nozzlePos.relative(inward);
            BlockState structuralState = level.getBlockState(structuralPos);
            if (!(structuralState.getBlock() instanceof TeslaTurbineStructuralBlock)) {
                continue;
            }

            BlockPos masterPos = TeslaTurbineStructuralBlock.getMaster(structuralPos, structuralState);
            if (!masterPos.equals(turbinePos)) {
                continue;
            }

            nozzles++;
        }
        return nozzles;
    }

    public void tick() {
        if (!evaluate()) {
            return;
        }

        core.markForClientSync();
    }

    public boolean evaluate() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return false;
        }

        BlockState state = turbine.getBlockState();
        BlockPos pos = turbine.getBlockPos();
        Axis axis = state.getValue(TeslaTurbineBlock.AXIS);
        previousClockwiseNozzles = attachedClockwiseNozzles;
        previousCounterClockwiseNozzles = attachedCounterClockwiseNozzles;
        boolean previousStructureValid = structureValid;
        structureValid = isStructureValid(pos, axis, level);
        if (structureValid) {
            attachedClockwiseNozzles = countNozzles(TeslaTurbineUtils.getNozzlePorts(true), pos, axis, level);
            attachedCounterClockwiseNozzles = countNozzles(TeslaTurbineUtils.getNozzlePorts(false), pos, axis, level);
        }
        else {
            attachedClockwiseNozzles = 0;
            attachedCounterClockwiseNozzles = 0;
        }
        return attachedClockwiseNozzles != previousClockwiseNozzles || attachedCounterClockwiseNozzles != previousCounterClockwiseNozzles || structureValid != previousStructureValid;
    }

    public void triggerExplosion() {
        BlockState oldState = turbine.getBlockState();
        Level level = turbine.getLevel();
        int rotorCount = oldState.getValue(TeslaTurbineBlock.ROTOR);
        if (rotorCount == 0 || level == null || level.isClientSide) {
            return;
        }

        BlockPos pos = turbine.getBlockPos();
        Axis axis = oldState.getValue(TeslaTurbineBlock.AXIS);
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        float explosionStrength = rotorCount * Math.max(0, CCBConfig.server().airtights.teslaTurbineExplosionStrengthMultiplier.getF());
        switch (axis) {
            case X -> {
                level.explode(null, centerX + 0.5, centerY, centerZ, explosionStrength, true, ExplosionInteraction.NONE);
                level.explode(null, centerX - 0.5, centerY, centerZ, explosionStrength, true, ExplosionInteraction.NONE);
            }
            case Z -> {
                level.explode(null, centerX, centerY, centerZ + 0.5, explosionStrength, true, ExplosionInteraction.NONE);
                level.explode(null, centerX, centerY, centerZ - 0.5, explosionStrength, true, ExplosionInteraction.NONE);
            }
            default -> {
                level.explode(null, centerX, centerY + 0.5, centerZ, explosionStrength, true, ExplosionInteraction.NONE);
                level.explode(null, centerX, centerY - 0.5, centerZ, explosionStrength, true, ExplosionInteraction.NONE);
            }
        }
        level.setBlockAndUpdate(pos, oldState.setValue(TeslaTurbineBlock.ROTOR, 0));
        turbine.getAdvancementBehaviour().awardPlayer(CCBAdvancements.TESLA_TURBINE_EASY_AS_PIE);
    }

    public boolean isActive() {
        return (attachedClockwiseNozzles > 0 || attachedCounterClockwiseNozzles > 0) && structureValid;
    }

    public void reset() {
        boolean changed = attachedClockwiseNozzles != 0 || attachedCounterClockwiseNozzles != 0 || structureValid;
        clearDerivedState();
        previousClockwiseNozzles = -1;
        previousCounterClockwiseNozzles = -1;
        if (!changed) {
            return;
        }

        core.markForClientSync();
    }

    public void invalidateForServerLoad() {
        clearDerivedState();
        previousClockwiseNozzles = -1;
        previousCounterClockwiseNozzles = -1;
    }

    public int getAttachedNozzle() {
        return attachedClockwiseNozzles + attachedCounterClockwiseNozzles;
    }

    public CompoundTag writeClient() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt(COMPOUND_KEY_CLOCKWISE_NOZZLES, attachedClockwiseNozzles);
        compoundTag.putInt(COMPOUND_KEY_COUNTER_CLOCKWISE_NOZZLES, attachedCounterClockwiseNozzles);
        compoundTag.putBoolean(COMPOUND_KEY_VALID, structureValid);
        return compoundTag;
    }

    public void readClient(CompoundTag compoundTag) {
        attachedClockwiseNozzles = Mth.clamp(compoundTag.getInt(COMPOUND_KEY_CLOCKWISE_NOZZLES), 0, TeslaTurbineUtils.MAX_NOZZLES_PER_DIRECTION);
        attachedCounterClockwiseNozzles = Mth.clamp(compoundTag.getInt(COMPOUND_KEY_COUNTER_CLOCKWISE_NOZZLES), 0, TeslaTurbineUtils.MAX_NOZZLES_PER_DIRECTION);
        structureValid = compoundTag.getBoolean(COMPOUND_KEY_VALID);
        previousClockwiseNozzles = attachedClockwiseNozzles;
        previousCounterClockwiseNozzles = attachedCounterClockwiseNozzles;
    }

    private void clearDerivedState() {
        attachedClockwiseNozzles = 0;
        attachedCounterClockwiseNozzles = 0;
        structureValid = false;
    }
}
