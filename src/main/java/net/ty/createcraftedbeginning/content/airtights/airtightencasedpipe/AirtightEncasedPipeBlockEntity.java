package net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTransporter;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEncasedPipeBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IGasTransporter {
    private CCBAdvancementBehaviour advancementBehaviour;

    public AirtightEncasedPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Level level = getLevel();
        if (level == null) {
            return false;
        }

        List<Direction> invalidDirections = getInvalidDirections(level);
        if (invalidDirections.isEmpty()) {
            return false;
        }

        CCBLang.translate("gui.airtight_encased_pipe").forGoggles(tooltip);
        CCBLang.translate("gui.airtight_encased_pipe.warning").style(ChatFormatting.GRAY).forGoggles(tooltip);
        for (Direction direction : invalidDirections) {
            CCBLang.translate("gui.airtight_handheld_drill.direction." + direction.getName()).style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        }
        return true;
    }

    private List<Direction> getInvalidDirections(Level level) {
        List<Direction> invalidDirections = new ArrayList<>();
        BlockState state = getBlockState();
        for (Direction direction : Iterate.directions) {
            if (state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                continue;
            }

            if (!AirtightEncasedPipeBlock.hasPlacementConnection(level, worldPosition, direction)) {
                continue;
            }

            invalidDirections.add(direction);
        }
        return invalidDirections;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.GASEOUS_VARIATIONS, CCBAdvancements.MINTY_FRESH);
        behaviours.add(advancementBehaviour);
        behaviours.add(new AirtightEncasedPipeTransportBehaviour(this));
    }

    @Override
    public boolean canTransport(Level level, BlockState blockState, BlockPos blockPos, Direction direction) {
        return blockState.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction));
    }

    @Override
    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    public class AirtightEncasedPipeTransportBehaviour extends GasTransportBehaviour {
        public AirtightEncasedPipeTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            BlockPos otherPos = worldPosition.relative(direction);
            Level level = getWorld();
            BlockState otherState = level.getBlockState(otherPos);
            return isValidAirtightComponents(level, otherPos, otherState, direction) && state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction));
        }
    }
}
