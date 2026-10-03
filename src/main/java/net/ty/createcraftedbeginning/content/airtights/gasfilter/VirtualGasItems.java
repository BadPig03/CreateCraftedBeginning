package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class VirtualGasItems {
    private VirtualGasItems() {
    }

    public static ItemStack createVirtualItem(GasStack gasContent) {
        if (gasContent.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack virtualItem = new ItemStack(CCBItems.GAS_VIRTUAL_ITEM.asItem());
        virtualItem.set(DataComponents.ITEM_NAME, gasContent.getHoverName());
        virtualItem.set(CCBDataComponents.GAS_VIRTUAL_ITEM_COLOR, gasContent.getHint());
        virtualItem.set(CCBDataComponents.GAS_VIRTUAL_ITEM_TYPE, gasContent.copyWithAmount(1));
        return virtualItem;
    }

    public static boolean isVirtualItem(ItemStack stack) {
        return stack.is(CCBItems.GAS_VIRTUAL_ITEM) && !stack.getOrDefault(CCBDataComponents.GAS_VIRTUAL_ITEM_TYPE, GasStack.EMPTY).isEmpty();
    }

    public static GasStack readGasSample(ItemStack stack) {
        GasStack virtualGas = stack.getOrDefault(CCBDataComponents.GAS_VIRTUAL_ITEM_TYPE, GasStack.EMPTY);
        if (virtualGas.isEmpty()) {
            return GasStack.EMPTY;
        }

        return virtualGas.copyWithAmount(1);
    }

    public static @Unmodifiable List<ItemStack> createVirtualItems(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }

        if (isVirtualItem(stack)) {
            return List.of(stack.copyWithCount(1));
        }

        GasStack balloonGas = BalloonItem.getGas(stack);
        if (!balloonGas.isEmpty()) {
            ItemStack virtualItem = createVirtualItem(balloonGas.copyWithAmount(1));
            if (virtualItem.isEmpty()) {
                return List.of();
            }

            return List.of(virtualItem.copyWithCount(1));
        }

        GasCanisterContainer canisterContainer = stack.getCapability(CanisterCapabilities.ITEM);
        if (canisterContainer == null || canisterContainer.isEmpty()) {
            return List.of();
        }

        return canisterContainer.createVirtualItems().stream().map(virtualItem -> virtualItem.copyWithCount(1)).toList();
    }
}
