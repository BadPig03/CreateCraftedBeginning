package net.ty.createcraftedbeginning.mixin.client.accessor;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.ty.createcraftedbeginning.platform.access.CreativeModeInventoryScreenAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@FunctionalInterface
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccessor extends CreativeModeInventoryScreenAccess {
    @Accessor("selectedTab")
    static CreativeModeTab ccb$getSelectedTab() {
        throw new AssertionError();
    }

    @Override
    @Accessor("scrollOffs")
    float ccb$getScrollOffs();
}
