package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags.Blocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystLoaderMapLayers {
    private static final int HALF_STEP = AmethystLoaderMapLayer.STEP / 2;
    private static final int HYSTERESIS = 2;
    private static final int SEARCH_RADIUS = 8;

    private AmethystLoaderMapLayers() {
    }

    static @Nullable AmethystLoaderMapLayer at(ServerLevel level, Vec3 position, @Nullable AmethystLoaderMapLayer previous) {
        ResourceKey<Level> dimension = level.dimension();
        if (dimension != Level.OVERWORLD && dimension != Level.END) {
            return at(level, position.y, previous);
        }

        BlockPos center = BlockPos.containing(position);
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) {
            return null;
        }

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        int highest = chunk.getHeight(Types.WORLD_SURFACE, x & 15, z & 15);
        int roof = maxY + 1;
        MutableBlockPos cursor = center.mutable();
        for (int scanY = Math.max(minY, y + 1); scanY <= highest; scanY++) {
            cursor.setY(scanY);
            if (!covers(chunk, cursor)) {
                continue;
            }

            roof = scanY;
            break;
        }
        if (roof > maxY) {
            return AmethystLoaderMapLayer.SURFACE;
        }

        int height = minY + Math.floorDiv(Mth.clamp(y, minY, maxY) - minY, AmethystLoaderMapLayer.STEP) * AmethystLoaderMapLayer.STEP + HALF_STEP;
        boolean interior = previous != null && previous.mode() == AmethystLoaderMapMode.INTERIOR;
        int previousHeight = previous == null ? height : previous.height();
        if (interior && position.y >= previousHeight - HALF_STEP - HYSTERESIS && position.y < previousHeight + HALF_STEP + HYSTERESIS) {
            height = previousHeight;
        }
        int bottom = Math.max(minY, height - SEARCH_RADIUS);
        int top = roof - 1;
        if (position.y < minY) {
            return new AmethystLoaderMapLayer(AmethystLoaderMapMode.INTERIOR, height, minY, minY - 1);
        }

        for (int scanY = Math.min(maxY, y - 1); scanY >= bottom; scanY--) {
            cursor.setY(scanY);
            if (!covers(chunk, cursor)) {
                continue;
            }

            bottom = scanY + 1;
            break;
        }
        if (interior && height == previousHeight) {
            int previousBottom = previous.bottom();
            int previousTop = previous.top();
            if (bottom <= previousBottom && top >= previousTop && y >= previousBottom && y <= previousTop) {
                return previous;
            }
        }

        return new AmethystLoaderMapLayer(AmethystLoaderMapMode.INTERIOR, height, bottom, top);
    }

    static AmethystLoaderMapLayer at(ServerLevel level, double y, @Nullable AmethystLoaderMapLayer previous) {
        if (mode(level, y) == AmethystLoaderMapMode.SURFACE) {
            return AmethystLoaderMapLayer.SURFACE;
        }

        if (previous != null && previous.mode() == AmethystLoaderMapMode.NETHER_INTERIOR) {
            int height = previous.height();
            if (y >= height - HALF_STEP - HYSTERESIS && y < height + HALF_STEP + HYSTERESIS) {
                return previous;
            }
        }

        int minY = level.getMinBuildHeight();
        int ceiling = roofY(level) - 1;
        int height = minY + Math.floorDiv(Mth.clamp(Mth.floor(y), minY, ceiling) - minY, AmethystLoaderMapLayer.STEP) * AmethystLoaderMapLayer.STEP + HALF_STEP;
        return new AmethystLoaderMapLayer(AmethystLoaderMapMode.NETHER_INTERIOR, Math.min(height, ceiling));
    }

    static AmethystLoaderMapMode mode(ServerLevel level, double y) {
        if (level.dimension() == Level.NETHER && level.dimensionType().hasCeiling() && y < roofY(level)) {
            return AmethystLoaderMapMode.NETHER_INTERIOR;
        }

        return AmethystLoaderMapMode.SURFACE;
    }

    static int roofY(ServerLevel level) {
        return level.getMinBuildHeight() + level.dimensionType().logicalHeight();
    }

    private static boolean covers(LevelChunk chunk, BlockPos position) {
        BlockState state = chunk.getBlockState(position);
        return !state.isAir() && !state.is(BlockTags.LEAVES) && !state.is(Blocks.GLASS_BLOCKS) && !state.is(Blocks.GLASS_PANES) && !(state.getBlock() instanceof BushBlock) && (state.isFaceSturdy(chunk, position, Direction.DOWN) || state.isFaceSturdy(chunk, position, Direction.UP));
    }
}
