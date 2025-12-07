package net.ty.createcraftedbeginning.api.gas.cansiters;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class GasCanisterSupplierUtils {
    private static final List<Function<Player, List<ItemStack>>> CANISTER_SUPPLIERS = new ArrayList<>();

    static {
        addCanisterSupplier(GasCanisterSupplierUtils::getCanisterPacksFromInventory);
        addCanisterSupplier(GasCanisterSupplierUtils::getCanistersFromInventory);
    }

    private GasCanisterSupplierUtils() {
    }

    /**
     * Registers a new supplier function for gas canisters.
     * <p>
     * This method allows other mods or systems to register custom supplier functions
     * that can provide gas canisters from additional sources beyond the default player inventory.
     * </p>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * {@code
     * GasCanisterSupplierUtils.addCanisterSupplier(player -> {
     *     // Return gas canisters from your custom storage system
     *     return getCanistersFromCustomStorage(player);
     * });
     * }
     * </pre>
     *
     * <p><b>Implementation Notes:</b></p>
     * <ul>
     *   <li>Supplier functions are called in the order they were registered</li>
     *   <li>Each supplier should return a list of valid gas pack ItemStacks</li>
     *   <li>The returned ItemStacks will be automatically filtered for valid canisters</li>
     *   <li>Suppliers should handle null players appropriately if needed</li>
     * </ul>
     *
     * <p><b>Thread Safety:</b> This method is not thread-safe and should be called during mod initialization.</p>
     *
     * @param supplier the function that provides gas canisters from a specific source (cannot be null).
     *                 The function should accept a Player parameter and return a List of ItemStacks.
     * @throws NullPointerException if the supplied function is null
     */
    public static void addCanisterSupplier(Function<Player, List<ItemStack>> supplier) {
        CANISTER_SUPPLIERS.add(supplier);
    }

    /**
     * Retrieves all valid gas canisters from the player's inventory, avoiding duplicate items.
     * <p>
     * This method searches the player's offhand, and inventory slots for valid gas canisters.
     * It ensures that the same physical item content is not added multiple times by checking for item identity
     * between the main offhand items and inventory items.
     * </p>
     *
     * @param player the player whose inventory will be searched (cannot be null)
     * @return a list containing all unique valid gas canisters from the player's inventory (never null)
     * @see GasCanisterQueryUtils#isValidCanister(ItemStack)
     * @see ItemStack#isSameItem(ItemStack, ItemStack)
     */
    public static @NotNull List<ItemStack> getCanistersFromInventory(@NotNull Player player) {
        List<ItemStack> canisters = new ArrayList<>();

        ItemStack offHandItem = player.getOffhandItem();
        if (GasCanisterQueryUtils.isValidCanister(offHandItem)) {
            canisters.add(offHandItem);
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (GasCanisterQueryUtils.isValidCanister(item) && !ItemStack.isSameItem(offHandItem, item)) {
                canisters.add(item);
            }
        }
        return canisters;
    }

    /**
     * Retrieves all gas canister packs from the player's inventory, avoiding duplicate items.
     * <p>
     * This method searches the player's main hand, offhand, and all inventory slots for
     * gas canister packs. It ensures that the same physical item stack is not processed
     * multiple times by checking for item identity between slots.
     * </p>
     *
     * @param player the player whose inventory will be searched (cannot be null)
     * @return a list containing all unique gas canister packs found in the player's inventory (never null)
     * @see GasCanisterQueryUtils#isValidCanisterPack(ItemStack)
     * @see ItemStack#isSameItem(ItemStack, ItemStack)
     */
    public static @NotNull List<ItemStack> getCanisterPacksFromInventory(@NotNull Player player) {
        List<ItemStack> packs = new ArrayList<>();

        ItemStack mainHandItem = player.getMainHandItem();
        if (GasCanisterQueryUtils.isValidCanisterPack(mainHandItem)) {
            packs.add(mainHandItem);
        }

        ItemStack offHandItem = player.getOffhandItem();
        if (GasCanisterQueryUtils.isValidCanisterPack(offHandItem) && !ItemStack.isSameItem(mainHandItem, offHandItem)) {
            packs.add(offHandItem);
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (GasCanisterQueryUtils.isValidCanisterPack(item) && !ItemStack.isSameItem(mainHandItem, item) && !ItemStack.isSameItem(offHandItem, item)) {
                packs.add(item);
            }
        }
        return packs;
    }

    /**
     * Retrieves all gas suppliers (gas canisters packs and gas canister) from all registered suppliers for the specified player.
     * <p>
     * This method combines items from all registered supplier functions and filters
     * the results to include only valid gas suppliers. The returned list is unmodifiable
     * and contains both individual gas canisters and gas canister packs that can provide gas.
     * </p>
     *
     * @param player the player to retrieve gas suppliers for (cannot be null)
     * @return an unmodifiable list containing all valid gas suppliers (gas canisters packs and gas canister)
     * from all registered suppliers (never null)
     * @see GasCanisterQueryUtils#isValidCanisterPack(ItemStack)
     * @see GasCanisterQueryUtils#isValidCanister(ItemStack)
     */
    public static @NotNull @Unmodifiable List<ItemStack> getAllGasSuppliers(@NotNull Player player) {
        return CANISTER_SUPPLIERS.stream().flatMap(supplier -> supplier.apply(player).stream()).filter(stack -> GasCanisterQueryUtils.isValidCanisterPack(stack) || GasCanisterQueryUtils.isValidCanister(stack)).toList();
    }

    /**
     * Retrieves the first non-empty gas supplier (gas canister pack or gas canister) from all registered suppliers.
     * <p>
     * This method searches through all items provided by registered supplier functions and returns
     * the first valid gas supplier that contains gas. The search order follows the registration order of the suppliers.
     * </p>
     *
     * @param player the player to search for gas suppliers (can be null)
     * @return the first valid non-empty gas supplier found, or {@link ItemStack#EMPTY} if no non-empty suppliers are found (never null)
     * @see GasCanisterQueryUtils#isValidCanisterPackNonEmpty(ItemStack)
     * @see GasCanisterQueryUtils#isValidCanisterNonEmpty(ItemStack)
     */
    public static @NotNull ItemStack getFirstNonEmptyGasSupplier(@Nullable Player player) {
        return CANISTER_SUPPLIERS.stream().flatMap(supplier -> supplier.apply(player).stream()).filter(stack -> GasCanisterQueryUtils.isValidCanisterPackNonEmpty(stack) || GasCanisterQueryUtils.isValidCanisterNonEmpty(stack)).findFirst().orElse(ItemStack.EMPTY);
    }

    /**
     * Retrieves the gas content from the first non-empty gas supplier found in the player's inventory.
     * <p>
     * This method searches for the first gas supplier (gas canister pack or gas canister) that contains gas
     * and returns its gas content. For canister packs, it returns the gas content of the first
     * non-empty canister in the pack. If no non-empty gas supplier is found, an empty GasStack is returned.
     * </p>
     *
     * @param player the player to search for gas suppliers (can be null)
     * @return the gas content from the first non-empty gas supplier found,
     * or {@link GasStack#EMPTY} if no non-empty suppliers are found (never null)
     * @see #getFirstNonEmptyGasSupplier(Player)
     * @see GasCanisterQueryUtils#isValidCanisterPackNonEmpty(ItemStack)
     * @see GasCanisterQueryUtils#getCanisterPackContent(ItemStack)
     * @see GasCanisterQueryUtils#isValidCanisterNonEmpty(ItemStack)
     * @see GasCanisterQueryUtils#getCanisterContent(ItemStack)
     */
    public static @NotNull GasStack getFirstNonEmptyGasContent(@Nullable Player player) {
        ItemStack gasSupplier = getFirstNonEmptyGasSupplier(player);
        if (GasCanisterQueryUtils.isValidCanisterPackNonEmpty(gasSupplier)) {
            List<GasStack> contents = GasCanisterQueryUtils.getCanisterPackContent(gasSupplier);
            if (!contents.isEmpty()) {
                return contents.getFirst();
            }
        }

        return GasCanisterQueryUtils.isValidCanisterNonEmpty(gasSupplier) ? GasCanisterQueryUtils.getCanisterContent(gasSupplier) : GasStack.EMPTY;

    }

    /**
     * Checks if the player has no usable gas available in any of their gas suppliers.
     * <p>
     * This method searches through all gas suppliers (gas canister packs and gas canisters) in the player's inventory
     * and checks if the first valid supplier contains any gas. It returns true if no gas is found,
     * indicating that the player has no usable gas available in any of their gas suppliers.
     * </p>
     *
     * @param player the player to check for usable gas (cannot be null)
     * @return {@code true} if no usable gas is found in any gas supplier,
     * {@code false} if at least one gas supplier contains gas
     * @see #getFirstNonEmptyGasContent(Player)
     */
    public static boolean noUsableGasAvailable(@NotNull Player player) {
        return getFirstNonEmptyGasContent(player).isEmpty();
    }

    /**
     * Creates a GasStack representing the total amount of the first found gas type across all the player's gas suppliers.
     * <p>
     * This method first identifies the gas type from the first non-empty gas supplier in the player's inventory,
     * then calculates the total amount of that specific gas type across all gas suppliers and returns it
     * as a single GasStack. If no gas is found in any supplier, an empty GasStack is returned.
     * </p>
     *
     * @param player the player whose gas suppliers will be checked (cannot be null)
     * @return a GasStack containing the first found gas type with the total amount from all suppliers,
     * or {@link GasStack#EMPTY} if no gas is found in any supplier (never null)
     * @see #getFirstNonEmptyGasContent(Player)
     * @see GasCanisterQueryUtils#getTotalGasAmount(Player, Gas)
     */
    public static @NotNull GasStack getTotalGasStack(@NotNull Player player) {
        GasStack gasStack = getFirstNonEmptyGasContent(player);
        if (gasStack.isEmpty()) {
            return GasStack.EMPTY;
        }

        Gas filterGas = gasStack.getGas();
        long amount = GasCanisterQueryUtils.getTotalGasAmount(player, filterGas);
        return amount == 0 ? GasStack.EMPTY : new GasStack(filterGas, amount);

    }
}
