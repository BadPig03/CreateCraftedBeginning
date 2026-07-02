package net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve;

import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasExtractor;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightCheckValveBlockEntity extends SmartBlockEntity implements IGasExtractor {
    private CCBAdvancementBehaviour advancementBehaviour;

    public AirtightCheckValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.GASEOUS_VARIATIONS, CCBAdvancements.MINTY_FRESH);
        behaviours.add(advancementBehaviour);

        CheckValvePipeTransportBehaviour transportBehaviour = new CheckValvePipeTransportBehaviour(this);
        behaviours.add(transportBehaviour);
    }

    @Override
    public boolean canExtract(Level level, BlockState blockState, BlockPos blockPos, Direction direction) {
        return true;
    }

    @Override
    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    public class CheckValvePipeTransportBehaviour extends GasTransportBehaviour {
        public CheckValvePipeTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            if (state.getValue(AirtightCheckValveBlock.AXIS) != direction.getAxis()) {
                return false;
            }

            BlockPos otherPos = worldPosition.relative(direction);
            Level level = getWorld();
            return isValidAirtightComponents(level, otherPos, level.getBlockState(otherPos), direction);
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
        public boolean canPullGasFrom(GasStack gas, BlockState state, Direction direction) {
            boolean isInverted = state.getValue(AirtightCheckValveBlock.INVERTED);
            AxisDirection axisDirection = direction.getAxisDirection();
            return axisDirection == AxisDirection.POSITIVE && !isInverted || axisDirection == AxisDirection.NEGATIVE && isInverted;
        }
    }
}
