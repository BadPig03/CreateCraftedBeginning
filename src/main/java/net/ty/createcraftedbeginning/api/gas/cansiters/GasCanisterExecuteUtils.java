package net.ty.createcraftedbeginning.api.gas.cansiters;

import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackUtils;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class GasCanisterExecuteUtils {
    private GasCanisterExecuteUtils() {
    }

    /**
     * Displays a custom warning hint to the player with visual and audio feedback.
     * <p>
     * This method shows a red error message to the player using the specified translation key
     * and arguments, and plays a deny sound effect to provide immediate feedback.
     * </p>
     *
     * @param player the player to display the warning to (cannot be null)
     * @param key    the translation key for the warning message (cannot be null)
     * @param args   the arguments to be used in the translated message
     * @see CCBLang#translateDirect(String, Object...)
     * @see CCBSoundEvents#DENY
     */
    public static void displayCustomWarningHint(@NotNull Player player, @NotNull String key, Object... args) {
        player.displayClientMessage(CCBLang.translateDirect(key, args).withStyle(ChatFormatting.RED), true);
        CCBSoundEvents.DENY.playOnServer(player.level(), player.blockPosition(), 1, 1);
    }

    /**
     * Changes the gas content of a gas canister by a specified amount.
     * <p>
     * This method increases or decreases the amount of gas in the canister by the specified value.
     * The new amount is clamped between 0 and the canister's maximum capacity for its current gas type.
     * If the canister is empty, no changes are made and 0 is returned.
     * </p>
     *
     * @param canister     the gas canister to modify (cannot be null)
     * @param changeAmount the amount of gas to add (positive) or remove (negative)
     * @return the actual amount of gas that was added or removed (maybe less than requested),
     * or 0 if the canister is invalid, empty, or no change is requested
     * @see GasCanisterQueryUtils#isValidCanister(ItemStack)
     * @see GasCanisterQueryUtils#getCanisterContent(ItemStack)
     * @see GasCanisterQueryUtils#getCanisterCapacity(ItemStack, Gas)
     * @see #setCanisterContent(ItemStack, GasStack)
     */
    public static long changeCanisterContent(@NotNull ItemStack canister, long changeAmount) {
        if (!GasCanisterQueryUtils.isValidCanister(canister) || changeAmount == 0) {
            return 0;
        }

        GasStack content = GasCanisterQueryUtils.getCanisterContent(canister);
        if (content.isEmpty()) {
            return 0;
        }

        Gas contentGas = content.getGas();
        long currentAmount = content.getAmount();
        long newAmount = Mth.clamp(currentAmount + changeAmount, 0, GasCanisterQueryUtils.getCanisterCapacity(canister, contentGas));
        setCanisterContent(canister, new GasStack(contentGas, newAmount));
        return newAmount - currentAmount;
    }

    /**
     * Changes the gas content of a gas canister by a specified amount, with gas type filtering.
     * <p>
     * This method increases or decreases the amount of gas in the canister by the specified value,
     * but only if the canister contains the specified gas type, or if the filter is set to {@code Gas.EMPTY_GAS_HOLDER.value()}.
     * The new amount is clamped between 0 and the canister's maximum capacity.
     * </p>
     * <p>
     * If the canister contains a different gas type than the specified filter (and filter is not empty),
     * no changes are made and 0 is returned.
     * </p>
     *
     * @param canister     the gas canister to modify (cannot be null)
     * @param filterGas    the gas type to filter by; use {@code Gas.EMPTY_GAS_HOLDER.value()} to bypass filtering (cannot be null)
     * @param changeAmount the amount of gas to add (positive) or remove (negative)
     * @return the actual amount of gas that was added or removed (maybe less than requested),
     * or 0 if the canister is invalid, contains a different gas type, or no change is requested
     * @see GasCanisterQueryUtils#isValidCanister(ItemStack)
     * @see GasCanisterQueryUtils#getCanisterContent(ItemStack)
     * @see #changeCanisterContent(ItemStack, long)
     */
    public static long changeCanisterContent(@NotNull ItemStack canister, @NotNull Gas filterGas, long changeAmount) {
        if (!GasCanisterQueryUtils.isValidCanister(canister) || changeAmount == 0) {
            return 0;
        }

        GasStack content = GasCanisterQueryUtils.getCanisterContent(canister);
        return filterGas != Gas.EMPTY_GAS_HOLDER.value() && !content.is(filterGas) ? 0 : changeCanisterContent(canister, changeAmount);

    }

    /**
     * Changes the gas content of canisters within a gas canister pack that match the specified gas type.
     * <p>
     * This method increases or decreases the gas amount in canisters within the pack that contain
     * the specified gas type. The change is applied sequentially across matching canisters in slot order (0 to 3).
     * </p>
     * <p>
     * Special cases:
     * <ul>
     *   <li>If the pack is invalid or change amount is zero, returns 0 without making changes</li>
     *   <li>If the filter gas is {@code Gas.EMPTY_GAS_HOLDER.value()}, returns 0 without making changes</li>
     * </ul>
     * </p>
     *
     * @param pack         the gas canister pack to modify (cannot be null)
     * @param filterGas    the gas type to filter by; only canisters containing this gas will be modified (cannot be null)
     * @param changeAmount the amount of gas to add (positive) or remove (negative) across matching canisters
     * @return the total actual amount of gas that was added or removed across all matching canisters
     * @see GasCanisterQueryUtils#isValidCanisterPack(ItemStack)
     * @see GasCanisterQueryUtils#isValidCanister(ItemStack)
     * @see #changeCanisterContent(ItemStack, long)
     */
    public static long changeCanisterPackContent(@NotNull ItemStack pack, @NotNull Gas filterGas, long changeAmount) {
        if (!GasCanisterQueryUtils.isValidCanisterPack(pack) || changeAmount == 0) {
            return 0;
        }
        if (filterGas == Gas.EMPTY_GAS_HOLDER.value()) {
            return 0;
        }

        UUID uuid = GasCanisterPackUtils.getCanisterPackUUID(pack);
        GasCanisterPackContents contents = CreateCraftedBeginning.GAS_CANISTER_PACK_CONTENTS_DATA_MANAGER.getContents(uuid);
        long remainingAmount = changeAmount;
        for (int slot = 0; slot < 4; slot++) {
            if (remainingAmount == 0) {
                break;
            }

            ItemStack canister = contents.getStackInSlot(slot);
            if (!GasCanisterQueryUtils.isValidCanister(canister)) {
                continue;
            }

            GasStack canisterContent = GasCanisterQueryUtils.getCanisterContent(canister);
            if (!canisterContent.is(filterGas)) {
                continue;
            }

            long changed = changeCanisterContent(canister, remainingAmount);
            if (changed == 0) {
                continue;
            }

            remainingAmount -= changed;
            contents.setStackInSlot(slot, canister);
        }

        return changeAmount - remainingAmount;
    }

    /**
     * Sets the gas content of a gas canister ItemStack.
     * <p>
     * This method updates the gas content of a valid gas canister with the provided gas stack.
     * If the ItemStack is not a valid gas canister, this method does nothing.
     * </p>
     *
     * @param canister the gas canister ItemStack to modify (cannot be null)
     * @param content  the gas stack to set as the new content of the canister (cannot be null)
     * @see GasCanisterQueryUtils#isValidCanister(ItemStack)
     */
    public static void setCanisterContent(@NotNull ItemStack canister, @NotNull GasStack content) {
        if (!GasCanisterQueryUtils.isValidCanister(canister)) {
            return;
        }

        canister.set(CCBDataComponents.CANISTER_CONTENT, content);
    }

    /**
     * Attempts to consume a specified amount of gas from the player's gas suppliers (canisters and packs).
     * <p>
     * This method handles gas consumption with the following conditions:
     * <ul>
     *   <li>Creative mode players always succeed without actual gas consumption</li>
     *   <li>Client-side execution always succeeds without actual gas consumption</li>
     *   <li>Zero or negative consumption amounts always succeed as no-op operations</li>
     * </ul>
     *
     * <p>The method fires a {@link GasConsumptionEvent} before processing, allowing modification
     * or cancellation of the consumption. Gas is consumed from both individual canisters and canister packs
     * that contain the specified gas type, in the order provided by the registered suppliers.</p>
     *
     * @param player          the player whose gas will be consumed (cannot be null)
     * @param targetGas       the type of gas to consume (cannot be null)
     * @param amountToConsume the amount of gas to consume (in units)
     * @param executeSupplier an optional condition that must return true to proceed with consumption
     * @return {@code true} if the gas was successfully consumed (or in special cases),
     * {@code false} if the player has insufficient gas after event processing
     * @see GasConsumptionEvent
     * @see GasCanisterSupplierUtils#getAllGasSuppliers(Player)
     * @see #changeCanisterPackContent(ItemStack, Gas, long)
     * @see #changeCanisterContent(ItemStack, Gas, long)
     */
    public static boolean tryGasConsumption(@NotNull Player player, @NotNull Gas targetGas, long amountToConsume, Supplier<Boolean> executeSupplier) {
        if (player.isCreative() || player.level().isClientSide || amountToConsume <= 0) {
            return true;
        }

        GasConsumptionEvent event = new GasConsumptionEvent(player, targetGas, amountToConsume, executeSupplier);
        NeoForge.EVENT_BUS.post(event);
        long actualAmountToConsume = event.isCanceled() ? 0 : event.getAmount();
        if (GasCanisterQueryUtils.getTotalGasAmount(player, targetGas) < actualAmountToConsume) {
            return false;
        }
        if (executeSupplier != null && !executeSupplier.get()) {
            return true;
        }

        List<ItemStack> gasSuppliers = GasCanisterSupplierUtils.getAllGasSuppliers(player);
        long remainingAmount = actualAmountToConsume;
        for (ItemStack gasSupplier : gasSuppliers) {
            if (remainingAmount == 0) {
                break;
            }

            if (GasCanisterQueryUtils.isValidCanisterPackNonEmpty(gasSupplier)) {
                long consumed = changeCanisterPackContent(gasSupplier, targetGas, -remainingAmount);
                remainingAmount -= consumed;
                continue;
            }

            if (!GasCanisterQueryUtils.isValidCanisterNonEmpty(gasSupplier)) {
                continue;
            }

            long consumed = changeCanisterContent(gasSupplier, targetGas, -remainingAmount);
            remainingAmount -= consumed;
        }

        return true;
    }

    /**
     * Attempts to consume a specified amount of gas from the player's gas suppliers without additional conditions.
     * <p>
     * This is a convenience method that calls the full {@code tryGasConsumption} implementation
     * with a condition supplier that always returns true, allowing unconditional gas consumption
     * if sufficient gas is available.
     * </p>
     *
     * @param player          the player whose gas will be consumed (cannot be null)
     * @param targetGas       the type of gas to consume (cannot be null)
     * @param amountToConsume the amount of gas to consume (in units)
     * @return {@code true} if the gas was successfully consumed, {@code false} if the player has insufficient gas
     * @see #tryGasConsumption(Player, Gas, long, Supplier)
     */
    public static boolean tryGasConsumption(@NotNull Player player, @NotNull Gas targetGas, long amountToConsume) {
        return tryGasConsumption(player, targetGas, amountToConsume, () -> true);
    }
}
