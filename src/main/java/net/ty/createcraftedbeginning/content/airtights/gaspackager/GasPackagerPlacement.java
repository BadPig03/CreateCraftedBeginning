package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlock;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPackagerPlacement {
    private GasPackagerPlacement() {
    }

    @Nullable
    public static BlockState withGasFacing(BlockPlaceContext context, @Nullable BlockState baseState) {
        if (baseState == null) {
            return null;
        }

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();
        boolean isSneaking = player != null && player.isShiftKeyDown();
        Direction preferredFacing = isSneaking ? null : findConnectedGasDirection(context, level, clickedPos);
        if (preferredFacing == null) {
            Direction lookDirection = context.getNearestLookingDirection();
            preferredFacing = isSneaking ? lookDirection : lookDirection.getOpposite();
        }

        BlockPos targetPos = clickedPos.relative(preferredFacing.getOpposite());
        if (player != null && !(player instanceof FakePlayer) && level.getBlockState(targetPos).getBlock() instanceof PortableGasInterfaceBlock) {
            CCBLang.translate("gui.warnings.no_gas_portable_interface").sendStatus(player);
            return null;
        }

        return baseState.setValue(BlockStateProperties.FACING, preferredFacing);
    }

    @Nullable
    private static Direction findConnectedGasDirection(BlockPlaceContext context, Level level, BlockPos clickedPos) {
        for (Direction direction : context.getNearestLookingDirections()) {
            BlockPos targetPos = clickedPos.relative(direction);
            BlockEntity targetBlockEntity = level.getBlockEntity(targetPos);
            if (targetBlockEntity instanceof GasPackagerBlockEntity) {
                continue;
            }

            Direction targetSide = direction.getOpposite();
            if (targetBlockEntity == null || level.getCapability(GasCapabilities.BLOCK, targetPos, targetSide) == null) {
                continue;
            }

            return targetSide;
        }
        return null;
    }
}
