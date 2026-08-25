package net.ty.createcraftedbeginning.platform.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ClientContextBridge {
    private static final Service NOOP = new Service() {};
    private static volatile Service service = NOOP;

    private ClientContextBridge() {
    }

    public static void install(Service service) {
        ClientContextBridge.service = service;
    }

    @Nullable
    public static Player getClientPlayer() {
        return service.getClientPlayer();
    }

    public static boolean isOverstressedTooltipEnabled() {
        return service.isOverstressedTooltipEnabled();
    }

    public static int getMaxItemStackDisplay() {
        return service.getMaxItemStackDisplay();
    }

    public static float getFilterItemRenderDistance() {
        return service.getFilterItemRenderDistance();
    }

    public interface Service {
        default @Nullable Player getClientPlayer() {
            return null;
        }

        default boolean isOverstressedTooltipEnabled() {
            return false;
        }

        default int getMaxItemStackDisplay() {
            return 4;
        }

        default float getFilterItemRenderDistance() {
            return 0;
        }
    }
}
