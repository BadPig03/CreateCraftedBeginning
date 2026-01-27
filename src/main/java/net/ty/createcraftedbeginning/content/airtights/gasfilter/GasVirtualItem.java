package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

public class GasVirtualItem extends Item {
    public GasVirtualItem(Properties properties) {
        super(properties);
    }

    public static @NotNull ItemStack getVirtualItem(@NotNull GasStack gasContent) {
        if (gasContent.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Gas gasType = gasContent.getGasType();
        ItemStack virtual = new ItemStack(CCBItems.GAS_VIRTUAL_ITEM.asItem());
        virtual.set(DataComponents.ITEM_NAME, Component.translatable(gasType.getTranslationKey()));
        virtual.set(CCBDataComponents.GAS_VIRTUAL_ITEM_COLOR, gasType.getTint());
        virtual.set(CCBDataComponents.GAS_VIRTUAL_ITEM_TYPE, new GasStack(gasType, 1));
        return virtual;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player && player.containerMenu instanceof GasFilterMenu) {
            return;
        }

        stack.shrink(1);
    }
}
