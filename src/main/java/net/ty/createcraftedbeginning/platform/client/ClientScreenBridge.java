package net.ty.createcraftedbeginning.platform.client;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ClientScreenBridge {
    private static final Service NOOP = new Service() {};
    private static volatile Service service = NOOP;

    private ClientScreenBridge() {
    }

    public static void install(Service service) {
        ClientScreenBridge.service = service;
    }

    public static void openGasFactoryGaugeScreen(FactoryPanelPosition panelPosition, Player player) {
        service.openGasFactoryGaugeScreen(panelPosition, player);
    }

    @Nullable
    public static FactoryPanelBehaviour resolveFactoryPanelBehaviour(RegistryFriendlyByteBuf extraData) {
        return service.resolveFactoryPanelBehaviour(extraData);
    }

    public interface Service {
        default void openGasFactoryGaugeScreen(FactoryPanelPosition panelPosition, Player player) {
        }

        default @Nullable FactoryPanelBehaviour resolveFactoryPanelBehaviour(RegistryFriendlyByteBuf extraData) {
            return null;
        }
    }
}
