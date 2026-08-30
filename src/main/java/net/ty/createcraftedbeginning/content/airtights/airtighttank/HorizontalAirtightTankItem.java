package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasConnectivityHandler;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HorizontalAirtightTankItem extends AirtightTankItem {
    private static final int INVALID_PLACEMENT = -1;

    public HorizontalAirtightTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Nullable
    private static Axis getHorizontalAxis(BlockState state) {
        if (!state.hasProperty(HorizontalAirtightTankBlock.HORIZONTAL_AXIS)) {
            return null;
        }

        Axis horizontalAxis = state.getValue(HorizontalAirtightTankBlock.HORIZONTAL_AXIS);
        if (!horizontalAxis.isHorizontal()) {
            return null;
        }
        return horizontalAxis;
    }

    private static int coordinate(BlockPos pos, Axis axis) {
        return axis.choose(pos.getX(), pos.getY(), pos.getZ());
    }

    private static BlockPos offsetLayer(BlockPos origin, Axis axis, int uOffset, int vOffset) {
        return switch (axis) {
            case X -> origin.offset(0, uOffset, vOffset);
            case Z -> origin.offset(uOffset, vOffset, 0);
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    void tryMultiPlace(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) {
            return;
        }

        Direction clickedFace = context.getClickedFace();
        Axis clickedAxis = clickedFace.getAxis();
        if (!clickedAxis.isHorizontal()) {
            return;
        }

        ItemStack tankStack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos placementPos = context.getClickedPos();
        BlockPos placedOnPos = placementPos.relative(clickedFace.getOpposite());
        BlockState placedOnState = level.getBlockState(placedOnPos);
        if (placedOnState.getBlock() != getBlock()) {
            return;
        }

        Axis placedOnAxis = getHorizontalAxis(placedOnState);
        if (placedOnAxis == null) {
            return;
        }

        HorizontalAirtightTankBlockEntity placedOnTank = GasConnectivityHandler.partAt(CCBBlockEntities.HORIZONTAL_AIRTIGHT_TANK.get(), level, placedOnPos);
        if (placedOnTank == null) {
            return;
        }

        if (!(placedOnTank.getControllerBE() instanceof HorizontalAirtightTankBlockEntity horizontalController)) {
            return;
        }

        Axis tankAxis = horizontalController.getMainConnectionAxis();
        if (tankAxis != placedOnAxis) {
            return;
        }

        if (clickedAxis != tankAxis) {
            return;
        }

        int width = horizontalController.getWidth();
        if (width == 1 || horizontalController.getHeight() >= horizontalController.getMaxLength(tankAxis, width)) {
            return;
        }

        Direction positiveDirection = Direction.fromAxisAndDirection(tankAxis, AxisDirection.POSITIVE);
        BlockPos controllerPos = horizontalController.getBlockPos();
        BlockPos layerStartPos = clickedFace == positiveDirection.getOpposite() ? controllerPos.relative(positiveDirection.getOpposite()) : controllerPos.relative(positiveDirection, horizontalController.getHeight());
        if (coordinate(layerStartPos, tankAxis) != coordinate(placementPos, tankAxis)) {
            return;
        }

        int tanksToPlace = countTanksToPlace(level, layerStartPos, tankAxis, width);
        if (tanksToPlace == INVALID_PLACEMENT || !player.isCreative() && tankStack.getCount() < tanksToPlace) {
            return;
        }

        placeTankLayer(context, level, layerStartPos, clickedFace, tankAxis, width);
    }

    private int countTanksToPlace(Level level, BlockPos startPos, Axis tankAxis, int width) {
        int tanksToPlace = 0;
        for (int uOffset = 0; uOffset < width; uOffset++) {
            for (int vOffset = 0; vOffset < width; vOffset++) {
                BlockPos targetPos = offsetLayer(startPos, tankAxis, uOffset, vOffset);
                BlockState targetState = level.getBlockState(targetPos);
                if (isCompatibleHorizontalTank(targetState, tankAxis)) {
                    continue;
                }

                if (targetState.getBlock() == getBlock() || !targetState.canBeReplaced()) {
                    return INVALID_PLACEMENT;
                }

                tanksToPlace++;
            }
        }
        return tanksToPlace;
    }

    private void placeTankLayer(BlockPlaceContext context, Level level, BlockPos startPos, Direction face, Axis tankAxis, int width) {
        for (int uOffset = 0; uOffset < width; uOffset++) {
            for (int vOffset = 0; vOffset < width; vOffset++) {
                BlockPos targetPos = offsetLayer(startPos, tankAxis, uOffset, vOffset);
                BlockState targetState = level.getBlockState(targetPos);
                if (isCompatibleHorizontalTank(targetState, tankAxis)) {
                    continue;
                }

                placeSingleBlock(BlockPlaceContext.at(context, targetPos, face));
            }
        }
    }

    private boolean isCompatibleHorizontalTank(BlockState candidateState, Axis tankAxis) {
        if (candidateState.getBlock() != getBlock()) {
            return false;
        }

        Axis candidateAxis = getHorizontalAxis(candidateState);
        return candidateAxis == tankAxis;
    }
}
