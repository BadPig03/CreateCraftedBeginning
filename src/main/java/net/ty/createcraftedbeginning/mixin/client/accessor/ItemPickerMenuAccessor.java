package net.ty.createcraftedbeginning.mixin.client.accessor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu;
import net.ty.createcraftedbeginning.platform.access.ItemPickerMenuAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
@Mixin(ItemPickerMenu.class)
public interface ItemPickerMenuAccessor extends ItemPickerMenuAccess {
    @Override
    @Invoker("getRowIndexForScroll")
    int ccb$getRowIndexForScroll(float scrollOffset);
}
