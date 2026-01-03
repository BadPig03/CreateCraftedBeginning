package net.ty.createcraftedbeginning.api.gas.cansiters;

import com.simibubi.create.AllEnchantments;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterItem;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackItem;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackUtils;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.IntStream;

public class GasCanisterQueryUtils {
    private static final long MILLIBUCKETS_PER_BUCKET = 1000L;

    private GasCanisterQueryUtils() {
    }

    /**
     * Checks if the given ItemStack is a valid gas canister.
     * <p>
     * A valid gas canister is defined as either:
     * <ul>
     *   <li>The specific gas canister item registered in {@link CCBItems#GAS_CANISTER}</li>
     *   <li>Any item that is an instance of {@link GasCanisterItem}</li>
     * </ul>
     * This allows for both the default canister and any custom canister implementations to be recognized.
     * </p>
     *
     * @param canister the ItemStack to check for validity (cannot be null)
     * @return {@code true} if the ItemStack is a valid gas canister (not empty and matches the criteria),
     * {@code false} otherwise
     */
    public static boolean isValidCanister(@NotNull ItemStack canister) {
        return !canister.isEmpty() && (canister.is(CCBItems.GAS_CANISTER) || canister.getItem() instanceof GasCanisterItem);
    }

    /**
     * Checks if the given ItemStack is a valid gas canister pack.
     * <p>
     * A valid gas canister pack is defined as either:
     * <ul>
     *   <li>The specific gas canister pack item registered in {@link CCBItems#GAS_CANISTER_PACK}</li>
     *   <li>Any item that is an instance of {@link GasCanisterPackItem}</li>
     * </ul>
     * This allows for both the default canister pack and any custom canister pack implementations to be recognized.
     * </p>
     *
     * @param pack the ItemStack to check for validity (cannot be null)
     * @return {@code true} if the ItemStack is a valid gas canister pack (not empty and matches the criteria),
     * {@code false} otherwise
     */
    public static boolean isValidCanisterPack(@NotNull ItemStack pack) {
        return !pack.isEmpty() && (pack.is(CCBItems.GAS_CANISTER_PACK) || pack.getItem() instanceof GasCanisterPackItem);
    }

    /**
     * Checks if the ItemStack is a valid gas canister that contains gas.
     * <p>
     * This method verifies that the ItemStack is both a valid gas canister and
     * contains a non-zero amount of gas, regardless of the gas type.
     * </p>
     *
     * @param canister the ItemStack to check (cannot be null)
     * @return {@code true} if the ItemStack is a valid gas canister and contains gas,
     * {@code false} if the canister is invalid or contains no gas
     * @see #isValidCanister(ItemStack)
     * @see #getGasAmount(ItemStack, Gas)
     */
    public static boolean isValidCanisterNonEmpty(@NotNull ItemStack canister) {
        return isValidCanister(canister) && getGasAmount(canister, Gas.EMPTY_GAS_HOLDER.value()) > 0;
    }

    /**
     * Retrieves the gas canister from a specific slot in a gas canister pack.
     * <p>
     * This method accesses the specified slot (0-3) of a gas canister pack and returns
     * the canister contained in that slot by reading from the pack's inventory data component.
     * If the provided ItemStack is not a valid gas canister pack or the slot index is out of range,
     * an empty ItemStack is returned.
     * </p>
     *
     * @param pack the gas canister pack ItemStack to retrieve from (cannot be null)
     * @param slot the slot index to retrieve from (0-3)
     * @return the gas canister in the specified slot, or {@link ItemStack#EMPTY} if the
     * pack is invalid or the slot index is out of range
     * @see #isValidCanisterPack(ItemStack)
     */
    public static ItemStack getCanisterInPack(@NotNull ItemStack pack, int slot) {
        if (!isValidCanisterPack(pack)) {
            return ItemStack.EMPTY;
        }

        if (slot < 0 || slot >= 4) {
            return ItemStack.EMPTY;
        }

        UUID uuid = GasCanisterPackUtils.getCanisterPackUUID(pack);
        GasCanisterPackContents contents = CreateCraftedBeginning.GAS_CANISTER_PACK_CONTENTS_DATA_MANAGER.getContents(uuid);
        return contents.getStackInSlot(slot);
    }

