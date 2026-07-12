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
import net.ty.createcraftedbeginning.api.gas.gases.GasConnectivityHandler;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HorizontalAirtightTankItem extends AirtightTankItem {
    public HorizontalAirtightTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Nullable
    private static Axis getHorizontalAxis(BlockState state) {
        if (!state.hasProperty(HorizontalAirtightTankBlock.HORIZONTAL_AXIS)) {
            return null;
        }

        Axis axis = state.getValue(HorizontalAirtightTankBlock.HORIZONTAL_AXIS);
        return axis.isHorizontal() ? axis : null;
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
    protected void tryMultiPlace(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) {
            return;
        }

        Direction face = context.getClickedFace();
        Axis faceAxis = face.getAxis();
        if (!faceAxis.isHorizontal()) {
            return;
        }

        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPos placedOnPos = pos.relative(face.getOpposite());
        BlockState placedOnState = level.getBlockState(placedOnPos);
        if (placedOnState.getBlock() != getBlock()) {
            return;
        }

        Axis placedOnAxis = getHorizontalAxis(placedOnState);
        if (placedOnAxis == null) {
            return;
        }

        HorizontalAirtightTankBlockEntity tankAt = GasConnectivityHandler.partAt(CCBBlockEntities.HORIZONTAL_AIRTIGHT_TANK.get(), level, placedOnPos);
        if (tankAt == null) {
            return;
        }

        AirtightTankBlockEntity controller = tankAt.getControllerBE();
        if (!(controller instanceof HorizontalAirtightTankBlockEntity horizontalController)) {
            return;
        }

        Axis tankAxis = horizontalController.getMainConnectionAxis();
        if (tankAxis != placedOnAxis) {
            return;
        }

        if (faceAxis != tankAxis) {
            return;
        }

        int width = horizontalController.getWidth();
        if (width == 1 || horizontalController.getHeight() >= horizontalController.getMaxLength(tankAxis, width)) {
            return;
        }

        Direction positive = Direction.fromAxisAndDirection(tankAxis, AxisDirection.POSITIVE);
        BlockPos startPos = face == positive.getOpposite() ? horizontalController.getBlockPos().relative(positive.getOpposite()) : horizontalController.getBlockPos().relative(positive, horizontalController.getHeight());
        if (coordinate(startPos, tankAxis) != coordinate(pos, tankAxis)) {
            return;
        }

        int tanksToPlace = 0;
        for (int uOffset = 0; uOffset < width; uOffset++) {
            for (int vOffset = 0; vOffset < width; vOffset++) {
                BlockPos offsetPos = offsetLayer(startPos, tankAxis, uOffset, vOffset);
                BlockState blockState = level.getBlockState(offsetPos);
                if (isCompatibleHorizontalTank(blockState, tankAxis)) {
                    continue;
                }

                if (blockState.getBlock() == getBlock() || !blockState.canBeReplaced()) {
                    return;
                }

                tanksToPlace++;
            }
        }

        if (!player.isCreative() && stack.getCount() < tanksToPlace) {
            return;
        }

        for (int uOffset = 0; uOffset < width; uOffset++) {
            for (int vOffset = 0; vOffset < width; vOffset++) {
                BlockPos offsetPos = offsetLayer(startPos, tankAxis, uOffset, vOffset);
                BlockState blockState = level.getBlockState(offsetPos);
                if (isCompatibleHorizontalTank(blockState, tankAxis)) {
                    continue;
                }

                placeSingleBlock(BlockPlaceContext.at(context, offsetPos, face));
            }
        }
    }

    private boolean isCompatibleHorizontalTank(BlockState state, Axis axis) {
        if (state.getBlock() != getBlock()) {
            return false;
        }

        Axis stateAxis = getHorizontalAxis(state);
        return stateAxis == axis;
    }
}
