package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver.AdjacentConnection;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightPipeAdvancement {
    private AirtightPipeAdvancement() {
    }

    public static void checkPlayerAction(Level level, BlockPos pos, @Nullable LivingEntity actor) {
        if (level.isClientSide || !(actor instanceof ServerPlayer player) || player instanceof FakePlayer || CCBAdvancements.BRONCHI.isAlreadyAwardedTo(player)) {
            return;
        }

        if (hasBranch(level, pos)) {
            CCBAdvancements.BRONCHI.awardTo(player);
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AirtightPipeBlock)) {
            return;
        }

        for (Direction face : Iterate.directions) {
            if (AirtightPipeConnections.isOpen(state, face) && GasConnectionResolver.resolveAdjacentConnection(level, pos, face).hasTransportConnection() && hasBranch(level, pos.relative(face))) {
                CCBAdvancements.BRONCHI.awardTo(player);
                return;
            }
        }
    }

    public static boolean hasBranch(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AirtightPipeBlock)) {
            return false;
        }

        int connections = 0;
        for (Direction face : Iterate.directions) {
            if (!AirtightPipeConnections.isOpen(state, face)) {
                continue;
            }

            AdjacentConnection adjacent = GasConnectionResolver.resolveAdjacentConnection(level, pos, face);
            if (!adjacent.hasTransportConnection() && !adjacent.hasGasHandler()) {
                continue;
            }

            connections++;
        }
        return connections >= 3;
    }
}
