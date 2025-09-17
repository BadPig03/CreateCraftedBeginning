package net.ty.createcraftedbeginning.data;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

@ApiStatus.Internal
public class CCBDistExecutor {
    @ApiStatus.Internal
    public static <T> @Nullable T unsafeCallWhenOn(Dist dist, Supplier<Callable<T>> toRun) {
        if (FMLLoader.getDist() == dist) {
            try {
                return toRun.get().call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
