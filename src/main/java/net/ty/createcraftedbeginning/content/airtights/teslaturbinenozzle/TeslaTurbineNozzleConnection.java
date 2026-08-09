package net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock.TeslaTurbineStructuralPosition;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class TeslaTurbineNozzleConnection {
    private final TeslaTurbineNozzleBlockEntity nozzle;
    private TeslaTurbineBlockEntity turbine;

    TeslaTurbineNozzleConnection(TeslaTurbineNozzleBlockEntity nozzle) {
        this.nozzle = nozzle;
    }

    @Nullable IGasHandler getGasCapability(@Nullable Direction direction) {
        BlockState state = nozzle.getBlockState();
        if (direction != state.getValue(TeslaTurbineNozzleBlock.FACING)) {
            return null;
        }

        if (turbine == null || turbine.isRemoved()) {
            turbine = findTurbine();
        }
        if (turbine == null) {
            return null;
        }

        boolean clockwise = state.getValue(TeslaTurbineNozzleBlock.CLOCKWISE);
        return turbine.createGasHandler(clockwise);
    }

    void invalidate() {
        turbine = null;
    }

    void scheduleValidation() {
        Level level = nozzle.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = nozzle.getBlockState();
        if (!(state.getBlock() instanceof TeslaTurbineNozzleBlock nozzleBlock)) {
            return;
        }

        BlockPos pos = nozzle.getBlockPos();
        if (level.getBlockTicks().hasScheduledTick(pos, nozzleBlock)) {
            return;
        }

        level.scheduleTick(pos, nozzleBlock, 1);
    }

    private @Nullable TeslaTurbineBlockEntity findTurbine() {
        Level level = nozzle.getLevel();
        if (level == null) {
            return null;
        }

        BlockState nozzleState = nozzle.getBlockState();
        Direction inwardDirection = nozzleState.getValue(TeslaTurbineNozzleBlock.FACING).getOpposite();
        BlockPos nozzlePos = nozzle.getBlockPos();
        BlockPos structurePos = nozzlePos.relative(inwardDirection);
        BlockState structureState = level.getBlockState(structurePos);
        if (!(structureState.getBlock() instanceof TeslaTurbineStructuralBlock)) {
            return null;
        }

        Axis structureAxis = structureState.getValue(TeslaTurbineStructuralBlock.AXIS);
        if (inwardDirection.getAxis() == structureAxis) {
            return null;
        }

        TeslaTurbineStructuralPosition structurePosition = structureState.getValue(TeslaTurbineStructuralBlock.STRUCTURAL_POSITION);
        if (TeslaTurbineStructuralPosition.isMid(structurePosition)) {
            return null;
        }

        if (TeslaTurbineNozzleBlock.hasOtherNozzle(level, structurePos, nozzlePos, structureAxis, structurePosition)) {
            return null;
        }

        BlockPos masterPos = TeslaTurbineStructuralBlock.getMaster(structurePos, structureState);
        return level.getBlockEntity(masterPos) instanceof TeslaTurbineBlockEntity master ? master : null;
    }
}
