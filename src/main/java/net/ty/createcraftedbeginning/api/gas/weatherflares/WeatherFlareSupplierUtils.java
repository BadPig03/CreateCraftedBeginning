package net.ty.createcraftedbeginning.api.gas.weatherflares;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class WeatherFlareSupplierUtils {
    private static final List<Function<Player, List<ItemStack>>> FLARE_SUPPLIERS = new ArrayList<>();

    static {
        addFlareSupplier(WeatherFlareSupplierUtils::getFlaresFromInventory);
    }

    private WeatherFlareSupplierUtils() {
    }

    public static void addFlareSupplier(Function<Player, List<ItemStack>> supplier) {
        FLARE_SUPPLIERS.add(supplier);
    }

    public static @NotNull List<ItemStack> getFlaresFromInventory(@NotNull Player player) {
        List<ItemStack> flares = new ArrayList<>();

        ItemStack offHandItem = player.getOffhandItem();
        if (WeatherFlaresQueryUtils.isValidFlare(offHandItem)) {
            flares.add(offHandItem);
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (WeatherFlaresQueryUtils.isValidFlare(item) && !ItemStack.isSameItem(offHandItem, item)) {
                flares.add(item);
            }
        }
        return flares;
    }

    public static @NotNull @Unmodifiable List<ItemStack> getAllFlares(@NotNull Player player) {
        return FLARE_SUPPLIERS.stream().flatMap(supplier -> supplier.apply(player).stream()).filter(WeatherFlaresQueryUtils::isValidFlare).toList();
    }

    public static @NotNull ItemStack getFirstFlare(@NotNull Player player) {
        List<ItemStack> flares = getAllFlares(player);
        if (flares.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return flares.getFirst();
    }
}
