package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlock;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock.calculateStructurePos;

public class TeslaTurbineStructureManager {
    private static final String COMPOUND_KEY_CLOCKWISE_NOZZLES = "ClockwiseNozzles";
    private static final String COMPOUND_KEY_COUNTER_CLOCKWISE_NOZZLES = "CounterClockwiseNozzles";
    private static final String COMPOUND_KEY_VALID = "Valid";

    private final TeslaTurbineCore core;
    private final TeslaTurbineBlockEntity turbine;

    private boolean structureValid;
    private int attachedClockwiseNozzle;
    private int attachedCounterClockwiseNozzle;
    private int previousClockwiseNozzle = -1;
    private int previousCounterClockwiseNozzle = -1;

    public TeslaTurbineStructureManager(@NotNull TeslaTurbineCore core, TeslaTurbineBlockEntity turbine) {
        this.core = core;
        this.turbine = turbine;
    }

    private static int findNozzle(@NotNull Set<Pair<Integer, Integer>> offsets, BlockPos pos, Axis axis, Level level) {
        int nozzles = 0;
        for (Pair<Integer, Integer> offset : offsets) {
            BlockPos nozzlePos = calculateStructurePos(pos, axis, offset.getFirst(), offset.getSecond());
            BlockState nozzleState = level.getBlockState(nozzlePos);
            if (!(nozzleState.getBlock() instanceof TeslaTurbineNozzleBlock)) {
                continue;
            }

            Direction facing = nozzleState.getValue(TeslaTurbineNozzleBlock.FACING).getOpposite();
            BlockPos facingPos = nozzlePos.relative(facing);
            BlockState facingState = level.getBlockState(facingPos);
            if (!(facingState.getBlock() instanceof TeslaTurbineStructuralBlock)) {
                continue;
            }

            BlockPos masterPos = TeslaTurbineStructuralBlock.getMaster(facingPos, facingState);
            if (!masterPos.equals(pos)) {
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

        turbine.notifyUpdate();
    }

    public boolean evaluate() {
        Level level = turbine.getLevel();
        if (level == null) {
            return false;
        }

        BlockState state = turbine.getBlockState();
        BlockPos pos = turbine.getBlockPos();
        Axis axis = state.getValue(TeslaTurbineBlock.AXIS);
        previousClockwiseNozzle = attachedClockwiseNozzle;
        previousCounterClockwiseNozzle = attachedCounterClockwiseNozzle;
        attachedClockwiseNozzle = findNozzle(core.getOffsets(false), pos, axis, level);
        attachedCounterClockwiseNozzle = findNozzle(core.getOffsets(true), pos, axis, level);
        if (!structureValid) {
            structureValid = true;
        }
        return attachedClockwiseNozzle != previousClockwiseNozzle || attachedCounterClockwiseNozzle != previousCounterClockwiseNozzle;
    }

    public void triggerExplosion() {
        BlockState oldState = turbine.getBlockState();
        Level level = turbine.getLevel();
        int rotorCount = oldState.getValue(TeslaTurbineBlock.ROTOR);
        if (rotorCount == 0 || level == null || level.isClientSide) {
            return;
        }

        BlockPos pos = turbine.getBlockPos();
        BlockState newState = oldState.setValue(TeslaTurbineBlock.ROTOR, 0);
        Axis axis = oldState.getValue(TeslaTurbineBlock.AXIS);
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        if (axis == Axis.X) {
            level.explode(null, centerX + 0.5, centerY, centerZ, rotorCount, false, ExplosionInteraction.NONE);
            level.explode(null, centerX - 0.5, centerY, centerZ, rotorCount, false, ExplosionInteraction.NONE);
        }
        else if (axis == Axis.Z) {
            level.explode(null, centerX, centerY, centerZ + 0.5, rotorCount, false, ExplosionInteraction.NONE);
            level.explode(null, centerX, centerY, centerZ - 0.5, rotorCount, false, ExplosionInteraction.NONE);
        }
        else {
            level.explode(null, centerX, centerY + 0.5, centerZ, rotorCount, false, ExplosionInteraction.NONE);
            level.explode(null, centerX, centerY - 0.5, centerZ, rotorCount, false, ExplosionInteraction.NONE);
        }
        level.setBlockAndUpdate(pos, newState);
        turbine.getAdvancementBehaviour().awardPlayer(CCBAdvancements.TESLA_TURBINE_EASY_AS_PIE);
    }

    public boolean isActive() {
        return (attachedClockwiseNozzle > 0 || attachedCounterClockwiseNozzle > 0) && structureValid;
    }

    public void reset() {
        attachedClockwiseNozzle = 0;
        attachedCounterClockwiseNozzle = 0;
        previousClockwiseNozzle = -1;
        previousCounterClockwiseNozzle = -1;
        structureValid = false;
    }

    public int getAttachedNozzle() {
        return attachedClockwiseNozzle + attachedCounterClockwiseNozzle;
    }

    public CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt(COMPOUND_KEY_CLOCKWISE_NOZZLES, attachedClockwiseNozzle);
        compoundTag.putInt(COMPOUND_KEY_COUNTER_CLOCKWISE_NOZZLES, attachedCounterClockwiseNozzle);
        compoundTag.putBoolean(COMPOUND_KEY_VALID, structureValid);
        return compoundTag;
    }

    public void read(@NotNull CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_CLOCKWISE_NOZZLES)) {
            attachedClockwiseNozzle = compoundTag.getInt(COMPOUND_KEY_CLOCKWISE_NOZZLES);
        }
        if (compoundTag.contains(COMPOUND_KEY_COUNTER_CLOCKWISE_NOZZLES)) {
            attachedCounterClockwiseNozzle = compoundTag.getInt(COMPOUND_KEY_COUNTER_CLOCKWISE_NOZZLES);
        }
        if (compoundTag.contains(COMPOUND_KEY_VALID)) {
            structureValid = compoundTag.getBoolean(COMPOUND_KEY_VALID);
        }
    }
}
