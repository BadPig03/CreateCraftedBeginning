package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasPipeBlock;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractAirtightMeterBlock<T extends AbstractAirtightMeterBlockEntity> extends AxisGasPipeBlock implements IBE<T>, GasConnectable {
    protected AbstractAirtightMeterBlock(Properties properties) {
        super(properties);
    }

    public static boolean isDisplayFace(BlockState state, Direction face) {
        Axis pipeAxis = state.getValue(AXIS);
        Axis faceAxis = face.getAxis();
        return switch (pipeAxis) {
            case Y -> faceAxis != Axis.Y;
            case Z -> faceAxis == Axis.X;
            default -> faceAxis == Axis.Z;
        };
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(AbstractAirtightMeterBlockEntity::getComparatorOutput).orElse(0);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace) {
        return currentState.getValue(AXIS) == localFace.getAxis();
    }
}
