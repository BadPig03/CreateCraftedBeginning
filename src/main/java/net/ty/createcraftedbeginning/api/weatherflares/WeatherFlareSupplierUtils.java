package net.ty.createcraftedbeginning.api.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public final class WeatherFlareSupplierUtils {
    private static final List<Function<Player, List<ItemStack>>> FLARE_SUPPLIERS = new ArrayList<>();

    private WeatherFlareSupplierUtils() {
    }

    /**
     * Adds the supplied flare supplier.
     *
     * @param supplier the supplier used to obtain the value
     */
    public static void addFlareSupplier(Function<Player, List<ItemStack>> supplier) {
        FLARE_SUPPLIERS.add(supplier);
    }

    /**
     * Returns the flares from inventory.
     *
     * @param player the player performing the operation
     * @return the flares from inventory
     */
    public static List<ItemStack> getFlaresFromInventory(Player player) {
        List<ItemStack> flares = new ArrayList<>();

        ItemStack offHandItem = player.getOffhandItem();
        if (WeatherFlaresQueryUtils.isValidFlare(offHandItem)) {
            flares.add(offHandItem);
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (!WeatherFlaresQueryUtils.isValidFlare(item) || offHandItem == item) {
                continue;
            }

            flares.add(item);
        }
        return flares;
    }

    /**
     * Returns all flares.
     *
     * @param player the player performing the operation
     * @return all flares
     */
    public static @Unmodifiable List<ItemStack> getAllFlares(Player player) {
        List<ItemStack> inventoryFlares = getFlaresFromInventory(player);
        return Stream.concat(inventoryFlares.stream(), FLARE_SUPPLIERS.stream().flatMap(supplier -> supplier.apply(player).stream())).filter(WeatherFlaresQueryUtils::isValidFlare).toList();
    }

    /**
     * Returns the first available flare.
     *
     * @param player the player performing the operation
     * @return the first available flare
     */
    public static ItemStack getFirstFlare(Player player) {
        ItemStack offHandItem = player.getOffhandItem();
        if (WeatherFlaresQueryUtils.isValidFlare(offHandItem)) {
            return offHandItem;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == offHandItem || !WeatherFlaresQueryUtils.isValidFlare(item)) {
                continue;
            }

            return item;
        }

        for (Function<Player, List<ItemStack>> supplier : FLARE_SUPPLIERS) {
            for (ItemStack flare : supplier.apply(player)) {
                if (!WeatherFlaresQueryUtils.isValidFlare(flare)) {
                    continue;
                }

                return flare;
            }
        }
        return ItemStack.EMPTY;
    }
}
