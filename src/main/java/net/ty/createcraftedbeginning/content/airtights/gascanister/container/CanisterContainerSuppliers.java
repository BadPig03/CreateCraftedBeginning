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

    public static List<IGasCanisterContainer> getCanisterContainersInInventory(Player player) {
        List<IGasCanisterContainer> containers = new ArrayList<>();
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            IGasCanisterContainer container = offhand.getCapability(GasHandler.ITEM);
            if (container != null) {
                containers.add(container);
            }
        }

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || offhand == stack) {
                continue;
            }

            IGasCanisterContainer container = stack.getCapability(GasHandler.ITEM);
            if (container != null) {
                containers.add(container);
            }
        }
        return containers;
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
        Set<IGasCanisterContainer> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<ItemStack, Integer> stackIndexes = new IdentityHashMap<>();
        for (Function<Player, List<IGasCanisterContainer>> supplier : CANISTER_CONTAINER_SUPPLIERS) {
            List<IGasCanisterContainer> supplied = supplier.apply(player);
            if (supplied == null) {
                continue;
            }

            for (IGasCanisterContainer container : supplied) {
                if (container == null || !seen.add(container)) {
                    continue;
                }

                ItemStack stack = container.getContainer();
                if (stack.isEmpty()) {
                    containers.add(container);
                    continue;
                }

                Integer index = stackIndexes.get(stack);
                if (index == null) {
                    stackIndexes.put(stack, containers.size());
                    containers.add(container);
                    continue;
                }

                IGasCanisterContainer existing = containers.get(index);
                if (container.getPriority() <= existing.getPriority()) {
                    continue;
                }

                containers.set(index, container);
            }
        }

        containers.sort((first, second) -> Integer.compare(second.getPriority(), first.getPriority()));
        List<IGasCanisterContainer> result = List.copyOf(containers);
        synchronized (SUPPLIER_CACHE) {
            SUPPLIER_CACHE.put(player, new SupplierCache(level, gameTime, result));
        }
        return result;
    }

    public static GasStack getFirstAvailableGasContent(Player player) {
        for (IGasCanisterContainer container : getAllSuppliers(player)) {
            for (int tank = 0; tank < container.getTanks(); tank++) {
                GasStack gasContent = container.getGasInTank(tank);
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

    public static Pair<GasStack, Pair<Long, Boolean>> getFirstCanisterSupplierPair(Player player) {
        for (IGasCanisterContainer container : getAllSuppliers(player)) {
            if (container instanceof GasCanisterPackContainerContents pack) {
                Pair<GasStack, Pair<Long, Boolean>> content = pack.getFirstNonEmptyPair();
                if (!content.getFirst().isEmpty()) {
                    return content;
                }

                continue;
            }

            for (int tank = 0; tank < container.getTanks(); tank++) {
                GasStack gasContent = container.getGasInTank(tank);
                if (gasContent.isEmpty()) {
                    continue;
                }

                boolean creative = container instanceof CreativeGasCanisterContainerContents;
                return Pair.of(gasContent, Pair.of(container.getTankCapacity(tank), creative));
            }
        }
        return Pair.of(GasStack.EMPTY, Pair.of(0L, false));
    }

    private record SupplierCache(Level level, long gameTime, List<IGasCanisterContainer> containers) {}
}
