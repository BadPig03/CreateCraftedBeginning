package net.ty.createcraftedbeginning.content.airtights.gascanister.container;

import net.createmod.catnip.data.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterItem;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterItem;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackContainerContents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CanisterContainerSuppliers {
    private static final List<Function<Player, List<IGasCanisterContainer>>> CANISTER_CONTAINER_SUPPLIERS = new ArrayList<>();
    private static final Map<Player, SupplierCache> SUPPLIER_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    static {
        addCanisterContainerSuppliers(CanisterContainerSuppliers::getCanisterContainersInInventory);
    }

    private CanisterContainerSuppliers() {
    }

    public static void addCanisterContainerSuppliers(Function<Player, List<IGasCanisterContainer>> supplier) {
        CANISTER_CONTAINER_SUPPLIERS.add(supplier);
        synchronized (SUPPLIER_CACHE) {
            SUPPLIER_CACHE.clear();
        }
    }

    public static boolean isValidCanisterContainer(ItemStack itemStack) {
        return !itemStack.isEmpty() && itemStack.getCapability(GasHandler.ITEM) != null;
    }

    public static boolean isValidGasCanister(ItemStack itemStack) {
        return isValidCanisterContainer(itemStack) && (itemStack.is(CCBItems.GAS_CANISTER) || itemStack.getItem() instanceof GasCanisterItem);
    }

    public static boolean isValidCreativeGasCanister(ItemStack itemStack) {
        return isValidCanisterContainer(itemStack) && (itemStack.is(CCBItems.CREATIVE_GAS_CANISTER) || itemStack.getItem() instanceof CreativeGasCanisterItem);
    }

    public static @Unmodifiable List<IGasCanisterContainer> getAllSuppliers(Player player) {
        Level level = player.level();
        long gameTime = level.getGameTime();
        synchronized (SUPPLIER_CACHE) {
            SupplierCache cache = SUPPLIER_CACHE.get(player);
            if (cache != null && cache.level() == level && cache.gameTime() == gameTime) {
                return cache.containers();
            }
        }

        List<IGasCanisterContainer> containers = new ArrayList<>();
        Set<IGasCanisterContainer> seenContainers = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<ItemStack, Integer> stackIndexes = new IdentityHashMap<>();
        for (Function<Player, List<IGasCanisterContainer>> supplier : CANISTER_CONTAINER_SUPPLIERS) {
            List<IGasCanisterContainer> suppliedContainers = supplier.apply(player);
            if (suppliedContainers == null) {
                continue;
            }

            for (IGasCanisterContainer container : suppliedContainers) {
                if (container == null || !seenContainers.add(container)) {
                    continue;
                }

                ItemStack stack = container.getContainer();
                if (stack.isEmpty()) {
                    containers.add(container);
                    continue;
                }

                Integer existingIndex = stackIndexes.get(stack);
                if (existingIndex == null) {
                    stackIndexes.put(stack, containers.size());
                    containers.add(container);
                    continue;
                }

                IGasCanisterContainer existingContainer = containers.get(existingIndex);
                if (container.getPriority() <= existingContainer.getPriority()) {
                    continue;
                }

                containers.set(existingIndex, container);
            }
        }

        containers.sort((firstContainer, secondContainer) -> Integer.compare(secondContainer.getPriority(), firstContainer.getPriority()));
        List<IGasCanisterContainer> resolvedContainers = List.copyOf(containers);
        synchronized (SUPPLIER_CACHE) {
            SUPPLIER_CACHE.put(player, new SupplierCache(level, gameTime, resolvedContainers));
        }
        return resolvedContainers;
    }

    public static GasStack getFirstAvailableGasContent(Player player) {
        for (IGasCanisterContainer container : getAllSuppliers(player)) {
            for (int tankIndex = 0; tankIndex < container.getTanks(); tankIndex++) {
                GasStack gasContent = container.getGasInTank(tankIndex);
                if (gasContent.isEmpty()) {
                    continue;
                }

                return gasContent;
            }
        }
        return GasStack.EMPTY;
    }

    public static boolean isAnyContainerAvailable(Player player) {
        return !getAllSuppliers(player).isEmpty();
    }

    static void invalidateCache(Player player) {
        synchronized (SUPPLIER_CACHE) {
            SUPPLIER_CACHE.remove(player);
        }
    }

    static Pair<GasStack, Pair<Long, Boolean>> getFirstCanisterSupplierPair(Player player) {
        for (IGasCanisterContainer container : getAllSuppliers(player)) {
            if (container instanceof GasCanisterPackContainerContents packContents) {
                Pair<GasStack, Pair<Long, Boolean>> packContent = packContents.getFirstNonEmptyPair();
                if (!packContent.getFirst().isEmpty()) {
                    return packContent;
                }

                continue;
            }

            for (int tankIndex = 0; tankIndex < container.getTanks(); tankIndex++) {
                GasStack gasContent = container.getGasInTank(tankIndex);
                if (gasContent.isEmpty()) {
                    continue;
                }

                boolean isCreative = container instanceof CreativeGasCanisterContainerContents;
                return Pair.of(gasContent, Pair.of(container.getTankCapacity(tankIndex), isCreative));
            }
        }
        return Pair.of(GasStack.EMPTY, Pair.of(0L, false));
    }

    private static List<IGasCanisterContainer> getCanisterContainersInInventory(Player player) {
        List<IGasCanisterContainer> containers = new ArrayList<>();
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            IGasCanisterContainer container = offhand.getCapability(GasHandler.ITEM);
            if (container != null) {
                containers.add(container);
            }
        }

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || offhand == stack) {
                continue;
            }

            IGasCanisterContainer container = stack.getCapability(GasHandler.ITEM);
            if (container == null) {
                continue;
            }

            containers.add(container);
        }
        return containers;
    }

    private record SupplierCache(Level level, long gameTime, List<IGasCanisterContainer> containers) {}
}
