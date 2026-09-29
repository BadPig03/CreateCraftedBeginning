package net.ty.createcraftedbeginning.content.airtights.gascanister.container;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.GasStack;
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
    private static final List<ContainerSupplier> CANISTER_CONTAINER_SUPPLIERS = new ArrayList<>();
    private static final Map<Player, SupplierCache> SUPPLIER_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    static {
        addCanisterContainerSuppliers(CanisterContainerSuppliers::getCanisterContainersInInventory, SupplyOrder.CONTAINER_PRIORITY);
    }

    private CanisterContainerSuppliers() {
    }

    public static void addCanisterContainerSuppliers(Function<Player, List<GasCanisterContainer>> supplier, SupplyOrder order) {
        CANISTER_CONTAINER_SUPPLIERS.add(new ContainerSupplier(supplier, order));
        synchronized (SUPPLIER_CACHE) {
            SUPPLIER_CACHE.clear();
        }
    }

    public static boolean isValidCanisterContainer(ItemStack itemStack) {
        return !itemStack.isEmpty() && itemStack.getCapability(CanisterCapabilities.ITEM) != null;
    }

    public static boolean isValidGasCanister(ItemStack itemStack) {
        return isValidCanisterContainer(itemStack) && (itemStack.is(CCBItems.GAS_CANISTER) || itemStack.getItem() instanceof GasCanisterItem);
    }

    public static boolean isValidCreativeGasCanister(ItemStack itemStack) {
        return isValidCanisterContainer(itemStack) && (itemStack.is(CCBItems.CREATIVE_GAS_CANISTER) || itemStack.getItem() instanceof CreativeGasCanisterItem);
    }

    public static @Unmodifiable List<GasCanisterContainer> getAllSuppliers(Player player) {
        Level level = player.level();
        long gameTime = level.getGameTime();
        synchronized (SUPPLIER_CACHE) {
            SupplierCache cache = SUPPLIER_CACHE.get(player);
            if (cache != null && cache.level() == level && cache.gameTime() == gameTime) {
                return cache.containers();
            }
        }

        List<SuppliedContainer> supplied = new ArrayList<>();
        for (ContainerSupplier source : CANISTER_CONTAINER_SUPPLIERS) {
            List<GasCanisterContainer> containers = source.supplier().apply(player);
            if (containers == null) {
                continue;
            }

            for (GasCanisterContainer container : containers) {
                if (container == null) {
                    continue;
                }

                supplied.add(new SuppliedContainer(container, source.order()));
            }
        }

        supplied.sort((first, second) -> {
            int sourceOrder = first.order().compareTo(second.order());
            if (sourceOrder != 0) {
                return sourceOrder;
            }

            if (first.order() == SupplyOrder.SLOT_ORDER) {
                return 0;
            }

            return Integer.compare(second.container().getPriority(), first.container().getPriority());
        });
        List<GasCanisterContainer> containers = new ArrayList<>();
        Set<GasCanisterContainer> seenContainers = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<ItemStack> seenStacks = Collections.newSetFromMap(new IdentityHashMap<>());
        for (SuppliedContainer entry : supplied) {
            GasCanisterContainer container = entry.container();
            if (!seenContainers.add(container)) {
                continue;
            }

            ItemStack stack = container.getContainer();
            if (!stack.isEmpty() && !seenStacks.add(stack)) {
                continue;
            }

            containers.add(container);
        }

        List<GasCanisterContainer> resolvedContainers = List.copyOf(containers);
        synchronized (SUPPLIER_CACHE) {
            SUPPLIER_CACHE.put(player, new SupplierCache(level, gameTime, resolvedContainers));
        }
        return resolvedContainers;
    }

    public static GasStack getFirstAvailableGasContent(Player player) {
        for (GasCanisterContainer container : getAllSuppliers(player)) {
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

    public static void invalidateCache(Player player) {
        synchronized (SUPPLIER_CACHE) {
            SUPPLIER_CACHE.remove(player);
        }
    }

    static CanisterSupplierSnapshot getFirstCanisterSupplierSnapshot(Player player) {
        for (GasCanisterContainer container : getAllSuppliers(player)) {
            for (int tankIndex = 0; tankIndex < container.getTanks(); tankIndex++) {
                GasStack gasContent = container.getGasInTank(tankIndex);
                if (gasContent.isEmpty()) {
                    continue;
                }

                boolean creative = container instanceof CreativeGasCanisterContainerContents;
                if (container instanceof GasCanisterPackContainerContents packContents) {
                    creative = packContents.isCreative(tankIndex);
                }
                return new CanisterSupplierSnapshot(gasContent, container.getTankMaxAmount(tankIndex), container.getTankPressurePa(tankIndex), creative);
            }
        }
        return CanisterSupplierSnapshot.EMPTY;
    }

    private static List<GasCanisterContainer> getCanisterContainersInInventory(Player player) {
        List<GasCanisterContainer> containers = new ArrayList<>();
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            GasCanisterContainer container = offhand.getCapability(CanisterCapabilities.ITEM);
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

            GasCanisterContainer container = stack.getCapability(CanisterCapabilities.ITEM);
            if (container == null) {
                continue;
            }

            containers.add(container);
        }
        return containers;
    }

    record CanisterSupplierSnapshot(GasStack content, long maxAmount, long pressurePa, boolean creative) {
        private static final CanisterSupplierSnapshot EMPTY = new CanisterSupplierSnapshot(GasStack.EMPTY, 0, 0, false);

        CanisterSupplierSnapshot {
            content = content.copy();
        }
    }

    public enum SupplyOrder {
        SLOT_ORDER,
        CONTAINER_PRIORITY
    }

    private record ContainerSupplier(Function<Player, List<GasCanisterContainer>> supplier, SupplyOrder order) {}

    private record SuppliedContainer(GasCanisterContainer container, SupplyOrder order) {}

    private record SupplierCache(Level level, long gameTime, List<GasCanisterContainer> containers) {}
}
