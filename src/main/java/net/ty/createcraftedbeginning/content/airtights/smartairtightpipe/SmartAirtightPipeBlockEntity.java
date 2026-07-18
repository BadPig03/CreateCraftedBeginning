package net.ty.createcraftedbeginning.content.airtights.smartairtightpipe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.GasPropagator;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasFilteringBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IDirectionalPipe;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IDirectionalPipe.DirectionalFacing;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTransporter;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasTransportBehaviour;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SmartAirtightPipeBlockEntity extends SmartBlockEntity implements IGasTransporter {
    private GasFilteringBehaviour filter;
    private CCBAdvancementBehaviour advancementBehaviour;

    public SmartAirtightPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        filter = new GasFilteringBehaviour(this, new SmartPipeFilterSlot()).withCallback(this::onFilterChanged);
        behaviours.add(filter);

        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.GASEOUS_VARIATIONS, CCBAdvancements.MINTY_FRESH);
        behaviours.add(advancementBehaviour);

        SmartPipeTransportBehaviour transport = new SmartPipeTransportBehaviour(this);
        behaviours.add(transport);
    }

    @Override
    public boolean canTransport(Level level, BlockState blockState, BlockPos blockPos, Direction direction) {
        return true;
    }

    @Override
    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    public GasFilteringBehaviour getFilter() {
        return filter;
    }

    private void onFilterChanged(ItemStack newFilter) {
        if (level == null || level.isClientSide) {
            return;
        }

        GasPropagator.propagatePipe(level, worldPosition, getBlockState());
    }

    public class SmartPipeTransportBehaviour extends AxisGasTransportBehaviour {
        public SmartPipeTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canPullGasFrom(GasStack gasStack, BlockState state, Direction direction) {
            return (gasStack.isEmpty() || filter != null && filter.test(gasStack)) && super.canPullGasFrom(gasStack, state, direction);
        }
    }

    class SmartPipeFilterSlot extends ValueBoxTransform {
        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
            if (axis != Axis.Y) {
                return VecHelper.rotateCentered(VecHelper.voxelSpace(8, 14.5f, 8), 90, Axis.Y);
            }

            DirectionalFacing facing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
            return switch (facing) {
                case SOUTH -> VecHelper.voxelSpace(8, 8, 1.5f);
                case WEST -> VecHelper.voxelSpace(14.5f, 8, 8);
                case EAST -> VecHelper.voxelSpace(1.5f, 8, 8);
                default -> VecHelper.voxelSpace(8, 8, 14.5f);
            };
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
            DirectionalFacing facing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
            TransformStack<PoseTransformStack> transform = TransformStack.of(ms);
            int yAngle = DirectionalFacing.getYAngle(facing);

            switch (axis) {
                case Y -> transform.rotateYDegrees(yAngle);
                case Z -> transform.rotateYDegrees(yAngle).rotateXDegrees(90);
                case X -> transform.rotateYDegrees(yAngle + 90).rotateXDegrees(90);
            }
        }

        @Override
        public float getScale() {
            return super.getScale() * 1.02f;
        }
    }
}
