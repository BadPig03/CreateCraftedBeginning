package net.ty.createcraftedbeginning.platform.access;

import net.minecraft.world.item.ItemStack;

public interface ItemInHandRendererAccess {
    ItemStack getMainHandItem();

    ItemStack getOffHandItem();
}
