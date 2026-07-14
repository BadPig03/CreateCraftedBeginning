package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasVirtualUtils {
    private GasVirtualUtils() {
    }

    public static ItemStack createVirtualItem(GasStack gasContent) {
        if (gasContent.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Gas gasType = gasContent.getGasType();
        ItemStack virtualItem = new ItemStack(CCBItems.GAS_VIRTUAL_ITEM.asItem());
        virtualItem.set(DataComponents.ITEM_NAME, Component.translatable(gasType.getTranslationKey()));
        virtualItem.set(CCBDataComponents.GAS_VIRTUAL_ITEM_COLOR, gasType.getTint());
        virtualItem.set(CCBDataComponents.GAS_VIRTUAL_ITEM_TYPE, gasContent.copyWithAmount(1));
        return virtualItem;
    }

    public static boolean isVirtualItem(ItemStack stack) {
        return stack.is(CCBItems.GAS_VIRTUAL_ITEM) && !stack.getOrDefault(CCBDataComponents.GAS_VIRTUAL_ITEM_TYPE, GasStack.EMPTY).isEmpty();
    }

    public static GasStack getGasType(ItemStack stack) {
        GasStack gas = stack.getOrDefault(CCBDataComponents.GAS_VIRTUAL_ITEM_TYPE, GasStack.EMPTY);
        if (gas.isEmpty()) {
            return GasStack.EMPTY;
        }
        return gas.copyWithAmount(1);
    }

    public static @Unmodifiable List<ItemStack> getVirtualItems(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }

        if (isVirtualItem(stack)) {
            return List.of(stack.copyWithCount(1));
        }

        if (BalloonUtils.containsGasContents(stack)) {
            return BalloonUtils.getGasContents(stack).gases().stream().map(GasVirtualUtils::createVirtualItem).filter(virtual -> !virtual.isEmpty()).map(virtual -> virtual.copyWithCount(1)).toList();
        }

        IGasCanisterContainer container = stack.getCapability(GasHandler.ITEM);
        if (container == null || container.isEmpty()) {
            return List.of();
        }

        return container.getVirtualItems().stream().map(virtual -> virtual.copyWithCount(1)).toList();
    }
}
