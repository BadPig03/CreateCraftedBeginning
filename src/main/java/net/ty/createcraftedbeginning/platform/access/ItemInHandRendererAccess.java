package net.ty.createcraftedbeginning.platform.access;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ItemInHandRendererAccess {
    ItemStack ccb$getMainHandItem();

    ItemStack ccb$getOffHandItem();
}