    /**
     * Checks if the ItemStack is a valid gas canister pack that contains gas.
     * <p>
     * This method verifies that the ItemStack is both a valid gas canister pack and
     * contains a non-zero amount of gas in any of its canisters, regardless of the gas type.
     * </p>
     *
     * @param pack the ItemStack to check (cannot be null)
     * @return {@code true} if the ItemStack is a valid gas canister pack and contains gas in any canister,
     * {@code false} if the pack is invalid or contains no gas in any canister
     * @see #isValidCanisterPack(ItemStack)
     * @see #getGasAmount(ItemStack, Gas)
     */
    public static boolean isValidCanisterPackNonEmpty(@NotNull ItemStack pack) {
        return isValidCanisterPack(pack) && getGasAmount(pack, Gas.EMPTY_GAS_HOLDER.value()) > 0;
    }

    /**
     * Retrieves all valid, non-empty gas canisters from a gas canister pack.
     * <p>
     * This method searches through all 4 slots (0-3) of a gas canister pack and collects
     * all valid gas canisters that contain gas. The returned list is unmodifiable and
     * contains only canisters that are both valid and non-empty.
     * </p>
     *
     * @param pack the gas canister pack ItemStack to search (cannot be null)
     * @return an unmodifiable list containing all valid, non-empty gas canisters found in the pack's slots (never null)
     * @see #getCanisterInPack(ItemStack, int)
     * @see #isValidCanisterNonEmpty(ItemStack)
     */
    public static @NotNull @Unmodifiable List<ItemStack> getNonEmptyCanistersFromPack(@NotNull ItemStack pack) {
        return IntStream.rangeClosed(0, 3).mapToObj(slot -> getCanisterInPack(pack, slot)).filter(GasCanisterQueryUtils::isValidCanisterNonEmpty).toList();
    }

    /**
     * Retrieves the gas content from a gas canister ItemStack.
     * <p>
     * This method extracts the gas stack stored in the canister's data components.
     * If the provided ItemStack is not a valid gas canister, an empty gas stack is returned.
     * </p>
     *
     * @param canister the gas canister ItemStack to retrieve gas content from (cannot be null)
     * @return the gas stack contained in the canister, or {@link GasStack#EMPTY} if the
     * ItemStack is not a valid gas canister (never null)
     * @see #isValidCanister(ItemStack)
     */
    public static @NotNull GasStack getCanisterContent(@NotNull ItemStack canister) {
        return isValidCanister(canister) ? canister.getOrDefault(CCBDataComponents.CANISTER_CONTENT, GasStack.EMPTY) : GasStack.EMPTY;
    }

    /**
     * Retrieves the gas content from all non-empty canisters in a gas canister pack.
     * <p>
     * This method extracts the gas stacks from all valid, non-empty gas canisters contained
     * within a gas canister pack. The returned list is unmodifiable and contains only the
     * gas content from canisters that actually contain gas.
     * </p>
     *
     * @param pack the gas canister pack ItemStack to retrieve gas content from (cannot be null)
     * @return an unmodifiable list of gas stacks from all non-empty canisters in the pack,
     * or an empty list if the pack is invalid or contains no non-empty canisters (never null)
     * @see #isValidCanisterPack(ItemStack)
     * @see #getNonEmptyCanistersFromPack(ItemStack)
     * @see #getCanisterContent(ItemStack)
     */
    public static @NotNull @Unmodifiable List<GasStack> getCanisterPackContent(@NotNull ItemStack pack) {
        return isValidCanisterPack(pack) ? getNonEmptyCanistersFromPack(pack).stream().map(GasCanisterQueryUtils::getCanisterContent).toList() : List.of();
    }

    /**
     * Retrieves the total amount of gas from a gas supplier (canister or canister pack), filtered by gas type.
     * <p>
     * This method handles both individual gas canisters and gas canister packs:
     * <ul>
     *   <li>For canister packs: sums the gas amounts from all canisters that match the filter gas type</li>
     *   <li>For individual canisters: returns the gas amount if it matches the filter gas type</li>
     * </ul>
     * If the filter is empty gas, all gas types will be counted (no filtering).
     * </p>
     *
     * @param gasSupplier the gas supplier (canister or canister pack) to check
     * @param filterGas   the specific gas type to filter by; use empty gas to count all gas types (cannot be null)
     * @return the total amount of gas in the supplier that matches the specified gas type
     * @see #isValidCanisterPack(ItemStack)
     * @see #getCanisterPackContent(ItemStack)
     * @see #isValidCanister(ItemStack)
     * @see #getCanisterContent(ItemStack)
     */
    public static long getGasAmount(ItemStack gasSupplier, @NotNull Gas filterGas) {
        if (isValidCanisterPack(gasSupplier)) {
            return getCanisterPackContent(gasSupplier).stream().filter(gasStack -> filterGas.isEmpty() || gasStack.is(filterGas)).mapToLong(GasStack::getAmount).sum();
        }

        if (isValidCanister(gasSupplier)) {
            GasStack content = getCanisterContent(gasSupplier);
            if (filterGas.isEmpty() || content.is(filterGas)) {
                return content.getAmount();
            }
        }

        return 0;
    }

