package net.ty.createcraftedbeginning.data;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class CCBDistExecutor {
    @Nullable
    public static <T> T unsafeCallWhenOn(Dist dist, Supplier<Callable<T>> toRun) {
        if (FMLLoader.getDist() != dist) {
            return null;
        }

        try {
            return toRun.get().call();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Nullable
    public static Player getClientPlayer() {
        return unsafeCallWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().player);
    }
}
