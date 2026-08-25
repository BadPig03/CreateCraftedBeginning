package net.ty.createcraftedbeginning.platform.access.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.CreativeModeTab;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface CreativeModeInventoryScreenAccess {
    CreativeModeTab ccb$getSelectedTab();

    float ccb$getScrollOffs();
}