    /**
     * Retrieves the maximum gas capacity of a gas canister, optionally filtered by gas type.
     * <p>
     * The capacity is calculated based on the canister's base capacity and any "Capacity" enchantments.
     * If the canister contains a gas that does not match the specified filter (and the filter is not empty),
     * the method returns 0. This allows querying capacity only for specific gas types.
     * </p>
     *
     * @param canister  the gas canister ItemStack to check (cannot be null)
     * @param filterGas the gas type to filter by; use an empty gas to ignore filtering (cannot be null)
     * @return the maximum gas capacity of the canister in millibuckets, or 0 if the ItemStack
     * is not a valid canister or its contained gas does not match the filter
     * @see #isValidCanister(ItemStack)
     * @see #getCanisterContent(ItemStack)
     */
    public static long getCanisterCapacity(@NotNull ItemStack canister, @NotNull Gas filterGas) {
        if (!isValidCanister(canister)) {
            return 0;
        }

        GasStack content = getCanisterContent(canister);
        if (!content.isEmpty() && !filterGas.isEmpty() && !content.is(filterGas)) {
            return 0;
        }

        int enchantLevel = canister.getTagEnchantments().entrySet().stream().filter(entry -> entry.getKey().is(AllEnchantments.CAPACITY)).findFirst().map(Entry::getValue).orElse(0);
        return CCBConfig.server().gas.canisterCapacity.get() * MILLIBUCKETS_PER_BUCKET * (1 + enchantLevel);
    }

    /**
     * Retrieves the total gas capacity of a gas canister pack, filtered by gas type.
     * <p>
     * This method calculates the combined maximum gas capacity of all valid gas canisters
     * contained within the canister pack that match the specified gas type. Only non-empty
     * canisters containing the specified gas type (or all types if using the empty gas
     * as the filter gas) are considered for the capacity calculation.
     * </p>
     *
     * @param pack      the gas canister pack ItemStack to check (cannot be null)
     * @param filterGas the gas type to filter by; use the empty gas to count capacity for all gas types (cannot be null)
     * @return the total gas capacity of all matching canisters in the pack in millibuckets,
     * or 0 if the ItemStack is not a valid canister pack
     * @see #isValidCanisterPack(ItemStack)
     * @see #getNonEmptyCanistersFromPack(ItemStack)
     * @see #getCanisterCapacity(ItemStack, Gas)
     */
    public static long getCanisterPackCapacity(@NotNull ItemStack pack, @NotNull Gas filterGas) {
        return isValidCanisterPack(pack) ? getNonEmptyCanistersFromPack(pack).stream().mapToLong(canister -> getCanisterCapacity(canister, filterGas)).sum() : 0;
    }

    /**
     * Retrieves the total gas capacity of a gas supplier (canister or canister pack), filtered by gas type.
     * <p>
     * This method determines the appropriate capacity calculation based on the type of gas supplier:
     * <ul>
     *   <li>For canister packs: returns the total capacity of all canisters matching the filter gas type</li>
     *   <li>For individual canisters: returns the capacity if it matches the filter gas type</li>
     * </ul>
     * The capacity is only calculated for canisters containing the specified gas type, or all types if using the empty gas.
     * </p>
     *
     * @param gasSupplier the gas supplier (canister or canister pack) to check (cannot be null)
     * @param filterGas   the gas type to filter by; use the empty gas to count capacity for all gas types (cannot be null)
     * @return the total gas capacity of the supplier in millibuckets, or 0 if the ItemStack is not a valid gas supplier
     * @see #isValidCanisterPack(ItemStack)
     * @see #getCanisterPackCapacity(ItemStack, Gas)
     * @see #isValidCanister(ItemStack)
     * @see #getCanisterCapacity(ItemStack, Gas)
     */
    public static long getGasCapacity(@NotNull ItemStack gasSupplier, @NotNull Gas filterGas) {
        if (isValidCanisterPack(gasSupplier)) {
            return getCanisterPackCapacity(gasSupplier, filterGas);
        }

        return isValidCanister(gasSupplier) ? getCanisterCapacity(gasSupplier, filterGas) : 0;
    }

