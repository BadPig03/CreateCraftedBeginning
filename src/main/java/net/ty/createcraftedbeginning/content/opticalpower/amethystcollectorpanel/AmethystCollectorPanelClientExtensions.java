package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.ty.createcraftedbeginning.client.particle.CCBParticles;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class AmethystCollectorPanelClientExtensions implements IClientBlockExtensions {
    @Override
    public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
        CCBParticles.addReducedDestroyEffects(state, level, pos, manager);
        return true;
    }
}
