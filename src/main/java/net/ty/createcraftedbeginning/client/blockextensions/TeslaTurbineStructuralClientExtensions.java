package net.ty.createcraftedbeginning.client.blockextensions;

import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.ty.createcraftedbeginning.client.CCBParticleUtils;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineUtils;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class TeslaTurbineStructuralClientExtensions implements IClientBlockExtensions, MultiPosDestructionHandler {
    @Override
    public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
        if (!(target instanceof BlockHitResult result)) {
            return IClientBlockExtensions.super.addHitEffects(state, level, target, manager);
        }

        BlockPos targetPos = result.getBlockPos();
        TeslaTurbineStructuralBlock block = CCBBlocks.TESLA_TURBINE_STRUCTURAL_BLOCK.get();
        if (!block.stillValid(level, targetPos, state, false)) {
            return true;
        }

        manager.crack(TeslaTurbineStructuralBlock.getMaster(targetPos, state), result.getDirection());
        return true;
    }

    @Override
    public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
        CCBParticleUtils.addReducedDestroyEffects(state, level, pos, manager);
        return true;
    }

    @Override
    public @Nullable Set<BlockPos> getExtraPositions(ClientLevel level, BlockPos pos, BlockState blockState, int progress) {
        TeslaTurbineStructuralBlock block = CCBBlocks.TESLA_TURBINE_STRUCTURAL_BLOCK.get();
        if (!block.stillValid(level, pos, blockState, false)) {
            return null;
        }

        BlockPos masterPos = TeslaTurbineStructuralBlock.getMaster(pos, blockState);
        Axis axis = blockState.getValue(BlockStateProperties.AXIS);
        Set<BlockPos> positions = new HashSet<>();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }

                positions.add(TeslaTurbineUtils.calculateStructurePos(masterPos, axis, i, j));
            }
        }
        positions.add(masterPos);
        return positions;
    }
}
