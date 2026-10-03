package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum DrillMiningDirection implements StringRepresentable {
    FOLLOW_PLAYER,
    DOWN,
    UP,
    NORTH,
    SOUTH,
    WEST,
    EAST;

    public static final Codec<DrillMiningDirection> CODEC = StringRepresentable.fromValues(DrillMiningDirection::values);
    public static final StreamCodec<ByteBuf, DrillMiningDirection> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(DrillMiningDirection.class);
    private static final double VERTICAL_DIRECTION_PITCH_DEGREES = 45;

    @Override
    public String getSerializedName() {
        return Lang.asId(name());
    }

    public Direction resolve(Vec3 lookDirection) {
        return switch (this) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
            case FOLLOW_PLAYER -> {
                double horizontalLength = Math.hypot(lookDirection.x, lookDirection.z);
                double pitchDegrees = Math.toDegrees(Math.atan2(lookDirection.y, horizontalLength));
                if (pitchDegrees > VERTICAL_DIRECTION_PITCH_DEGREES) {
                    yield Direction.UP;
                }

                if (pitchDegrees < -VERTICAL_DIRECTION_PITCH_DEGREES) {
                    yield Direction.DOWN;
                }

                yield Direction.getNearest(lookDirection.x, 0, lookDirection.z);
            }
        };
    }
}
