package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import com.mojang.serialization.Codec;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IDirectionalPipe {
    EnumProperty<DirectionalFacing> DIRECTIONAL_FACING = EnumProperty.create("directional_facing", DirectionalFacing.class);

    enum DirectionalFacing implements StringRepresentable {
        NULL,
        NORTH,
        EAST,
        SOUTH,
        WEST;

        public static final Codec<DirectionalFacing> CODEC = StringRepresentable.fromEnum(DirectionalFacing::values);

        /**
         * Returns the y angle.
         *
         * @param facing the facing direction
         * @return the y angle
         */
        @Contract(pure = true)
        public static int getYAngle(DirectionalFacing facing) {
            return switch (facing) {
                case EAST -> 90;
                case SOUTH -> 0;
                case WEST -> 270;
                default -> 180;
            };
        }

        /**
         * Returns the facing direction.
         *
         * @param direction the direction associated with the operation
         * @return the facing direction
         */
        @Contract(pure = true)
        public static DirectionalFacing getFacingDirection(Direction direction) {
            return switch (direction) {
                case NORTH -> NORTH;
                case EAST -> EAST;
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                default -> NULL;
            };
        }

        /**
         * Returns the direction.
         *
         * @param facing the facing direction
         * @return the direction
         */
        @Contract(pure = true)
        public static Direction getDirection(DirectionalFacing facing) {
            return switch (facing) {
                case EAST -> Direction.EAST;
                case SOUTH -> Direction.SOUTH;
                case WEST -> Direction.WEST;
                default -> Direction.NORTH;
            };
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getSerializedName() {
            return Lang.asId(name());
        }
    }
}
