package net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasTransporter;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightCheckValveBlockEntity extends SmartBlockEntity implements IGasTransporter {
    private CCBAdvancementBehaviour advancementBehaviour;

    public AirtightCheckValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.GASEOUS_VARIATIONS, CCBAdvancements.MINTY_FRESH);
        behaviours.add(advancementBehaviour);

        behaviours.add(new CheckValvePipeTransportBehaviour(this));
    }

    @Override
    public boolean canTransport(Level level, BlockState state, BlockPos pos, Direction direction) {
        return AirtightCheckValveBlock.isInputSide(state, direction);
    }

    @Override
    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    public class CheckValvePipeTransportBehaviour extends AxisGasTransportBehaviour {
        public CheckValvePipeTransportBehaviour(SmartBlockEntity blockEntity) {
            super(blockEntity);
        }

        @Override
        public boolean allowsInboundFlow(BlockState state, Direction direction) {
            return canHaveFlowToward(state, direction) && AirtightCheckValveBlock.isInputSide(state, direction);
        }

        @Override
        public boolean allowsOutboundFlow(BlockState state, Direction direction) {
            return canHaveFlowToward(state, direction) && AirtightCheckValveBlock.isOutputSide(state, direction);
        }
    }
}
