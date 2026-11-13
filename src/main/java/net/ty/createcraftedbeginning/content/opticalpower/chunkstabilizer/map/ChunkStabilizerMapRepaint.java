package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapSnapshot.Column;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class ChunkStabilizerMapRepaint {
    private static final int WIDTH = ChunkStabilizerMapSnapshot.TEXTURE_WIDTH;
    private static final int HEIGHT = ChunkStabilizerMapSnapshot.TEXTURE_HEIGHT;
    private static final int STRIDE = ChunkStabilizerMapSnapshot.SAMPLE_WIDTH;
    private static final int TILE_SIZE = 8;
    private static final int TILES_X = (WIDTH + TILE_SIZE - 1) / TILE_SIZE;
    private static final int TILES_Z = (HEIGHT + TILE_SIZE - 1) / TILE_SIZE;

    private ChunkStabilizerMapRepaint() {
    }

    static Plan plan(@Nullable ChunkStabilizerMapSnapshot previous, ChunkStabilizerMapSnapshot current, boolean reload) {
        GlobalPos position = current.position();
        GlobalPos oldPosition = previous == null ? null : previous.position();
        if (position == null) {
            throw new IllegalArgumentException("Missing current amethyst map position.");
        }

        if (reload || oldPosition == null || oldPosition.dimension() != position.dimension()) {
            return new Plan(0, 0, false, List.of(new Region(0, 0, WIDTH, HEIGHT)));
        }

        BlockPos origin = ChunkStabilizerMapSnapshot.textureOrigin(position.pos());
        BlockPos oldOrigin = ChunkStabilizerMapSnapshot.textureOrigin(oldPosition.pos());
        int dx = origin.getX() - oldOrigin.getX();
        int dz = origin.getZ() - oldOrigin.getZ();
        if (Math.abs(dx) >= WIDTH || Math.abs(dz) >= HEIGHT) {
            return new Plan(0, 0, false, List.of(new Region(0, 0, WIDTH, HEIGHT)));
        }

        List<Column> columns = current.columns();
        List<Column> oldColumns = previous.columns();
        BitSet tiles = new BitSet(TILES_X * TILES_Z);
        for (int z = -1; z <= HEIGHT; z++) {
            for (int x = -1; x <= WIDTH; x++) {
                int oldX = x + dx;
                int oldZ = z + dz;
                Column column = columns.get((z + 1) * STRIDE + x + 1);
                boolean changed = oldX < -1 || oldX > WIDTH || oldZ < -1 || oldZ > HEIGHT || !column.equals(oldColumns.get((oldZ + 1) * STRIDE + oldX + 1));
                if (!changed && (dx != 0 || dz != 0) && x >= 0 && x < WIDTH && z >= 0 && z < HEIGHT) {
                    changed = oldX < 0 || oldX == WIDTH || oldZ < 0 || oldZ == HEIGHT || column.color() < 0;
                }
                if (!changed) {
                    continue;
                }

                for (int row = Math.max(0, z - 1); row <= Math.min(HEIGHT - 1, z + 1); row++) {
                    for (int col = Math.max(0, x - 1); col <= Math.min(WIDTH - 1, x + 1); col++) {
                        tiles.set(row / TILE_SIZE * TILES_X + col / TILE_SIZE);
                    }
                }
            }
        }
        List<Region> regions = new ArrayList<>();
        for (int row = 0; row < TILES_Z; row++) {
            int col = 0;
            while (col < TILES_X) {
                if (!tiles.get(row * TILES_X + col)) {
                    col++;
                    continue;
                }

                int start = col++;
                while (col < TILES_X && tiles.get(row * TILES_X + col)) {
                    col++;
                }
                int x = start * TILE_SIZE;
                int z = row * TILE_SIZE;
                regions.add(new Region(x, z, Math.min(WIDTH, col * TILE_SIZE) - x, Math.min(TILE_SIZE, HEIGHT - z)));
            }
        }
        return new Plan(dx, dz, true, List.copyOf(regions));
    }

    record Plan(int dx, int dz, boolean reusable, List<Region> regions) {}

    record Region(int x, int z, int width, int height) {}
}
