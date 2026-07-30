package net.ty.createcraftedbeginning.platform;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBehaviour;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBClientBridge {
    private static final Service NOOP = new Service() {};
    private static volatile Service service = NOOP;

    private CCBClientBridge() {
    }

    public static void install(Service implementation) {
        service = Objects.requireNonNull(implementation, "implementation");
    }

    @Nullable
    public static Player getClientPlayer() {
        return service.getClientPlayer();
    }

    public static boolean addAlignedTooltipBars(List<Component> tooltip, int indent, List<? extends Component> labels, List<? extends Component> bars) {
        return service.addAlignedTooltipBars(tooltip, indent, labels, bars);
    }

    public static void dontAnimateAirtightCannon(InteractionHand hand) {
        service.dontAnimateAirtightCannon(hand);
    }

    public static void showAirtightForgingPressPlacementBounds(BlockPlaceContext context) {
        service.showAirtightForgingPressPlacementBounds(context);
    }

    public static void showAirtightReactorKettlePlacementBounds(BlockPlaceContext context) {
        service.showAirtightReactorKettlePlacementBounds(context);
    }

    public static void showTeslaTurbinePlacementBounds(BlockPlaceContext context) {
        service.showTeslaTurbinePlacementBounds(context);
    }

    public static void openGasFactoryGaugeScreen(GasFactoryGaugeBehaviour behaviour, Player player) {
        service.openGasFactoryGaugeScreen(behaviour, player);
    }

    @Nullable
    public static GasFactoryGaugeBehaviour createGasFactoryGaugeBehaviour(RegistryFriendlyByteBuf extraData) {
        return service.createGasFactoryGaugeBehaviour(extraData);
    }

    public interface Service {
        default @Nullable Player getClientPlayer() {
            return null;
        }

        default boolean addAlignedTooltipBars(List<Component> tooltip, int indent, List<? extends Component> labels, List<? extends Component> bars) {
            return false;
        }

        default void dontAnimateAirtightCannon(InteractionHand hand) {
        }

        default void showAirtightForgingPressPlacementBounds(BlockPlaceContext context) {
        }

        default void showAirtightReactorKettlePlacementBounds(BlockPlaceContext context) {
        }

        default void showTeslaTurbinePlacementBounds(BlockPlaceContext context) {
        }

        default void openGasFactoryGaugeScreen(GasFactoryGaugeBehaviour behaviour, Player player) {
        }

        default @Nullable GasFactoryGaugeBehaviour createGasFactoryGaugeBehaviour(RegistryFriendlyByteBuf extraData) {
            return null;
        }
    }
}
