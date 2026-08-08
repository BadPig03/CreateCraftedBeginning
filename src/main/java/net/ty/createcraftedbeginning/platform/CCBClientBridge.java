package net.ty.createcraftedbeginning.platform;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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

    public static void showGasAreaOutline(Player player, BlockPos pos, Direction direction, float inflation, int color) {
        service.showGasAreaOutline(player, pos, direction, inflation, color);
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

    public static void openGasFactoryGaugeScreen(ScreenTarget target, Player player) {
        service.openGasFactoryGaugeScreen(target, player);
    }

    @Nullable
    public static ScreenTarget createGasFactoryGaugeBehaviour(RegistryFriendlyByteBuf extraData) {
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

        default void showGasAreaOutline(Player player, BlockPos pos, Direction direction, float inflation, int color) {
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

        default void openGasFactoryGaugeScreen(ScreenTarget target, Player player) {
        }

        default @Nullable ScreenTarget createGasFactoryGaugeBehaviour(RegistryFriendlyByteBuf extraData) {
            return null;
        }
    }

    public interface ScreenTarget {}
}
