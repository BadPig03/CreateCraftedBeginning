package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.ChunkPos;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderLocation;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AmethystLoaderMapSnapshot(UUID identity, State state, @Nullable GlobalPos position, List<Column> columns) {
    public static final int WIDTH = 96;
    public static final int HEIGHT = 96;
    static final int MARGIN = 24;
    static final int TEXTURE_WIDTH = WIDTH + MARGIN * 2;
    static final int TEXTURE_HEIGHT = HEIGHT + MARGIN * 2;
    static final int SAMPLE_WIDTH = TEXTURE_WIDTH + 2;
    static final int SAMPLE_HEIGHT = TEXTURE_HEIGHT + 2;

    public AmethystLoaderMapSnapshot {
        int expected = state == State.READY ? SAMPLE_WIDTH * SAMPLE_HEIGHT : 0;
        int count = columns.size();
        if (count != expected) {
            throw new IllegalArgumentException("Amethyst map snapshot column count must be " + expected + " for state '" + state + "'; got " + count + '.');
        }

        if (state == State.READY && position == null) {
            throw new IllegalArgumentException("Ready amethyst map snapshot requires a world position.");
        }

        position = position == null ? null : GlobalPos.of(position.dimension(), position.pos().immutable());
        columns = List.copyOf(columns);
    }

    static BlockPos origin(BlockPos position) {
        return new BlockPos((position.getX() & -16) + 8 - WIDTH / 2, 0, (position.getZ() & -16) + 8 - HEIGHT / 2);
    }

    static BlockPos textureOrigin(BlockPos position) {
        return origin(position).offset(-MARGIN, 0, -MARGIN);
    }

    static List<Long> window(BlockPos position) {
        return window(position, 0);
    }

    static List<Long> bufferedWindow(BlockPos position) {
        return window(position, 1);
    }

    static AmethystLoaderMapSnapshot assemble(UUID identity, AmethystLoaderLocation location, Map<Long, AmethystLoaderMapChunk> chunks) {
        GlobalPos position = location.blockPosition();
        BlockPos origin = textureOrigin(position.pos());
        int startX = origin.getX();
        int startZ = origin.getZ();
        List<Column> columns = new ArrayList<>(SAMPLE_WIDTH * SAMPLE_HEIGHT);
        for (int z = startZ - 1; z <= startZ + TEXTURE_HEIGHT; z++) {
            for (int x = startX - 1; x <= startX + TEXTURE_WIDTH; x++) {
                AmethystLoaderMapChunk chunk = chunks.get(ChunkPos.asLong(x >> 4, z >> 4));
                columns.add(chunk == null || chunk.version() == null ? Column.UNKNOWN : chunk.columns().get((z & 15) * 16 + (x & 15)));
            }
        }
        return new AmethystLoaderMapSnapshot(identity, State.READY, position, columns);
    }

    private static List<Long> window(BlockPos position, int margin) {
        BlockPos origin = origin(position);
        int startX = origin.getX();
        int startZ = origin.getZ();
        List<Long> chunks = new ArrayList<>();
        for (int z = Math.floorDiv(startZ - 1, 16) - margin; z <= Math.floorDiv(startZ + HEIGHT, 16) + margin; z++) {
            for (int x = Math.floorDiv(startX - 1, 16) - margin; x <= Math.floorDiv(startX + WIDTH, 16) + margin; x++) {
                chunks.add(ChunkPos.asLong(x, z));
            }
        }
        return List.copyOf(chunks);
    }

    public enum State {
        READY,
        UNAVAILABLE
    }

    enum Tint {
        NONE,
        GRASS,
        FOLIAGE,
        SPRUCE,
        BIRCH
    }

    record Column(int color, int height, int waterDepth, int biome, Tint tint) {
        static final Column WALL = new Column(-2, 0, 0, 0, Tint.NONE);
        static final Column UNKNOWN = new Column(-1, 0, 0, 0, Tint.NONE);
        static final Column VOID = new Column(0, 0, 0, 0, Tint.NONE);
    }
}
