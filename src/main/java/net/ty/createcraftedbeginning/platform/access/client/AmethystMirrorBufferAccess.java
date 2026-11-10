package net.ty.createcraftedbeginning.platform.access.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AmethystMirrorBufferAccess {
    long ccb$reserveMirrorVertices(int count);
}
