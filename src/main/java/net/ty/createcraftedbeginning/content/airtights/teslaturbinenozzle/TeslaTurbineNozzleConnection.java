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
public final class TeslaTurbineNozzleConnection {
    private final TeslaTurbineNozzleBlockEntity nozzle;
    private TeslaTurbineBlockEntity turbine;

    public TeslaTurbineNozzleConnection(TeslaTurbineNozzleBlockEntity nozzle) {
        this.nozzle = nozzle;
    }

    @Nullable public IGasHandler getGasCapability(@Nullable Direction accessDirection) {
        BlockState nozzleState = nozzle.getBlockState();
        if (accessDirection != nozzleState.getValue(TeslaTurbineNozzleBlock.FACING)) {
            return null;
        }

        if (turbine == null || turbine.isRemoved()) {
            turbine = findTurbine();
        }
        if (turbine == null) {
            return null;
        }

        return turbine.createGasHandler(nozzleState.getValue(TeslaTurbineNozzleBlock.CLOCKWISE));
    }

    public void invalidate() {
        turbine = null;
    }

    public void scheduleValidation() {
        Level level = nozzle.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        if (!(nozzle.getBlockState().getBlock() instanceof TeslaTurbineNozzleBlock nozzleBlock)) {
            return;
        }

        BlockPos nozzlePos = nozzle.getBlockPos();
        if (level.getBlockTicks().hasScheduledTick(nozzlePos, nozzleBlock)) {
            return;
        }

        level.scheduleTick(nozzlePos, nozzleBlock, 1);
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
