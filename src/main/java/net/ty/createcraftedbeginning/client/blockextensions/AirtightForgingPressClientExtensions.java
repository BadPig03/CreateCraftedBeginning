package net.ty.createcraftedbeginning.client.blockextensions;

import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralPosition;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class AirtightForgingPressClientExtensions implements IClientBlockExtensions, MultiPosDestructionHandler {
    @Override
    public Set<BlockPos> getExtraPositions(ClientLevel level, BlockPos pos, BlockState blockState, int progress) {
        HashSet<BlockPos> positions = new HashSet<>();
        for (AirtightForgingPressStructuralPosition position : AirtightForgingPressStructuralPosition.all()) {
            positions.add(pos.offset(position.getStructureOffset()));
        }
        return positions;
    }
}
