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
public final class WeatherFlareSupplierUtils {
    private static final List<Function<Player, List<ItemStack>>> FLARE_SUPPLIERS = new ArrayList<>();

    private WeatherFlareSupplierUtils() {
    }

    @SuppressWarnings("unused")
    public static void addFlareSupplier(Function<Player, List<ItemStack>> supplier) {
        FLARE_SUPPLIERS.add(supplier);
    }

    public static List<ItemStack> getFlaresFromInventory(Player player) {
        List<ItemStack> flares = new ArrayList<>();

        ItemStack offHandItem = player.getOffhandItem();
        if (WeatherFlareQueryUtils.isValidFlare(offHandItem)) {
            flares.add(offHandItem);
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (!WeatherFlareQueryUtils.isValidFlare(item) || offHandItem == item) {
                continue;
            }

            flares.add(item);
        }
        return flares;
    }

    @SuppressWarnings("unused")
    public static @Unmodifiable List<ItemStack> getAllFlares(Player player) {
        List<ItemStack> inventoryFlares = getFlaresFromInventory(player);
        return Stream.concat(inventoryFlares.stream(), FLARE_SUPPLIERS.stream().flatMap(supplier -> supplier.apply(player).stream())).filter(WeatherFlareQueryUtils::isValidFlare).toList();
    }

    public static ItemStack getFirstFlare(Player player) {
        ItemStack offHandItem = player.getOffhandItem();
        if (WeatherFlareQueryUtils.isValidFlare(offHandItem)) {
            return offHandItem;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == offHandItem || !WeatherFlareQueryUtils.isValidFlare(item)) {
                continue;
            }

            return item;
        }

        for (Function<Player, List<ItemStack>> supplier : FLARE_SUPPLIERS) {
            for (ItemStack flare : supplier.apply(player)) {
                if (!WeatherFlareQueryUtils.isValidFlare(flare)) {
                    continue;
                }

                return flare;
            }
        }
        return ItemStack.EMPTY;
    }
}
