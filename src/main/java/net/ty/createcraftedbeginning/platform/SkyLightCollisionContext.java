package net.ty.createcraftedbeginning.platform;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SkyLightCollisionContext extends EntityCollisionContext {
    public static final SkyLightCollisionContext INSTANCE = new SkyLightCollisionContext(null);
    private static final int MIN_CACHED_SAMPLE_COUNT = 6;
    private final @Nullable Query query;

    private SkyLightCollisionContext(@Nullable Query query) {
        super(false, -Double.MAX_VALUE, ItemStack.EMPTY, fluid -> false, null);
        this.query = query;
    }

    public static SkyLightCollisionContext createQuery(BlockGetter level) {
        return new SkyLightCollisionContext(new Query(level));
    }

    public static SkyLightCollisionContext createQuery(BlockGetter level, int sampleCount) {
        if (sampleCount < MIN_CACHED_SAMPLE_COUNT) {
            return INSTANCE;
        }

        return createQuery(level);
    }

    @Override
    public boolean isAbove(VoxelShape shape, BlockPos pos, boolean canAscend) {
        return canAscend;
    }

    public BlockHitResult clip(BlockGetter level, ClipContext context) {
        Query cache = isQueryFor(level) ? query : null;
        return BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (clip, pos) -> {
            BlockState state;
            if (cache == null) {
                state = level.getBlockState(pos);
            }
            else {
                state = cache.states.get(pos);
                if (state == null) {
                    state = level.getBlockState(pos);
                    cache.states.put(pos.immutable(), state);
                }
            }
            boolean canOcclude = state.canOcclude();
            if (!canOcclude && !state.is(CCBBlockTags.AMETHYST_COLLECTOR_SKY_OCCLUDERS.tag) && state.getLightBlock(level, pos) == 0) {
                return null;
            }

            Vec3 start = clip.getFrom();
            Vec3 end = clip.getTo();
            VoxelShape shape = cache == null ? null : cache.shapes.get(pos);
            if (shape == null) {
                if (canOcclude) {
                    shape = state.getOcclusionShape(level, pos);
                }
                else {
                    shape = state.getShape(level, pos, this);
                }
                if (cache != null) {
                    cache.shapes.put(pos.immutable(), shape);
                }
            }
            VoxelShape fluidShape = cache == null ? null : cache.fluidShapes.get(pos);
            if (fluidShape == null) {
                fluidShape = state.getFluidState().getShape(level, pos);
                if (cache != null) {
                    cache.fluidShapes.put(pos.immutable(), fluidShape);
                }
            }
            BlockHitResult blockHit = shape.clip(start, end, pos);
            BlockHitResult fluidHit = fluidShape.clip(start, end, pos);
            if (blockHit == null) {
                return fluidHit;
            }

            if (fluidHit != null && fluidHit.getLocation().distanceToSqr(start) < blockHit.getLocation().distanceToSqr(start)) {
                return fluidHit;
            }

            return blockHit;
        }, clip -> {
            Vec3 end = clip.getTo();
            Vec3 delta = clip.getFrom().subtract(end);
            return BlockHitResult.miss(end, Direction.getNearest(delta.x, delta.y, delta.z), BlockPos.containing(end));
        });
    }

    public boolean isQueryFor(BlockGetter level) {
        return query != null && query.level == level;
    }

    public void cacheShape(BlockGetter level, BlockPos pos, VoxelShape shape) {
        Query cache = query;
        if (cache == null || cache.level != level) {
            return;
        }

        cache.shapes.put(pos.immutable(), shape);
    }

    private static final class Query {
        private final BlockGetter level;
        private final Map<BlockPos, BlockState> states = new HashMap<>();
        private final Map<BlockPos, VoxelShape> shapes = new HashMap<>();
        private final Map<BlockPos, VoxelShape> fluidShapes = new HashMap<>();

        private Query(BlockGetter level) {
            this.level = level;
        }
    }
}
