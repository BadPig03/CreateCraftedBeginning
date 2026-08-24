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
import net.ty.createcraftedbeginning.client.CCBParticles;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.IAirtightReactorKettleStructural;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class AirtightReactorKettleStructuralClientExtensions implements IClientBlockExtensions, MultiPosDestructionHandler {
    @Override
    public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
        if (!(target instanceof BlockHitResult hitResult)) {
            return false;
        }

        BlockPos targetPos = hitResult.getBlockPos();
        return level.getBlockState(targetPos).getBlock() instanceof IAirtightReactorKettleStructural structural && !structural.stillValid(level, targetPos, state);
    }

    @Override
    public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
        CCBParticles.addReducedDestroyEffects(state, level, pos, manager);
        return true;
    }

    @Override
    public @Nullable Set<BlockPos> getExtraPositions(ClientLevel level, BlockPos pos, BlockState blockState, int progress) {
        if (level.getBlockState(pos).getBlock() instanceof IAirtightReactorKettleStructural structural && !structural.stillValid(level, pos, blockState)) {
            return null;
        }

        BlockPos masterPos = AirtightReactorKettleUtils.getMaster(pos, blockState);
        HashSet<BlockPos> positions = new HashSet<>();
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int yOffset = -1; yOffset <= 1; yOffset++) {
                for (int zOffset = -1; zOffset <= 1; zOffset++) {
                    if (xOffset == 0 && yOffset == 0 && zOffset == 0) {
                        continue;
                    }

                    positions.add(masterPos.offset(xOffset, yOffset, zOffset));
                }
            }
        }
        positions.add(masterPos);
        return positions;
    }
}
