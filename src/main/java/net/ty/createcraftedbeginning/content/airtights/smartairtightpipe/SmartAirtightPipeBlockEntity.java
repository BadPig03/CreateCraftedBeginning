package net.ty.createcraftedbeginning.content.airtights.smartairtightpipe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.gases.GasFilteringBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.GasPropagator;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.IDirectionalPipe;
import net.ty.createcraftedbeginning.api.gas.gases.IDirectionalPipe.DirectionalFacing;
import net.ty.createcraftedbeginning.api.gas.gases.IGasExtractor;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SmartAirtightPipeBlockEntity extends SmartBlockEntity implements IGasExtractor {
    private GasFilteringBehaviour filter;

    public SmartAirtightPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        filter = new GasFilteringBehaviour(this, new SmartPipeFilterSlot()).withCallback(this::onFilterChanged);
        SmartPipeTransportBehaviour transportBehaviour = new SmartPipeTransportBehaviour(this);
        behaviours.add(filter);
        behaviours.add(transportBehaviour);
    }

    private void onFilterChanged(ItemStack newFilter) {
        if (level == null || level.isClientSide) {
            return;
        }

        GasPropagator.propagateChangedPipe(level, worldPosition, getBlockState());
    }

    @Override
    public boolean canExtract(Level level, BlockState blockState, BlockPos blockPos, Direction direction) {
        return true;
    }

    public class SmartPipeTransportBehaviour extends GasTransportBehaviour {
        public SmartPipeTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(@NotNull BlockState state, @NotNull Direction direction) {
            if (state.getValue(SmartAirtightPipeBlock.AXIS) != direction.getAxis()) {
                return false;
            }

            BlockPos otherPos = worldPosition.relative(direction);
            BlockState otherState = getWorld().getBlockState(otherPos);
            return isValidAirtightComponents(otherPos, otherState, direction);
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction) {
            if (isIncorrectAxis(state, direction)) {
                return AttachmentTypes.NONE;
            }

            BlockState otherState = level.getBlockState(pos.relative(direction));
            Block otherBlock = otherState.getBlock();
            Axis axis = state.getValue(AirtightCheckValveBlock.AXIS);
            return otherBlock instanceof IAxisPipe axisPipe && axisPipe.getAxis(otherState) == axis ? AttachmentTypes.NONE : AttachmentTypes.RIM.withoutConnector();
        }

        @Override
        public boolean canPullGasFrom(@NotNull GasStack gasStack, BlockState state, Direction direction) {
            return (gasStack.isEmpty() || filter != null && filter.test(gasStack)) && super.canPullGasFrom(gasStack, state, direction);
        }
    }

    class SmartPipeFilterSlot extends ValueBoxTransform {
        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, @NotNull BlockState state) {
            Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
            if (axis == Axis.Y) {
                DirectionalFacing facing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
                return switch (facing) {
                    case SOUTH -> VecHelper.voxelSpace(8, 8, 1.5f);
                    case WEST -> VecHelper.voxelSpace(14.5f, 8, 8);
                    case EAST -> VecHelper.voxelSpace(1.5f, 8, 8);
                    default -> VecHelper.voxelSpace(8, 8, 14.5f);
                };
            }
            return VecHelper.rotateCentered(VecHelper.voxelSpace(8, 14.5f, 8), 90, Axis.Y);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, @NotNull BlockState state, PoseStack ms) {
            Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
            DirectionalFacing facing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
            TransformStack<PoseTransformStack> transformStack = TransformStack.of(ms);
            if (axis == Axis.Y) {
                transformStack.rotateYDegrees(DirectionalFacing.getYAngle(facing));
            }
            else if (axis == Axis.Z) {
                transformStack.rotateYDegrees(DirectionalFacing.getYAngle(facing)).rotateXDegrees(90);
            }
            else {
                transformStack.rotateYDegrees(DirectionalFacing.getYAngle(facing) + 90).rotateXDegrees(90);
            }
        }

        @Override
        public float getScale() {
            return super.getScale() * 1.02f;
        }
    }
}
