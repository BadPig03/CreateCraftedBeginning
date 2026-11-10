package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightPipeTooltip {
    private AirtightPipeTooltip() {
    }

    static boolean addTo(List<Component> tooltip, Level level, BlockPos pos, BlockState state) {
        List<Direction> invalidDirections = getInvalidDirections(level, pos, state);
        if (invalidDirections.isEmpty()) {
            return false;
        }

        CCBLang.translate("gui.airtight_pipe").style(ChatFormatting.GOLD).forGoggles(tooltip);
        CCBLang.addToGoggles(tooltip, "gui.airtight_pipe.warning.closed_ports");
        for (Direction direction : invalidDirections) {
            CCBLang.translate("gui.airtight_handheld_drill.direction." + direction.getName()).style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        }
        return true;
    }

    private static List<Direction> getInvalidDirections(Level level, BlockPos pos, BlockState state) {
        List<Direction> invalidDirections = new ArrayList<>();
        for (Direction direction : Iterate.directions) {
            if (state.getValue(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                continue;
            }

            BlockPos neighbor = pos.relative(direction);
            if (!level.isLoaded(neighbor)) {
                continue;
            }

            BlockState adjacent = level.getBlockState(neighbor);
            Direction opposite = direction.getOpposite();
            boolean connectable = adjacent.getBlock() instanceof GasConnectable component && component.canConnectOnFace(neighbor, adjacent, opposite) || GasCapabilities.hasBlockHandler(level, neighbor, opposite);
            if (!connectable) {
                continue;
            }

            invalidDirections.add(direction);
        }
        return invalidDirections;
    }
}
