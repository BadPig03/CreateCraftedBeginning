package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalElement;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserBlockClip;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorAimRay extends ClipContext {
    AmethystMirrorAimRay(Player player, Vec3 start, Vec3 end) {
        super(start, end, Block.OUTLINE, Fluid.NONE, player);
    }

    @Override
    public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getBlock() instanceof OpticalElement element) {
            OpticalGeometry geometry = element.getOpticalGeometry(level, pos, state, 1);
            if (geometry != null) {
                return geometry.getCollisionShape();
            }
        }

        return super.getBlockShape(state, level, pos);
    }

    BlockHitResult clip(Level level) {
        Vec3 start = getFrom();
        Vec3 end = getTo();
        BlockHitResult initial = level.clip(this);
        LongOpenHashSet visited = new LongOpenHashSet();
        BlockHitResult[] nearest = {initial};
        BlockGetter.traverseBlocks(start, initial.getLocation(), level, (world, cell) -> {
            int reach = LaserBlockClip.NEIGHBOR_REACH;
            for (BlockPos pos : BlockPos.betweenClosed(cell.offset(-reach, -reach, -reach), cell.offset(reach, reach, reach))) {
                if (!visited.add(pos.asLong()) || !world.isLoaded(pos)) {
                    continue;
                }

                BlockState state = world.getBlockState(pos);
                if (!(state.getBlock() instanceof OpticalElement)) {
                    continue;
                }

                BlockHitResult hit = getBlockShape(state, world, pos).clip(start, end, pos);
                if (hit == null || start.distanceToSqr(hit.getLocation()) >= start.distanceToSqr(nearest[0].getLocation())) {
                    continue;
                }

                nearest[0] = hit.withPosition(pos.immutable());
            }
            return null;
        }, world -> null);
        return nearest[0];
    }
}
