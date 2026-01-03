package net.ty.createcraftedbeginning.api.gas.cansiters;

import net.createmod.catnip.theme.Color;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.data.CCBDistExecutor;

@OnlyIn(Dist.CLIENT)
public final class GasCanisterClientUtils {
    private GasCanisterClientUtils() {
    }

    /**
     * Determines whether the gas status bar should be visible on the client side.
     * <p>
     * This client-side only method checks if the local player has any non-empty gas suppliers
     * (gas canisters packs or gas canister) by retrieving the first non-empty gas content.
     * The status bar is displayed when the player has at least one gas supplier that contains gas.
     * </p>
     *
     * @return {@code true} if the player has at least one non-empty gas supplier and the status bar should be visible,
     * {@code false} if the player is null or has no gas suppliers containing gas
     * @see CCBDistExecutor#getClientPlayer()
     * @see GasCanisterSupplierUtils#getFirstNonEmptyGasContent(Player)
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean isBarVisible() {
        Player player = CCBDistExecutor.getClientPlayer();
        return player != null && !GasCanisterSupplierUtils.getFirstNonEmptyGasContent(player).isEmpty();
    }

    /**
     * Calculates the color for the gas status bar on the client side.
     * <p>
     * This client-side only method determines the appropriate color for displaying the gas
     * status bar by mixing cyan and white colors based on the fill ratio of the first gas type
     * found in the player's gas suppliers. The color transitions between these two colors
     * proportionally to the current gas fill level.
     * </p>
     *
     * @return the calculated color value for the status bar, or 0 if the player is null
     * @see GasCanisterQueryUtils#getTotalGasRatio(Player, Gas)
     * @see GasCanisterSupplierUtils#getFirstNonEmptyGasContent(Player)
     */
    @OnlyIn(Dist.CLIENT)
    public static int getBarColor() {
        Player player = CCBDistExecutor.getClientPlayer();
        return player == null ? 0 : Color.mixColors(GasCanisterUtils.COLOR_CYAN, GasCanisterUtils.COLOR_WHITE, GasCanisterQueryUtils.getTotalGasRatio(player, GasCanisterSupplierUtils.getFirstNonEmptyGasContent(player).getGas()));
    }

    /**
     * Calculates the width of the gas status bar on the client side.
     * <p>
     * This client-side only method determines the visual width of the gas status bar
     * based on the fill ratio of the first gas type found in the player's gas suppliers.
     * The width is calculated as a percentage of the maximum bar width (13 pixels).
     * </p>
     *
     * @return the calculated width of the status bar in pixels (0-13), or 0 if the player is null
     * @see CCBDistExecutor#getClientPlayer()
     * @see GasCanisterQueryUtils#getTotalGasRatio(Player, Gas)
     * @see GasCanisterSupplierUtils#getFirstNonEmptyGasContent(Player)
     */
    @OnlyIn(Dist.CLIENT)
    public static int getBarWidth() {
        Player player = CCBDistExecutor.getClientPlayer();
        return player == null ? 0 : Math.round(13 * GasCanisterQueryUtils.getTotalGasRatio(player, GasCanisterSupplierUtils.getFirstNonEmptyGasContent(player).getGas()));
    }
}
