package net.ty.createcraftedbeginning.platform.access.client;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ItemPickerMenuAccess {
    int ccb$getRowIndexForScroll(float scrollOffset);
}
