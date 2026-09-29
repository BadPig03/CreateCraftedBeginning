package net.ty.createcraftedbeginning.platform.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ClientRenderBridge {
    private static final Service NOOP = new Service() {};
    private static volatile Service service = NOOP;

    private ClientRenderBridge() {
    }

    public static void install(Service service) {
        ClientRenderBridge.service = service;
    }

    public static boolean addAlignedTooltipBars(List<Component> tooltip, int indent, List<? extends Component> labels, List<? extends Component> bars) {
        return service.addAlignedTooltipBars(tooltip, indent, labels, bars);
    }

    public static void dontAnimateAirtightCannon(InteractionHand hand) {
        service.dontAnimateAirtightCannon(hand);
    }

    public static void showPlacementBounds(BlockPlaceContext context, String outlineId, BlockPos placementPos, AABB bounds) {
        Player player = context.getPlayer();
        if (player == null) {
            return;
        }

        showPlacementBounds(player, outlineId, placementPos, bounds, "gui.warnings.clear_blocks_for_placement");
    }

    public static void showPlacementBounds(Player player, String outlineId, BlockPos placementPos, AABB bounds, String warningKey) {
        service.showPlacementBounds(player, outlineId, placementPos, bounds, warningKey);
    }

    public static void showGasAreaOutline(Player player, BlockPos effectPos, float inflation, int color) {
        service.showGasAreaOutline(player, effectPos, inflation, color);
    }

    public interface Service {
        default boolean addAlignedTooltipBars(List<Component> tooltip, int indent, List<? extends Component> labels, List<? extends Component> bars) {
            return false;
        }

        default void dontAnimateAirtightCannon(InteractionHand hand) {
        }

        default void showPlacementBounds(Player player, String outlineId, BlockPos placementPos, AABB bounds, String warningKey) {
        }

        default void showGasAreaOutline(Player player, BlockPos effectPos, float inflation, int color) {
        }
    }
}
