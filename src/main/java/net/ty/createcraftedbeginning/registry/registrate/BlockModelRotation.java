package net.ty.createcraftedbeginning.registry.registrate;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BlockModelRotation {
    private BlockModelRotation() {
    }

    static int horizontal(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
            default -> 0;
        };
    }
}
