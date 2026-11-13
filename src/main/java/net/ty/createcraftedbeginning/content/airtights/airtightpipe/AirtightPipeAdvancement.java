package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightPipeAdvancement {
    private AirtightPipeAdvancement() {
    }

    static void awardCasing(Level level, BlockPos pos, @Nullable LivingEntity actor) {
        if (level.isClientSide || !(actor instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AirtightPipeBlock) || !state.getValue(AirtightPipeBlock.CASED)) {
            return;
        }

        CCBAdvancements.BRONCHI.awardTo(player);
    }
}