    /**
     * Calculates the total amount of a specific gas type across all the player's non-empty gas suppliers.
     * <p>
     * This method searches through all non-empty gas suppliers (gas canisters and gas canister packs)
     * in the player's inventory and sums up the amount of the given gas type. The calculation
     * only includes suppliers that actually contain gas.
     * </p>
     *
     * @param player    the player whose gas suppliers will be checked (cannot be null)
     * @param filterGas the specific gas type to calculate the total amount for (cannot be null)
     * @return the total amount of the specified gas type across all non-empty gas suppliers,
     * or 0 if the player has no non-empty gas suppliers
     * @see GasCanisterSupplierUtils#getAllGasSuppliersNonEmpty(Player)
     * @see #getGasAmount(ItemStack, Gas)
     */
    public static long getTotalGasAmount(@NotNull Player player, @NotNull Gas filterGas) {
        List<ItemStack> gasSuppliers = GasCanisterSupplierUtils.getAllGasSuppliersNonEmpty(player);
        return gasSuppliers.isEmpty() ? 0 : gasSuppliers.stream().mapToLong(gasSupplier -> getGasAmount(gasSupplier, filterGas)).sum();
    }

    /**
     * Calculates the total gas capacity for a specific gas type across all the player's non-empty gas suppliers.
     * <p>
     * This method searches through all non-empty gas suppliers (gas canisters and gas canister packs)
     * in the player's inventory and sums up the storage capacity for the specified gas type.
     * The capacity is only calculated for suppliers that contain the specified gas type
     * (or all types if using the empty gas placeholder) and are non-empty.
     * </p>
     *
     * @param player    the player whose gas suppliers will be checked (cannot be null)
     * @param filterGas the specific gas type to calculate the capacity for;
     *                  use {@link Gas#EMPTY_GAS_HOLDER} to count capacity for all gas types (cannot be null)
     * @return the total storage capacity available for the specified gas type across all compatible non-empty gas suppliers
     * @see GasCanisterSupplierUtils#getAllGasSuppliersNonEmpty(Player)
     * @see #getGasCapacity(ItemStack, Gas)
     */
    public static long getTotalGasCapacity(@NotNull Player player, @NotNull Gas filterGas) {
        List<ItemStack> gasSuppliers = GasCanisterSupplierUtils.getAllGasSuppliersNonEmpty(player);
        return gasSuppliers.isEmpty() ? 0 : gasSuppliers.stream().mapToLong(gasSupplier -> getGasCapacity(gasSupplier, filterGas)).sum();
    }

    /**
     * Calculates the ratio of the total amount of a specific gas type to the total storage capacity
     * for that gas across all the player's gas suppliers.
     * <p>
     * This method computes the fill ratio of the specified gas type by dividing the total gas amount
     * by the total storage capacity. The returned value is clamped between 0.0 and 1.0 to represent
     * a valid percentage ratio. If the total capacity is zero, the method returns 0.0.
     * </p>
     *
     * @param player    the player whose gas suppliers will be checked (cannot be null)
     * @param filterGas the specific gas type to calculate the ratio for (cannot be null)
     * @return a value between 0.0 and 1.0 representing the fill ratio (0 = empty, 1 = full capacity)
     * @see #getTotalGasAmount(Player, Gas)
     * @see #getTotalGasCapacity(Player, Gas)
     */
    public static float getTotalGasRatio(@NotNull Player player, @NotNull Gas filterGas) {
        long capacity = getTotalGasCapacity(player, filterGas);
        if (capacity == 0) {
            return 0;
        }

        return Mth.clamp((float) getTotalGasAmount(player, filterGas) / capacity, 0, 1);
    }

    /**
     * Determines if a gas canister can accept injection of the specified gas.
     * <p>
     * A canister is injectable if it meets all the following conditions:
     * <ul>
     *   <li>The canister is a valid gas canister</li>
     *   <li>Either the canister is empty, or it contains the same type of gas as the source gas</li>
     *   <li>The canister is not at maximum capacity for the gas type</li>
     * </ul>
     * </p>
     *
     * @param canister       the gas canister to check
     * @param sourceGasStack the gas to be injected (cannot be null)
     * @return {@code true} if the canister can accept the gas, {@code false} otherwise
     * @see #isValidCanister(ItemStack)
     * @see #getCanisterContent(ItemStack)
     * @see #getCanisterCapacity(ItemStack, Gas)
     * @see GasStack#isEmpty()
     * @see GasStack#isSameGas(GasStack, GasStack)
     */
    public static boolean isCanisterInjectable(ItemStack canister, GasStack sourceGasStack) {
        if (!isValidCanister(canister)) {
            return false;
        }

        GasStack content = getCanisterContent(canister);
        return content.isEmpty() || GasStack.isSameGas(content, sourceGasStack) && content.getAmount() < getCanisterCapacity(canister, sourceGasStack.getGas());
    }
}
