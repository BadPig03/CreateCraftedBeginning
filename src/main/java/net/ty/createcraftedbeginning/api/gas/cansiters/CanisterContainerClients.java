package net.ty.createcraftedbeginning.api.gas.cansiters;

import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.data.CCBDistExecutor;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.NotNull;

public final class CanisterContainerClients {
    public static final String COMPOUND_KEY_STORED_GAS_TYPE = "CreateCraftedBeginningStoredGasType";

    private CanisterContainerClients() {
    }

    /**
     * Determines whether the gas canister status bar should be visible in the client's HUD.
     * <p>
     * This client-side method checks if the local player has at least one non-empty gas canister container
     * that can be displayed in the user interface. The bar is only visible when there is a valid container
     * with gas content to display.
     * </p>
     *
     * @return true if the gas canister status bar should be rendered, false otherwise
     * @see CanisterContainerSuppliers#getFirstCanisterSupplier(Player)
     * @see IGasCanisterContainer#isEmpty()
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean isBarVisible() {
        Player player = CCBDistExecutor.getClientPlayer();
        if (player == null) {
            return false;
        }

        IGasCanisterContainer container = CanisterContainerSuppliers.getFirstCanisterSupplier(player);
        return container != null && !container.isEmpty();
    }

    /**
     * Calculates the color for the gas canister status bar based on the current fill level.
     * <p>
     * This client-side method determines the visual color of the gas canister HUD element
     * by interpolating between cyan (empty) and white (full) based on the fill ratio
     * of the player's first available gas canister container.
     * </p>
     * <p>
     * The color transition provides a visual indication of the gas level, with cyan
     * representing low levels and white representing full capacity.
     * </p>
     *
     * @return the interpolated color value for the gas bar display, or 0 if no valid container is found
     * @see CanisterContainerSuppliers#getFirstCanisterSupplierRatio(Player)
     * @see Color#mixColors(int, int, float)
     */
    @OnlyIn(Dist.CLIENT)
    public static int getBarColor() {
        Player player = CCBDistExecutor.getClientPlayer();
        if (player == null) {
            return 0;
        }

        float ratio = CanisterContainerSuppliers.getFirstCanisterSupplierRatio(player);
        if (ratio == 0) {
            return 0;
        }

        return Color.mixColors(GasCanisterUtils.COLOR_CYAN, GasCanisterUtils.COLOR_WHITE, ratio);
    }

    /**
     * Calculates the visual width of the gas canister status bar in the HUD.
     * <p>
     * This client-side method determines the width of the gas canister HUD element
     * based on the fill ratio of the player's first available gas canister container.
     * The bar width ranges from 0 to 13 pixels, representing empty to full capacity.
     * </p>
     * <p>
     * The width is calculated by multiplying the maximum bar width (13 pixels)
     * by the current fill ratio of the gas container.
     * </p>
     *
     * @return the width of the gas bar in pixels (0-13), or 0 if no valid container is found
     * @see CanisterContainerSuppliers#getFirstCanisterSupplierRatio(Player)
     */
    @OnlyIn(Dist.CLIENT)
    public static int getBarWidth() {
        Player player = CCBDistExecutor.getClientPlayer();
        if (player == null) {
            return 0;
        }

        float ratio = CanisterContainerSuppliers.getFirstCanisterSupplierRatio(player);
        if (ratio == 0) {
            return 0;
        }

        return Math.round(13 * ratio);
    }

    /**
     * Calculates the color for the gas canister status bar based on its current fill level.
     * <p>
     * This method determines the visual color of a gas canister's status bar by interpolating
     * between cyan (empty) and white (full) based on the fill ratio of the gas canister.
     * The fill ratio is calculated as the current gas amount divided by the canister's total capacity.
     * </p>
     * <p>
     * If the canister is invalid, empty, or has zero capacity, the method returns 0 indicating
     * that no bar should be displayed.
     * </p>
     *
     * @param canister the gas canister ItemStack to check (cannot be null)
     * @return the interpolated color value for the gas bar, or 0 if the canister is invalid, empty, or has zero capacity
     * @see GasCanisterContainerContents
     * @see Color#mixColors(int, int, float)
     */
    public static int getBarColor(@NotNull ItemStack canister) {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return 0;
        }

        long amount = canisterContents.getGasInTank(0).getAmount();
        long capacity = canisterContents.getTankCapacity(0);
        if (amount == 0 || capacity == 0) {
            return 0;
        }

        return Color.mixColors(GasCanisterUtils.COLOR_CYAN, GasCanisterUtils.COLOR_WHITE, Mth.clamp((float) amount / capacity, 0, 1));
    }

    /**
     * Calculates the visual width of the gas canister status bar based on its current fill level.
     * <p>
     * This method determines the width of the gas canister's status bar by calculating
     * the fill ratio of the gas canister and converting it to a pixel width.
     * The width ranges from 0 to 13 pixels, representing empty to full capacity.
     * </p>
     * <p>
     * The fill ratio is calculated as the current gas amount divided by the canister's total capacity,
     * clamped between 0.0 and 1.0, then multiplied by the maximum bar width of 13 pixels.
     * </p>
     * <p>
     * If the canister is invalid, empty, or has zero capacity, the method returns 0 indicating
     * that no bar should be displayed.
     * </p>
     *
     * @param canister the gas canister ItemStack to check (cannot be null)
     * @return the width of the gas bar in pixels (0-13), or 0 if the canister is invalid, empty, or has zero capacity
     * @see GasCanisterContainerContents
     */
    public static int getBarWidth(@NotNull ItemStack canister) {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return 0;
        }

        long amount = canisterContents.getGasInTank(0).getAmount();
        long capacity = canisterContents.getTankCapacity(0);
        if (amount == 0 || capacity == 0) {
            return 0;
        }

        return Math.round(13 * Mth.clamp((float) amount / capacity, 0, 1));
    }

    /**
     * Retrieves the stored gas type from the player's persistent data.
     * <p>
     * This method reads the gas type that was previously stored in the player's persistent NBT data.
     * The gas type is stored as a ResourceLocation string and converted back to a Gas object.
     * </p>
     *
     * @param player the player whose stored gas type will be retrieved (cannot be null)
     * @return the stored gas type (never null)
     * @see NBTHelper#readResourceLocation(CompoundTag, String) 
     * @see Gas#getGasTypeByName(ResourceLocation)
     */
    public static @NotNull Gas getStoredGasType(@NotNull Player player) {
        return Gas.getGasTypeByName(NBTHelper.readResourceLocation(player.getPersistentData(), COMPOUND_KEY_STORED_GAS_TYPE));
    }

    /**
     * Displays a custom warning hint to the player with visual and audio feedback (server-side only).
     * <p>
     * This method shows a client-side warning message to the player in red text format
     * and plays a denial sound effect, but only executes on the server side. The message
     * is displayed as an action bar message (above the hotbar) for prominent visibility.
     * </p>
     * <p>
     * The method includes a client-side check to ensure it only runs on the server,
     * preventing duplicate execution when called from client-side code.
     * </p>
     *
     * @param player the player to display the warning to (must not be null)
     * @param key    the translation key for the warning message (must not be null)
     * @param args   optional arguments for the translation key
     * @see CCBLang#translateDirect(String, Object...)
     * @see CCBSoundEvents#DENY
     */
    public static void displayCustomWarningHint(@NotNull Player player, @NotNull String key, Object... args) {
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        player.displayClientMessage(CCBLang.translateDirect(key, args).withStyle(ChatFormatting.RED), true);
        CCBSoundEvents.DENY.playOnServer(level, player.blockPosition(), 1, 1);
    }
}
