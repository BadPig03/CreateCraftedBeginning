package net.ty.createcraftedbeginning.api.gas.interfaces;

import com.mojang.serialization.Codec;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface IDirectionalPipe {
    EnumProperty<DirectionalFacing> DIRECTIONAL_FACING = EnumProperty.create("directional_facing", DirectionalFacing.class);

    enum DirectionalFacing implements StringRepresentable {
        NULL,
        NORTH,
        EAST,
        SOUTH,
        WEST;

        public static final Codec<DirectionalFacing> CODEC = StringRepresentable.fromEnum(DirectionalFacing::values);

        @Override
        public @NotNull String getSerializedName() {
            return Lang.asId(name());
        }

        @Contract(pure = true)
        public static int getYAngle(@NotNull DirectionalFacing facing) {
            return switch (facing) {
                case EAST -> 90;
                case SOUTH -> 0;
                case WEST -> 270;
                default -> 180;
            };
        }

        @Contract(pure = true)
        public static DirectionalFacing getFacingDirection(@NotNull Direction direction) {
            return switch (direction) {
                case Direction.NORTH -> NORTH;
                case Direction.EAST -> EAST;
                case Direction.SOUTH -> SOUTH;
                case Direction.WEST -> WEST;
                default -> NULL;
            };
        }

        @Contract(pure = true)
        public static Direction getDirection(@NotNull DirectionalFacing facing) {
            return switch (facing) {
                case EAST -> Direction.EAST;
                case SOUTH -> Direction.SOUTH;
                case WEST -> Direction.WEST;
                default -> Direction.NORTH;
            };
        }
    }
}
