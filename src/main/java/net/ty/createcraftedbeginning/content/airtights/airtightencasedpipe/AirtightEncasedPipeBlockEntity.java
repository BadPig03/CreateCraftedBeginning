package net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.IGasExtractor;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AirtightEncasedPipeBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IGasExtractor {
    public AirtightEncasedPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Level level = getLevel();
        if (level == null) {
            return false;
        }

        List<Direction> invalidDirections = new ArrayList<>();
        for (Direction direction : Iterate.directions) {
            BlockPos otherPos = worldPosition.relative(direction);
            if (level.getBlockState(otherPos).isAir()) {
                continue;
            }

            if (!GasTransportBehaviour.isValidAirtightComponents(level, otherPos, level.getBlockState(otherPos), direction) || getBlockState().getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                continue;
            }

            invalidDirections.add(direction);
        }

        if (invalidDirections.isEmpty()) {
            return false;
        }

        CCBLang.translate("gui.goggles.airtight_encased_pipe").forGoggles(tooltip);
        CCBLang.translate("gui.goggles.airtight_encased_pipe.warning").style(ChatFormatting.GRAY).forGoggles(tooltip);
        for (Direction direction : invalidDirections) {
            CCBLang.translate("gui.airtight_handheld_drill.direction." + direction.getName()).style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        }
        return true;
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        AirtightEncasedPipeTransportBehaviour transportBehaviour = new AirtightEncasedPipeTransportBehaviour(this);
        behaviours.add(transportBehaviour);
    }

    @Override
    public boolean canExtract(Level level, @NotNull BlockState blockState, BlockPos blockPos, Direction direction) {
        return blockState.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction));
    }

    public class AirtightEncasedPipeTransportBehaviour extends GasTransportBehaviour {
        public AirtightEncasedPipeTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            BlockPos otherPos = worldPosition.relative(direction);
            Level level = getWorld();
            return isValidAirtightComponents(level, otherPos, level.getBlockState(otherPos), direction) && state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction));
        }
    }
}
