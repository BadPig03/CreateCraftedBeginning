package net.ty.createcraftedbeginning.client.blockextensions;

import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.ty.createcraftedbeginning.client.CCBParticleUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.IAirtightForgingPressStructural;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class AirtightForgingPressStructuralClientExtensions implements IClientBlockExtensions, MultiPosDestructionHandler {
    @Override
    public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
        if (!(target instanceof BlockHitResult result)) {
            return false;
        }
        BlockPos targetPos = result.getBlockPos();
        BlockState targetState = level.getBlockState(targetPos);
        return targetState.getBlock() instanceof IAirtightForgingPressStructural structural && !structural.stillValid(level, targetPos, state);
    }

    @Override
    public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
        CCBParticleUtils.addReducedDestroyEffects(state, level, pos, manager);
        return true;
    }

    @Override
    public @Nullable Set<BlockPos> getExtraPositions(ClientLevel level, BlockPos pos, BlockState blockState, int progress) {
        BlockState currentState = level.getBlockState(pos);
        if (currentState.getBlock() instanceof IAirtightForgingPressStructural structural && !structural.stillValid(level, pos, blockState)) {
            return null;
        }
        BlockPos masterPos = AirtightForgingPressUtils.getMaster(pos, blockState);
        HashSet<BlockPos> positions = new HashSet<>();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    if (i == 0 && j == 0 && k == 0) {
                        continue;
                    }

                    positions.add(masterPos.offset(i, j, k));
                }
            }
        }
        positions.add(masterPos);
        return positions;
    }
}
