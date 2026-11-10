package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightPipeConnections {
    private AirtightPipeConnections() {
    }

    public static boolean isOpen(BlockState state, Direction face) {
        return state.getValue(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(face));
    }

    public static BlockState straight(BlockState state, Axis axis) {
        for (Direction face : Iterate.directions) {
            state = state.setValue(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(face), face.getAxis() == axis);
        }
        return state;
    }

    @Nullable
    public static Axis straightAxis(BlockState state) {
        Direction first = null;
        int count = 0;
        for (Direction face : Iterate.directions) {
            if (!isOpen(state, face)) {
                continue;
            }

            count++;
            if (first == null) {
                first = face;
                continue;
            }

            if (face != first.getOpposite()) {
                return null;
            }
        }
        if (count != 2) {
            return null;
        }

        return first.getAxis();
    }

    public static BlockState update(BlockState state, LevelAccessor accessor, BlockPos pos, Axis preferredAxis) {
        if (!(accessor instanceof Level level) || state.getValue(AirtightPipeBlock.CASED)) {
            return state;
        }

        for (Direction face : Iterate.directions) {
            if (!level.isLoaded(pos.relative(face))) {
                return state;
            }
        }

        BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(level, pos, BracketedBlockEntityBehaviour.TYPE);
        if (bracket != null && bracket.isBracketPresent()) {
            return state;
        }

        EnumSet<Direction> candidates = EnumSet.noneOf(Direction.class);
        int oldOpenCount = 0;
        for (Direction face : Iterate.directions) {
            if (isOpen(state, face)) {
                oldOpenCount++;
            }

            BlockPos adjacentPos = pos.relative(face);
            BlockState adjacent = level.getBlockState(adjacentPos);
            Direction opposite = face.getOpposite();
            Block adjacentBlock = adjacent.getBlock();
            if (adjacentBlock instanceof AirtightPipeBlock) {
                BracketedBlockEntityBehaviour adjacentBracket = BlockEntityBehaviour.get(level, adjacentPos, BracketedBlockEntityBehaviour.TYPE);
                boolean fixed = adjacent.getValue(AirtightPipeBlock.CASED) || adjacentBracket != null && adjacentBracket.isBracketPresent();
                if (!fixed || isOpen(adjacent, opposite)) {
                    candidates.add(face);
                }
                continue;
            }

            if (!(adjacentBlock instanceof GasConnectable connectable && connectable.canConnectOnFace(adjacentPos, adjacent, opposite) || GasCapabilities.hasBlockHandler(level, adjacentPos, opposite))) {
                continue;
            }

            candidates.add(face);
        }
        if (candidates.isEmpty()) {
            if (oldOpenCount == 2) {
                return state;
            }

            return straight(state, preferredAxis);
        }

        if (candidates.size() == 1) {
            candidates.add(candidates.iterator().next().getOpposite());
        }
        for (Direction face : Iterate.directions) {
            state = state.setValue(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(face), candidates.contains(face));
        }
        return state;
    }
}
