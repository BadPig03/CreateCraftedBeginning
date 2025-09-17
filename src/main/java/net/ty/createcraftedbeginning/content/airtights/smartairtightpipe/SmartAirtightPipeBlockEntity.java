package net.ty.createcraftedbeginning.content.airtights.smartairtightpipe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.GasFilteringBehaviour;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.checkvalve.CheckValveBlock;
import net.ty.createcraftedbeginning.api.gas.AxisGasPipeBlockEntity;
import net.ty.createcraftedbeginning.api.gas.GasPropagator;
import net.ty.createcraftedbeginning.api.gas.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.interfaces.IDirectionalPipe;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SmartAirtightPipeBlockEntity extends AxisGasPipeBlockEntity {
    private GasFilteringBehaviour filter;

    public SmartAirtightPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        filter = new GasFilteringBehaviour(this, new SmartPipeFilterSlot()).withCallback(this::onFilterChanged);
        behaviours.add(new SmartPipeTransportBehaviour(this));
        behaviours.add(filter);
    }

    private void onFilterChanged(ItemStack newFilter) {
        if (level == null || level.isClientSide) {
            return;
        }
        GasPropagator.propagateChangedPipe(level, worldPosition, getBlockState());
    }

    public class SmartPipeTransportBehaviour extends GasTransportBehaviour {
        public SmartPipeTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canPullGasFrom(@NotNull GasStack gas, BlockState state, Direction direction) {
            if (gas.isEmpty() || (filter != null && filter.test(gas))) {
                return super.canPullGasFrom(gas, state, direction);
            }
            return false;
        }

        @Override
        public boolean canHaveFlowToward(@NotNull BlockState state, Direction direction) {
            return state.hasProperty(SmartAirtightPipeBlock.AXIS) && state.getValue(SmartAirtightPipeBlock.AXIS) == direction.getAxis();
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
            if (!canHaveFlowToward(state, direction)) {
                return AttachmentTypes.NONE;
            }

            BlockState otherState = getWorld().getBlockState(getPos().relative(direction));
            Block otherBlock = otherState.getBlock();
            Direction.Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);

            return switch (otherBlock) {
                case AirtightPipeBlock ignored when otherState.getValue(AirtightPipeBlock.AXIS) == axis -> AttachmentTypes.NONE;
                case CheckValveBlock ignored when otherState.getValue(CheckValveBlock.AXIS) == axis -> AttachmentTypes.NONE;
                case SmartAirtightPipeBlock ignored when otherState.getValue(SmartAirtightPipeBlock.AXIS) == axis -> AttachmentTypes.NONE;
                default -> AttachmentTypes.RIM.withoutConnector();
            };
        }
    }

    class SmartPipeFilterSlot extends ValueBoxTransform {
        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, @NotNull BlockState state) {
            Direction.Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
            if (axis == Direction.Axis.Y) {
                IDirectionalPipe.DirectionalFacing facing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);

                return switch (facing) {
                    case SOUTH -> VecHelper.voxelSpace(8, 8, 1.5f);
                    case WEST -> VecHelper.voxelSpace(14.5f, 8, 8);
                    case EAST -> VecHelper.voxelSpace(1.5f, 8, 8);
                    default -> VecHelper.voxelSpace(8, 8, 14.5f);
                };
            }
            return VecHelper.rotateCentered(VecHelper.voxelSpace(8, 14.5f, 8), 90, Direction.Axis.Y);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, @NotNull BlockState state, PoseStack ms) {
            Direction.Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
            TransformStack<PoseTransformStack> transformStack = TransformStack.of(ms);
            if (axis == Direction.Axis.Y) {
                IDirectionalPipe.DirectionalFacing facing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
                transformStack.rotateYDegrees(IDirectionalPipe.DirectionalFacing.getYAngle(facing));
            } else if (axis == Direction.Axis.Z) {
                transformStack.rotateYDegrees(0).rotateXDegrees(90);
            } else {
                transformStack.rotateYDegrees(90).rotateXDegrees(90);
            }
        }

        @Override
        public float getScale() {
            return super.getScale() * 1.02f;
        }
    }
}
