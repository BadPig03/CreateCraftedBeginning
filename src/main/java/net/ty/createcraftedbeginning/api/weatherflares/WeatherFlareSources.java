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
public final class WeatherFlareSources {
    private static final List<Function<Player, List<ItemStack>>> FLARE_SUPPLIERS = new ArrayList<>();

    private WeatherFlareSources() {
    }

    @SuppressWarnings("unused")
    public static void register(Function<Player, List<ItemStack>> supplier) {
        FLARE_SUPPLIERS.add(supplier);
    }

    public static List<ItemStack> findInInventory(Player player) {
        List<ItemStack> flares = new ArrayList<>();

        ItemStack offHandItem = player.getOffhandItem();
        if (WeatherFlareQueries.isValidFlare(offHandItem)) {
            flares.add(offHandItem);
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (!WeatherFlareQueries.isValidFlare(item) || offHandItem == item) {
                continue;
            }

            flares.add(item);
        }
        return flares;
    }

    @SuppressWarnings("unused")
    public static @Unmodifiable List<ItemStack> findAll(Player player) {
        List<ItemStack> inventoryFlares = findInInventory(player);
        return Stream.concat(inventoryFlares.stream(), FLARE_SUPPLIERS.stream().flatMap(supplier -> supplier.apply(player).stream())).filter(WeatherFlareQueries::isValidFlare).toList();
    }

    public static ItemStack findFirst(Player player) {
        ItemStack offHandItem = player.getOffhandItem();
        if (WeatherFlareQueries.isValidFlare(offHandItem)) {
            return offHandItem;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == offHandItem || !WeatherFlareQueries.isValidFlare(item)) {
                continue;
            }

            return item;
        }

        for (Function<Player, List<ItemStack>> supplier : FLARE_SUPPLIERS) {
            for (ItemStack flare : supplier.apply(player)) {
                if (!WeatherFlareQueries.isValidFlare(flare)) {
                    continue;
                }

                return flare;
            }
        }
        return ItemStack.EMPTY;
    }
}
