package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystCollectorPanelTextureMapping {
    private AmethystCollectorPanelTextureMapping() {
    }

    static int stripStart(int tiles) {
        return tiles >= 3 ? 0 : tiles == 2 ? 48 : 80;
    }

    static double tileCoordinate(int index, int length, int sourceTiles, double local) {
        int tile = index == 0 ? 0 : index == length - 1 ? sourceTiles - 1 : 1;
        return tile * 16 + local;
    }

    static double topU(int x, int width, int depth, double localX) {
        if (width == 1 && depth > 1) {
            return (depth == 2 ? 96 : 112) + localX;
        }
        if (depth == 1 && width > 1) {
            return 80 + tileCoordinate(x, width, Math.min(3, width), localX);
        }
        int tiles = Math.min(3, Math.max(width, depth));
        return stripStart(tiles) + tileCoordinate(x, width, tiles, localX);
    }

    static double topV(int z, int width, int depth, double localZ) {
        if (width == 1 && depth > 1) {
            return 32 + tileCoordinate(z, depth, Math.min(3, depth), localZ);
        }
        if (depth == 1 && width > 1) {
            return (width == 2 ? 16 : 0) + localZ;
        }
        int tiles = Math.min(3, Math.max(width, depth));
        return (3 - tiles) * 16 + tileCoordinate(z, depth, tiles, localZ);
    }

    static double sideU(int index, int length, double local) {
        int tiles = Math.min(3, length);
        return stripStart(tiles) + tileCoordinate(index, length, tiles, local);
    }

    static double bottomU(int x, int width, int depth, double localX) {
        if (width == 1 && depth > 1) {
            return (depth == 2 ? 96 : 112) + localX;
        }
        if (depth == 1 && width > 1) {
            return (width == 2 ? 80 : 48) + tileCoordinate(x, width, Math.min(3, width), localX);
        }
        int tiles = Math.min(3, Math.max(width, depth));
        return stripStart(tiles) + tileCoordinate(x, width, tiles, localX);
    }

    static double bottomV(int z, int width, int depth, double localZ) {
        if (width == 1 && depth > 1) {
            return (depth == 2 ? 96 : 80) + tileCoordinate(z, depth, Math.min(3, depth), localZ);
        }
        if (depth == 1 && width > 1) {
            return (width == 2 ? 80 : 96) + localZ;
        }
        int tiles = Math.min(3, Math.max(width, depth));
        return 64 + tileCoordinate(z, depth, tiles, localZ);
    }
}
