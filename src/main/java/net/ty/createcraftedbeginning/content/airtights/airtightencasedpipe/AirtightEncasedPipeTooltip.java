package net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe;

import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightEncasedPipeTooltip {
    private AirtightEncasedPipeTooltip() {
    }

    static boolean addTo(List<Component> tooltip, Level level, BlockPos pos, BlockState state) {
        List<Direction> invalidDirections = getInvalidDirections(level, pos, state);
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

    private static List<Direction> getInvalidDirections(Level level, BlockPos pos, BlockState state) {
        List<Direction> invalidDirections = new ArrayList<>();
        for (Direction direction : Iterate.directions) {
            if (state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                continue;
            }
            if (AirtightEncasedPipeBlock.hasPlacementConnection(level, pos, direction)) {
                invalidDirections.add(direction);
            }
        }
        return invalidDirections;
    }
}
