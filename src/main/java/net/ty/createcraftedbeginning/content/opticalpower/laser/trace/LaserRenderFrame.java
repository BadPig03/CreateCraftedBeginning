package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserRenderFrame {
    private static @Nullable WeakReference<Level> level;
    private static @Nullable Thread thread;
    private static @Nullable Object token;
    private static int partialTickBits;

    private LaserRenderFrame() {}

    public static void begin(@Nullable Level current) {
        clear();
        if (current == null) {
            return;
        }

        level = new WeakReference<>(current);
        thread = Thread.currentThread();
        token = new Object();
    }

    public static void clear() {
        level = null;
        thread = null;
        token = null;
    }

    public static @Nullable Object current(Level current, float partialTick) {
        WeakReference<Level> reference = level;
        if (thread != Thread.currentThread() || reference == null || reference.get() != current) {
            return null;
        }

        int bits = Float.floatToIntBits(partialTick);
        if (partialTickBits != bits) {
            partialTickBits = bits;
            token = new Object();
        }
        return token;
    }
}
