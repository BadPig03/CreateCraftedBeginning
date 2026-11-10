package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderBlock;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSnapshot.Column;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSnapshot.Tint;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystLoaderMapSampler {
    private static final int CAVITY_RADIUS = 8;
    private static final ClassValue<Boolean> STATIC_COLOR = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("getMapColor", BlockState.class, BlockGetter.class, BlockPos.class, MapColor.class).getDeclaringClass() == IBlockExtension.class;
            }
            catch (NoSuchMethodException exception) {
                throw new IllegalStateException("Block map color method is unavailable.", exception);
            }
        }
    };

    private AmethystLoaderMapSampler() {
    }

    static Sample sampleColumn(ServerLevel level, LevelChunk chunk, Registry<Biome> biomes, MutableBlockPos cursor, int x, int z, AmethystLoaderMapLayer layer) {
        boolean cacheable = true;
        AmethystLoaderMapMode mode = layer.mode();
        int minY = level.getMinBuildHeight();
        int height = chunk.getHeight(Types.WORLD_SURFACE, x & 15, z & 15);
        if (mode != AmethystLoaderMapMode.SURFACE && height >= minY) {
            boolean interior = mode == AmethystLoaderMapMode.INTERIOR;
            int ceiling = interior ? layer.top() : AmethystLoaderMapLayers.roofY(level) - 1;
            int bottom = interior ? layer.bottom() : minY;
            if (bottom > ceiling) {
                return new Sample(Column.VOID, true);
            }

            int reference = Mth.clamp(layer.height(), bottom, ceiling);
            height = minY - 1;
            for (int distance = 0; distance <= CAVITY_RADIUS && height < minY; distance++) {
                for (int side = 0; side < (distance == 0 ? 1 : 2); side++) {
                    int candidate = reference + (side == 0 ? -distance : distance);
                    if (candidate < bottom || candidate > ceiling) {
                        continue;
                    }

                    cursor.set(x, candidate, z);
                    BlockState candidateState = chunk.getBlockState(cursor);
                    Block candidateBlock = candidateState.getBlock();
                    cacheable &= !candidateBlock.hasDynamicShape();
                    boolean loader = interior && candidateBlock instanceof AmethystLoaderBlock;
                    if (!loader && !candidateState.isAir() && (candidateState.getFluidState().isEmpty() || candidateState.isFaceSturdy(chunk, cursor, Direction.UP))) {
                        continue;
                    }

                    height = candidate;
                    break;
                }
            }
            if (height < minY) {
                return new Sample(Column.WALL, cacheable);
            }

            cursor.set(x, height, z);
            FluidState fluid = chunk.getFluidState(cursor);
            while (!fluid.isEmpty() && height < ceiling) {
                cursor.set(x, height + 1, z);
                BlockState above = chunk.getBlockState(cursor);
                cacheable &= !above.getBlock().hasDynamicShape();
                if (!above.getFluidState().getType().isSame(fluid.getType()) || above.isFaceSturdy(chunk, cursor, Direction.UP)) {
                    break;
                }

                height++;
            }
        }
        int waterSurface = minY - 1;
        MapColor color = MapColor.NONE;
        BlockState state = Blocks.AIR.defaultBlockState();
        while (height >= minY) {
            cursor.set(x, height, z);
            state = chunk.getBlockState(cursor);
            Block block = state.getBlock();
            cacheable &= STATIC_COLOR.get(block.getClass()) && !block.hasDynamicShape();
            if (state.getFluidState().is(FluidTags.WATER) && !state.isFaceSturdy(chunk, cursor, Direction.UP)) {
                waterSurface = Math.max(waterSurface, height);
                height--;
                continue;
            }

            color = state.getMapColor(level, cursor);
            boolean loader = mode == AmethystLoaderMapMode.INTERIOR && block instanceof AmethystLoaderBlock;
            if (color != MapColor.NONE && !(block instanceof BushBlock) && !loader) {
                break;
            }

            height--;
        }
        if (height < minY && waterSurface < minY) {
            return new Sample(Column.VOID, cacheable);
        }

        int surface = Math.max(height, waterSurface);
        int waterDepth = Math.max(0, waterSurface - height);
        Biome biome = chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(surface), QuartPos.fromBlock(z)).value();
        Tint tint = Tint.NONE;
        Block block = state.getBlock();
        if (block instanceof GrassBlock) {
            tint = Tint.GRASS;
        }
        else if (state.is(Blocks.SPRUCE_LEAVES)) {
            tint = Tint.SPRUCE;
        }
        else if (state.is(Blocks.BIRCH_LEAVES)) {
            tint = Tint.BIRCH;
        }
        else if (block instanceof VineBlock || block instanceof LeavesBlock && !state.is(Blocks.CHERRY_LEAVES) && !state.is(Blocks.AZALEA_LEAVES) && !state.is(Blocks.FLOWERING_AZALEA_LEAVES)) {
            tint = Tint.FOLIAGE;
        }
        if (height < minY) {
            color = MapColor.WATER;
            tint = Tint.NONE;
        }
        return new Sample(new Column(color.id, surface, waterDepth, biomes.getId(biome), tint), cacheable);
    }

    record Sample(Column column, boolean cacheable) {
    }
}
