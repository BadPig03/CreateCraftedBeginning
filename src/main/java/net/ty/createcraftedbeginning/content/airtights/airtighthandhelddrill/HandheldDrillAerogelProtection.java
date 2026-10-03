package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.HandheldDrillAerogelProtectionButton;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class HandheldDrillAerogelProtection {
    private HandheldDrillAerogelProtection() {
    }

    static void apply(ServerPlayer player, ItemStack drill, Set<BlockPos> miningPositions) {
        if (!HandheldDrillAerogelProtectionButton.INSTANCE.canApply(drill)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        Set<BlockPos> boundaryPositions = new LinkedHashSet<>();
        for (BlockPos miningPos : miningPositions) {
            for (Direction direction : Iterate.directions) {
                BlockPos boundaryPos = miningPos.relative(direction);
                if (miningPositions.contains(boundaryPos)) {
                    continue;
                }

                boundaryPositions.add(boundaryPos);
            }
        }

        FilterItemStack filter = AirtightHandheldDrillMiningContext.getFilter(drill);
        Map<BlockState, Boolean> filterMatches = new HashMap<>();
        BlockState aerogel = CCBBlocks.AEROGEL_BLOCK.getDefaultState();
        for (BlockPos boundaryPos : boundaryPositions) {
            if (!level.isLoaded(boundaryPos) || !level.getWorldBorder().isWithinBounds(boundaryPos) || !level.mayInteract(player, boundaryPos) || player.blockActionRestricted(level, boundaryPos, player.gameMode.getGameModeForPlayer())) {
                continue;
            }

            BlockState originalState = level.getBlockState(boundaryPos);
            if (!(originalState.getBlock() instanceof LiquidBlock) || originalState.getDestroySpeed(level, boundaryPos) < 0 || AirtightHandheldDrillMiningContext.isProtected(level, boundaryPos, originalState, filter, filterMatches, false)) {
                continue;
            }

            BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, boundaryPos);
            if (!level.setBlock(boundaryPos, aerogel, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
                continue;
            }

            boolean accepted = false;
            try {
                accepted = !EventHooks.onBlockPlace(player, snapshot, Direction.UP);
            }
            finally {
                if (!accepted) {
                    snapshot.restore(Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }

            if (!accepted) {
                continue;
            }

            level.updateNeighborsAt(boundaryPos, aerogel.getBlock());
        }
    }
}
