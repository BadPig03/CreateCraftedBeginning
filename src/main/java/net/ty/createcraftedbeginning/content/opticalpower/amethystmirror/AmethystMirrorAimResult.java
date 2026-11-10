package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AmethystMirrorAimResult(AmethystMirrorAimStatus status, @Nullable Quaternionf rotation, Vec3 incomingStart, Vec3 reflectionPoint, Vec3 targetPoint) {
    static final StreamCodec<RegistryFriendlyByteBuf, AmethystMirrorAimResult> STREAM_CODEC = StreamCodec.of((buffer, result) -> {
        buffer.writeUtf(result.status.getSerializedName());
        Quaternionf rotation = result.rotation;
        buffer.writeBoolean(rotation != null);
        if (rotation == null) {
            return;
        }

        buffer.writeFloat(rotation.x);
        buffer.writeFloat(rotation.y);
        buffer.writeFloat(rotation.z);
        buffer.writeFloat(rotation.w);
        for (Vec3 point : List.of(result.incomingStart, result.reflectionPoint, result.targetPoint)) {
            buffer.writeDouble(point.x);
            buffer.writeDouble(point.y);
            buffer.writeDouble(point.z);
        }
    }, buffer -> {
        AmethystMirrorAimStatus status = AmethystMirrorAimStatus.fromId(buffer.readUtf());
        if (!buffer.readBoolean()) {
            return failure(status);
        }

        Quaternionf rotation = new Quaternionf(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        Vec3 incoming = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Vec3 reflection = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Vec3 target = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        return new AmethystMirrorAimResult(status, rotation, incoming, reflection, target);
    });

    AmethystMirrorAimResult {
        if (rotation != null) {
            rotation = new Quaternionf(rotation);
        }
    }

    static AmethystMirrorAimResult failure(AmethystMirrorAimStatus status) {
        return new AmethystMirrorAimResult(status, null, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO);
    }

    @Override
    public @Nullable Quaternionf rotation() {
        if (rotation == null) {
            return null;
        }

        return new Quaternionf(rotation);
    }
}
