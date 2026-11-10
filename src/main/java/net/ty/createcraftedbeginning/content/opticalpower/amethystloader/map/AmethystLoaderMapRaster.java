package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.util.FastColor.ABGR32;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.Mth;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.MapColor;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapRepaint.Region;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSnapshot.Column;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSnapshot.Tint;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystLoaderMapRaster {
    private static final int WIDTH = AmethystLoaderMapSnapshot.TEXTURE_WIDTH;
    private static final int HEIGHT = AmethystLoaderMapSnapshot.TEXTURE_HEIGHT;
    private static final int MAX_SLOPE = 4;
    private static final float MIN_SHADE = 0.72F;
    private static final float MAX_SHADE = 1.22F;
    private static final float SLOPE_STRENGTH = 0.045F;
    private static final float FOLIAGE_BRIGHTNESS = 0.8F;
    private static final float GRASS_BRIGHTNESS = 0.95F;
    private static final float WATER_ABSORPTION_DEPTH = 4;
    private static final float WATER_SURFACE_OPACITY = 0.35F;
    private static final float WATER_DEPTH_OPACITY = 0.55F;
    private static final float WATER_MAX_DARKENING = 0.25F;
    private static final int WATER_DARKENING_DEPTH = 8;

    private AmethystLoaderMapRaster() {
    }

    static NativeImage createImage(AmethystLoaderMapSnapshot snapshot, Registry<Biome> biomes) {
        NativeImage image = new NativeImage(WIDTH, HEIGHT, false);
        paint(image, snapshot, biomes, List.of(new Region(0, 0, WIDTH, HEIGHT)));
        return image;
    }

    static void paint(NativeImage image, AmethystLoaderMapSnapshot snapshot, Registry<Biome> biomes, List<Region> regions) {
        GlobalPos position = snapshot.position();
        if (position == null) {
            throw new IllegalArgumentException("Missing amethyst loader map position.");
        }

        List<Column> columns = snapshot.columns();
        int stride = AmethystLoaderMapSnapshot.SAMPLE_WIDTH;
        BlockPos origin = AmethystLoaderMapSnapshot.textureOrigin(position.pos());
        int startX = origin.getX();
        int startZ = origin.getZ();
        for (Region region : regions) {
            int minX = region.x();
            int minZ = region.z();
            int maxX = minX + region.width();
            int maxZ = minZ + region.height();
            for (int row = minZ; row < maxZ; row++) {
                for (int column = minX; column < maxX; column++) {
                    int index = (row + 1) * stride + column + 1;
                    Column sample = columns.get(index);
                    int mapColor = sample.color();
                    if (mapColor == Column.WALL.color()) {
                        image.setPixelRGBA(column, row, 0xFF30323A);
                        continue;
                    }

                    if (mapColor < 0) {
                        image.setPixelRGBA(column, row, unknownColor(column, row));
                        continue;
                    }


                    if (mapColor == 0) {
                        image.setPixelRGBA(column, row, 0xFF222222);
                        continue;
                    }

                    Tint tint = sample.tint();
                    int color = switch (tint) {
                        case GRASS, FOLIAGE -> biomeColor(columns, index, biomes, startX + column, startZ + row, false);
                        case SPRUCE -> FoliageColor.getEvergreenColor();
                        case BIRCH -> FoliageColor.getBirchColor();
                        default -> MapColor.byId(mapColor).col;
                    };
                    float brightness = 1;
                    if (tint == Tint.GRASS) {
                        brightness = GRASS_BRIGHTNESS;
                    }
                    else if (tint != Tint.NONE) {
                        brightness = FOLIAGE_BRIGHTNESS;
                    }
                    int depth = sample.waterDepth();
                    if (depth > 0) {
                        int water = biomeColor(columns, index, biomes, startX + column, startZ + row, true);
                        float opacity = WATER_SURFACE_OPACITY + WATER_DEPTH_OPACITY * (1 - (float) Math.exp(-depth / WATER_ABSORPTION_DEPTH));
                        color = ARGB32.lerp(opacity, color, water);
                        brightness = 1 - WATER_MAX_DARKENING * depth / (depth + WATER_DARKENING_DEPTH);
                    }
                    Column north = columns.get(index - stride);
                    Column west = columns.get(index - 1);
                    int height = sample.height();
                    int northSlope = north.color() > 0 ? Mth.clamp(height - north.height(), -MAX_SLOPE, MAX_SLOPE) : 0;
                    int westSlope = west.color() > 0 ? Mth.clamp(height - west.height(), -MAX_SLOPE, MAX_SLOPE) : 0;
                    brightness *= Mth.clamp(1 + (northSlope + westSlope) * SLOPE_STRENGTH, MIN_SHADE, MAX_SHADE);
                    int red = Mth.clamp(Math.round(ARGB32.red(color) * brightness), 0, 255);
                    int green = Mth.clamp(Math.round(ARGB32.green(color) * brightness), 0, 255);
                    int blue = Mth.clamp(Math.round(ARGB32.blue(color) * brightness), 0, 255);
                    image.setPixelRGBA(column, row, ABGR32.fromArgb32(ARGB32.color(red, green, blue)));
                }
            }
        }
    }

    static int unknownColor(int x, int z) {
        if ((x + z) / 4 % 2 == 0) {
            return 0xFF555555;
        }

        return 0xFF626262;
    }

    private static int biomeColor(List<Column> columns, int index, Registry<Biome> biomes, int x, int z, boolean water) {
        int stride = AmethystLoaderMapSnapshot.SAMPLE_WIDTH;
        Tint tint = columns.get(index).tint();
        int red = 0;
        int green = 0;
        int blue = 0;
        int count = 0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                Column neighbor = columns.get(index + dz * stride + dx);
                if (neighbor.color() <= 0) {
                    continue;
                }

                Biome biome = biomes.byIdOrThrow(neighbor.biome());
                int color;
                if (water) {
                    color = biome.getWaterColor();
                }
                else if (tint == Tint.GRASS) {
                    color = biome.getGrassColor(x + dx, z + dz);
                }
                else {
                    color = biome.getFoliageColor();
                }
                red += ARGB32.red(color);
                green += ARGB32.green(color);
                blue += ARGB32.blue(color);
                count++;
            }
        }
        return ARGB32.color(red / count, green / count, blue / count);
    }
}
