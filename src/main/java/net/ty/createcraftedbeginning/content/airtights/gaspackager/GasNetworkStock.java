package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasNetworkStock {

    private GasNetworkStock() {
    }

    public static int getUniqueStockOf(UUID network, ItemStack stack, @Nullable IdentifiedInventory ignoredInventory) {
        long totalStock = 0;
        Set<InventoryIdentifier> processedInventories = new HashSet<>();
        for (LogisticallyLinkedBehaviour link : LogisticallyLinkedBehaviour.getAllPresent(network, false)) {
            InventoryIdentifier gasInventoryIdentifier = link.blockEntity instanceof PackagerLinkBlockEntity linkEntity && linkEntity.getPackager() instanceof GasPackagerBlockEntity packager ? packager.getGasInventoryIdentifier() : null;
            if (gasInventoryIdentifier != null && !processedInventories.add(gasInventoryIdentifier)) {
                continue;
            }

            totalStock += Math.max(0, link.getSummary(ignoredInventory).getCountOf(stack));
            if (totalStock < BigItemStack.INF) {
                continue;
            }

            return BigItemStack.INF;
        }

        return (int) totalStock;
    }
}
