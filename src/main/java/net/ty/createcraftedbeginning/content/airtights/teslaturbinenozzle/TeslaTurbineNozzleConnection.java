package net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineGeometry.NozzlePort;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class TeslaTurbineNozzleConnection {
    private final TeslaTurbineNozzleBlockEntity nozzle;
    private @Nullable TeslaTurbineBlockEntity turbine;
    private @Nullable TeslaTurbineNozzleGasHandler gasCapability;
    private @Nullable NozzlePort gasCapabilityPort;
    private @Nullable Direction gasCapabilityFacing;

    TeslaTurbineNozzleConnection(TeslaTurbineNozzleBlockEntity nozzle) {
        this.nozzle = nozzle;
    }

    @Nullable GasHandler getGasCapability(@Nullable Direction accessDirection) {
        Level level = nozzle.getLevel();
        BlockPos nozzlePos = nozzle.getBlockPos();
        if (level == null || nozzle.isRemoved() || !level.isLoaded(nozzlePos) || level.getBlockEntity(nozzlePos) != nozzle) {
            invalidate();
            return null;
        }

        BlockState nozzleState = nozzle.getBlockState();
        Direction facing = nozzleState.getValue(TeslaTurbineNozzleBlock.FACING);
        if (accessDirection != facing) {
            return null;
        }

        BlockPos structurePos = nozzlePos.relative(facing.getOpposite());
        if (!level.isLoaded(structurePos)) {
            invalidate();
            return null;
        }

        NozzlePort port = TeslaTurbineNozzleBlock.findPort(level, facing.getOpposite(), nozzlePos);
        if (port == null) {
            invalidate();
            return null;
        }

        BlockState structureState = level.getBlockState(structurePos);
        BlockPos masterPos = TeslaTurbineStructuralBlock.getMaster(structurePos, structureState);
        if (!level.isLoaded(masterPos) || !(level.getBlockEntity(masterPos) instanceof TeslaTurbineBlockEntity master) || master.isRemoved() || master.getBlockState().getValue(TeslaTurbineBlock.AXIS) != structureState.getValue(TeslaTurbineStructuralBlock.AXIS)) {
            invalidate();
            return null;
        }

        if (gasCapability == null || turbine != master || gasCapabilityPort != port || gasCapabilityFacing != facing) {
            turbine = master;
            gasCapabilityPort = port;
            gasCapabilityFacing = facing;
            gasCapability = new TeslaTurbineNozzleGasHandler(this, (GasPressureBoundary) master.createGasHandler(port.clockwise()));
        }
        return gasCapability;
    }

    boolean isCurrent(TeslaTurbineNozzleGasHandler handler) {
        return gasCapability == handler && getGasCapability(gasCapabilityFacing) == handler;
    }

    void invalidate() {
        turbine = null;
        gasCapability = null;
        gasCapabilityPort = null;
        gasCapabilityFacing = null;
    }

    void scheduleValidation() {
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
}
