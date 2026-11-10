package net.ty.createcraftedbeginning.mixin.client.minecraft;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.platform.access.client.AmethystMirrorBufferAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements AmethystMirrorBufferAccess {
    @Shadow
    @Final
    private ByteBufferBuilder buffer;
    @Shadow
    @Final
    private VertexFormat format;
    @Shadow
    @Final
    private Mode mode;
    @Shadow
    @Final
    private int vertexSize;
    @Shadow
    private int vertices;
    @Shadow
    private long vertexPointer;

    @SuppressWarnings({"ConstantValue", "EqualsBetweenInconvertibleTypes"})
    @Override
    public long ccb$reserveMirrorVertices(int count) {
        if (!getClass().equals(BufferBuilder.class)) {
            return -1;
        }

        if (format != DefaultVertexFormat.NEW_ENTITY || mode != Mode.QUADS || count <= 0) {
            return -1;
        }

        ensureBuilding();
        endLastVertex();
        long pointer = buffer.reserve(Math.multiplyExact(count, vertexSize));
        vertices += count;
        vertexPointer = pointer + (long) (count - 1) * vertexSize;
        return pointer;
    }

    @Shadow
    protected abstract void ensureBuilding();

    @Shadow
    protected abstract void endLastVertex();
}
