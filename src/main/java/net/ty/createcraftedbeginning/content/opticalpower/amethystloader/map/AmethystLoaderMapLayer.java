package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public record AmethystLoaderMapLayer(AmethystLoaderMapMode mode, int height, int bottom, int top) {
    static final AmethystLoaderMapLayer SURFACE = new AmethystLoaderMapLayer(AmethystLoaderMapMode.SURFACE, 0);
    static final int STEP = 8;
    static final int MAX_CACHED_LAYERS = 5;

    AmethystLoaderMapLayer(AmethystLoaderMapMode mode, int height) {
        this(mode, height, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static AmethystLoaderMapLayer read(FriendlyByteBuf buffer) {
        return new AmethystLoaderMapLayer(buffer.readEnum(AmethystLoaderMapMode.class), buffer.readInt(), buffer.readInt(), buffer.readInt());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeEnum(mode);
        buffer.writeInt(height);
        buffer.writeInt(bottom);
        buffer.writeInt(top);
    }
}
