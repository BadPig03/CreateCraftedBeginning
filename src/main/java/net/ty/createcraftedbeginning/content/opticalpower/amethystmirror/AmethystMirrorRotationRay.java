package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystMirrorRotationRay extends EntityCollisionContext {
    private boolean unloaded;

    private AmethystMirrorRotationRay(Player player) {
        super(player.isDescending(), player.getY(), player.getMainHandItem(), player::canStandOnFluid, player);
    }

    static @Nullable BlockHitResult clip(Level level, Player player, Vec3 end) {
        AmethystMirrorRotationRay ray = new AmethystMirrorRotationRay(player);
        BlockHitResult hit = level.clip(new ClipContext(player.getEyePosition(), end, Block.OUTLINE, Fluid.NONE, ray));
        if (ray.unloaded) {
            return null;
        }

        return hit;
    }

    public @Nullable BlockHitResult stopAtUnloadedCell(BlockGetter world, ClipContext context, BlockPos pos) {
        if (!(world instanceof Level level) || level.isOutsideBuildHeight(pos) || level.isLoaded(pos)) {
            return null;
        }

        unloaded = true;
        Vec3 end = context.getTo();
        Vec3 delta = context.getFrom().subtract(end);
        return BlockHitResult.miss(end, Direction.getNearest(delta.x, delta.y, delta.z), BlockPos.containing(end));
    }
}
