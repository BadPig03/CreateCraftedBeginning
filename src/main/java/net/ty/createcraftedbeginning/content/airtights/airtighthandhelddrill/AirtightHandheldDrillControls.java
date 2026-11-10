package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import com.google.common.collect.MapMaker;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.ConcurrentMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class AirtightHandheldDrillControls {
    private static final ConcurrentMap<Player, SingleBlockMode> SINGLE_BLOCK_MODES = new MapMaker().weakKeys().makeMap();

    private AirtightHandheldDrillControls() {
    }

    public static synchronized boolean isSingleBlockMining(Player player, ItemStack drill) {
        SingleBlockMode mode = SINGLE_BLOCK_MODES.get(player);
        return mode != null && mode.drill() == drill && mode.hotbarSlot() == player.getInventory().selected && mode.dimension() == player.level().dimension() && player.getMainHandItem() == drill && drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL) && player.isAlive() && !player.isSpectator() && player.containerMenu == player.inventoryMenu;
    }

    public static synchronized void validateServerState(ServerPlayer player) {
        if (!SINGLE_BLOCK_MODES.containsKey(player) || isSingleBlockMining(player, player.getMainHandItem())) {
            return;
        }

        setSingleBlockMode(player, false);
    }

    static synchronized boolean setSingleBlockMode(Player player, boolean pressed) {
        if (pressed) {
            ItemStack drill = player.getMainHandItem();
            if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL) || !player.isAlive() || player.isSpectator() || player.containerMenu != player.inventoryMenu || isSingleBlockMining(player, drill)) {
                return false;
            }

            SINGLE_BLOCK_MODES.put(player, new SingleBlockMode(drill, player.getInventory().selected, player.level().dimension()));
        }
        else if (SINGLE_BLOCK_MODES.remove(player) == null) {
            return false;
        }

        if (!(player instanceof ServerPlayer serverPlayer) || !(serverPlayer.gameMode instanceof DrillMiningProgressAccess progress)) {
            return true;
        }

        progress.ccb$resetDrillMiningProgress();
        return true;
    }

    private record SingleBlockMode(ItemStack drill, int hotbarSlot, ResourceKey<Level> dimension) {
    }
}
