package net.ty.createcraftedbeginning.api.gas.cansiters;

import net.createmod.catnip.data.Pair;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterItem;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterItem;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackContainerContents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class CanisterContainerSuppliers {
    private static final List<Function<Player, List<IGasCanisterContainer>>> CANISTER_CONTAINER_SUPPLIERS = new ArrayList<>();

    static {
        addCanisterContainerSuppliers(CanisterContainerSuppliers::getCanisterContainersInInventory);
    }

    private CanisterContainerSuppliers() {
    }

    /**
     * Registers a new supplier function for gas canister containers.
     * <p>
     * The registered supplier function will be called when retrieving gas canister containers
     * for a player. This allows for extending the sources of gas canister containers beyond
     * the default inventory search.
     * </p>
     * <p>
     * Supplier functions should return a list of {@link IGasCanisterContainer} instances
     * available to the specified player.
     * </p>
     *
     * @param supplier the function that provides a list of gas canister containers for a player
     * @see #getAllSuppliers(Player)
     * @see #CANISTER_CONTAINER_SUPPLIERS
     */
    public static void addCanisterContainerSuppliers(Function<Player, List<IGasCanisterContainer>> supplier) {
        CANISTER_CONTAINER_SUPPLIERS.add(supplier);
    }

    /**
     * Retrieves all valid gas canister containers from the player's inventory.
     * <p>
     * This method searches through the player's entire inventory, including the offhand slot,
     * to find all items that are valid gas canister containers. It checks both the offhand slot
     * and all main inventory slots, ensuring that duplicate items (same item and components)
     * are not added multiple times.
     * </p>
     * <p>
     * The returned list is sorted by container priority in descending order (highest priority first).
     * This ensures that higher priority containers are processed before lower priority ones.
     * </p>
     *
     * @param player the player whose inventory will be searched (must not be null)
     * @return a sorted list of gas canister containers found in the player's inventory,
     * sorted by priority in descending order
     * @see #isValidCanisterContainer(ItemStack)
     * @see IGasCanisterContainer#getPriority()
     */
    public static @NotNull List<IGasCanisterContainer> getCanisterContainersInInventory(@NotNull Player player) {
        List<IGasCanisterContainer> containers = new ArrayList<>();
        ItemStack offHandItem = player.getOffhandItem();
        if (isValidCanisterContainer(offHandItem)) {
            containers.add(offHandItem.getCapability(GasHandler.ITEM));
        }

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (!isValidCanisterContainer(item) || ItemStack.isSameItemSameComponents(offHandItem, item)) {
                continue;
            }

            containers.add(item.getCapability(GasHandler.ITEM));
        }

        containers.sort((c1, c2) -> Integer.compare(c2.getPriority(), c1.getPriority()));
        return containers;
    }

    /**
     * Checks if an ItemStack represents a valid, non-empty gas canister container.
     * <p>
     * This method determines validity by first checking if the ItemStack is not empty,
     * and then verifying if it has the gas handler capability. A valid gas canister
     * container must be a non-empty item that implements the gas handling functionality.
     * </p>
     *
     * @param itemStack the ItemStack to check for gas canister container capability (must not be null)
     * @return true if the ItemStack is non-empty and has gas handler capability, false otherwise
     */
    public static boolean isValidCanisterContainer(@NotNull ItemStack itemStack) {
        return !itemStack.isEmpty() && itemStack.getCapability(GasHandler.ITEM) != null;
    }

    /**
     * Checks if an ItemStack represents a valid gas canister item.
     * <p>
     * This method determines if the ItemStack is both a valid gas canister container
     * and specifically a gas canister item by checking if it matches the registered
     * gas canister item or is an instance of {@link GasCanisterItem}.
     * </p>
     * <p>
     * This is more specific than {@link #isValidCanisterContainer(ItemStack)} as it
     * verifies the item is actually a gas canister rather than just having gas container capabilities.
     * </p>
     *
     * @param itemStack the ItemStack to check for gas canister validity (must not be null)
     * @return true if the ItemStack is a valid gas canister item, false otherwise
     * @see #isValidCanisterContainer(ItemStack)
     */
    public static boolean isValidGasCanister(@NotNull ItemStack itemStack) {
        return isValidCanisterContainer(itemStack) && (itemStack.is(CCBItems.GAS_CANISTER) || itemStack.getItem() instanceof GasCanisterItem);
    }

    /**
     * Checks if an ItemStack represents a valid creative gas canister item.
     * <p>
     * This method determines if the ItemStack is both a valid gas canister container
     * and specifically a creative gas canister item by checking if it matches the registered
     * creative gas canister item or is an instance of {@link CreativeGasCanisterItem}.
     * </p>
     * <p>
     * This is a specialized version of {@link #isValidGasCanister(ItemStack)} that specifically
     * checks for creative variants of gas canisters, which typically have unlimited capacity
     * or other creative-mode properties.
     * </p>
     *
     * @param itemStack the ItemStack to check for creative gas canister validity (must not be null)
     * @return true if the ItemStack is a valid creative gas canister item, false otherwise
     * @see #isValidCanisterContainer(ItemStack)
     * @see #isValidGasCanister(ItemStack)
     */
    public static boolean isValidCreativeGasCanister(@NotNull ItemStack itemStack) {
        return isValidCanisterContainer(itemStack) && (itemStack.is(CCBItems.CREATIVE_GAS_CANISTER) || itemStack.getItem() instanceof CreativeGasCanisterItem);
    }

    /**
     * Retrieves all gas canister containers from all registered suppliers for the specified player.
     * <p>
     * This method collects gas canister containers by applying all registered supplier functions
     * to the player, then combines and returns the results as an unmodifiable list.
     * The suppliers are called in the order they were registered.
     * </p>
     *
     * @param player the player to get canister containers for (must not be null)
     * @return an unmodifiable list containing all gas canister containers from all registered suppliers
     * @see #CANISTER_CONTAINER_SUPPLIERS
     * @see #addCanisterContainerSuppliers(Function)
     */
    public static @NotNull @Unmodifiable List<IGasCanisterContainer> getAllSuppliers(@NotNull Player player) {
        return CANISTER_CONTAINER_SUPPLIERS.stream().flatMap(supplier -> supplier.apply(player).stream()).toList();
    }

    /**
     * Retrieves the first available gas canister container supplier for the player.
     * <p>
     * This method collects all gas canister containers from all registered suppliers
     * and returns the first one in the list. If no containers are available from any
     * supplier, returns null.
     * </p>
     * <p>
     * The order of containers is determined by the order of supplier registration and
     * the internal sorting logic of each supplier (e.g., priority-based sorting).
     * </p>
     *
     * @param player the player to get the first gas canister container for
     * @return the first gas canister container from all suppliers, or null if no containers are available
     * @see #getAllSuppliers(Player)
     */
    @Nullable
    public static IGasCanisterContainer getFirstCanisterSupplier(Player player) {
        List<IGasCanisterContainer> suppliers = getAllSuppliers(player);
        if (suppliers.isEmpty()) {
            return null;
        }

        return suppliers.getFirst();
    }

    /**
     * Retrieves the first available gas content from the player's gas canister containers.
     * <p>
     * This method searches through the player's available gas canister containers and returns
     * the first non-empty gas stack found. It handles both individual gas canisters and
     * gas canister packs, searching through all slots in a pack to find the first non-empty gas.
     * </p>
     * <p>
     * For gas canister packs, it iterates through all slots until it finds a non-empty gas stack.
     * For individual canisters, it returns the gas content of the first tank if not empty.
     * </p>
     *
     * @param player the player whose gas canister containers will be searched
     * @return the first non-empty gas stack found, or {@link GasStack#EMPTY} if no gas is available
     * @see #getFirstCanisterSupplier(Player)
     * @see GasCanisterPackContainerContents
     * @see GasCanisterContainerContents
     */
    public static @NotNull GasStack getFirstAvailableGasContent(Player player) {
        IGasCanisterContainer container = getFirstCanisterSupplier(player);
        return switch (container) {
            case GasCanisterPackContainerContents packContents -> {
                if (packContents.isEmpty()) {
                    yield GasStack.EMPTY;
                }

                for (int i = 0; i < GasCanisterPackContainerContents.MAX_COUNT; i++) {
                    if (packContents.isEmpty(i)) {
                        continue;
                    }

                    yield packContents.getGasInTank(i);
                }

                yield GasStack.EMPTY;
            }
            case GasCanisterContainerContents canisterContents -> {
                if (container.isEmpty()) {
                    yield GasStack.EMPTY;
                }

                yield canisterContents.getGasInTank(0);
            }
            case null, default -> GasStack.EMPTY;
        };
    }

    /**
     * Checks if the player has any gas canister containers available from all registered suppliers.
     * <p>
     * This method determines whether there are any gas canister containers accessible to the player
     * by checking all registered container suppliers. It returns true if at least one container
     * is available, regardless of its content or state.
     * </p>
     *
     * @param player the player to check for available gas canister containers
     * @return true if the player has at least one gas canister container available, false otherwise
     * @see #getAllSuppliers(Player)
     */
    public static boolean isAnyContainerAvailable(Player player) {
        List<IGasCanisterContainer> suppliers = getAllSuppliers(player);
        return !suppliers.isEmpty();
    }

    /**
     * Retrieves the gas content and capacity information from the first available gas canister container.
     * <p>
     * This method attempts to find the first available gas canister container for the player and returns
     * a pair containing the gas stack and capacity information. The method handles different container
     * types including gas canister packs and individual canisters.
     * </p>
     * <p>
     * For gas canister packs, it returns the first non-empty gas pair from the pack.
     * For individual canisters, it returns the gas content and capacity of the first tank.
     * If no container is found or the container type is not recognized, returns an empty pair.
     * </p>
     *
     * @param player the player whose first gas canister container will be checked
     * @return a pair containing the gas stack and capacity of the first available container,
     * or an empty pair (GasStack.EMPTY, 0L) if no container is available
     * @see #getFirstCanisterSupplier(Player)
     * @see GasCanisterPackContainerContents#getFirstNonEmptyPair()
     * @see GasCanisterContainerContents#getGasInTank(int)
     * @see GasCanisterContainerContents#getTankCapacity(int)
     */
    public static @NotNull Pair<GasStack, Long> getFirstCanisterSupplierPair(Player player) {
        IGasCanisterContainer container = getFirstCanisterSupplier(player);
        return switch (container) {
            case GasCanisterPackContainerContents packContents -> packContents.getFirstNonEmptyPair();
            case GasCanisterContainerContents canisterContents -> Pair.of(canisterContents.getGasInTank(0), canisterContents.getTankCapacity(0));
            case null, default -> Pair.of(GasStack.EMPTY, 0L);
        };
    }

    /**
     * Calculates the fill ratio of the first available gas canister container for the player.
     * <p>
     * This method retrieves the first available gas canister container (either a canister pack
     * or an individual canister) and calculates the ratio of current gas amount to maximum capacity.
     * The ratio is clamped between 0.0 and 1.0 to represent empty (0) to full (1) states.
     * </p>
     *
     * @param player the player whose first gas canister container will be checked
     * @return the fill ratio of the first available container (0.0 to 1.0), or 0.0 if no container is found
     * @see #getFirstCanisterSupplierPair(Player)
     */
    public static float getFirstCanisterSupplierRatio(Player player) {
        Pair<GasStack, Long> pair = getFirstCanisterSupplierPair(player);
        GasStack content = pair.getFirst();
        long capacity = pair.getSecond();
        if (capacity == 0) {
            return 0;
        }

        return Mth.clamp((float) content.getAmount() / capacity, 0, 1);
    }
}
