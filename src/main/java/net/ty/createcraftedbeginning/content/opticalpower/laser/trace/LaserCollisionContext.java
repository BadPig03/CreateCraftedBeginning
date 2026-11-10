package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserCollisionContext extends EntityCollisionContext {
    private static final int MAX_CACHED_BLOCKS = 4096;
    private final float partialTick;
    private final LaserTraceGeometry geometry;
    private final @Nullable BlockGetter overrideOwner;
    private final @Nullable BlockPos overridePos;
    private final @Nullable OpticalGeometry overrideGeometry;
    private final Long2ObjectOpenHashMap<BlockHitResult> blockHits = new Long2ObjectOpenHashMap<>();
    private @Nullable Object queryOwner;
    private @Nullable Vec3 queryStart;
    private @Nullable Vec3 queryEnd;

    public LaserCollisionContext(float partialTick) {
        this(partialTick, false);
    }

    public LaserCollisionContext(float partialTick, boolean tracking) {
        super(false, -Double.MAX_VALUE, ItemStack.EMPTY, fluid -> false, null);
        this.partialTick = partialTick;
        geometry = new LaserTraceGeometry(tracking);
        overrideOwner = null;
        overridePos = null;
        overrideGeometry = null;
    }

    public LaserCollisionContext(float partialTick, BlockGetter owner, BlockPos pos, OpticalGeometry geometry) {
        super(false, -Double.MAX_VALUE, ItemStack.EMPTY, fluid -> false, null);
        this.partialTick = partialTick;
        this.geometry = new LaserTraceGeometry(false);
        overrideOwner = owner;
        overridePos = pos.immutable();
        overrideGeometry = geometry;
    }

    @Override
    public boolean isAbove(VoxelShape shape, BlockPos pos, boolean canAscend) {
        return canAscend;
    }

    public float getPartialTick() {
        return partialTick;
    }

    Function<BlockPos, @Nullable BlockHitResult> cacheQuery(Object owner, Vec3 start, Vec3 end, Function<BlockPos, @Nullable BlockHitResult> query) {
        if (queryOwner != owner || !start.equals(queryStart) || !end.equals(queryEnd)) {
            blockHits.clear();
            queryOwner = owner;
            queryStart = start;
            queryEnd = end;
        }
        return pos -> {
            long key = pos.asLong();
            if (blockHits.containsKey(key)) {
                return blockHits.get(key);
            }

            BlockHitResult hit = query.apply(pos);
            if (blockHits.size() >= MAX_CACHED_BLOCKS) {
                blockHits.clear();
            }
            blockHits.put(key, hit);
            return hit;
        };
    }

    LaserTraceGeometry getGeometry() {
        return geometry;
    }

    @Nullable OpticalGeometry getOverride(BlockGetter owner, BlockPos pos) {
        if (owner != overrideOwner || !pos.equals(overridePos)) {
            return null;
        }

        return overrideGeometry;
    }
}
