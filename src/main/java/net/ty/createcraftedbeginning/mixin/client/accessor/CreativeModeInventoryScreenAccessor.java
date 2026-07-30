package net.ty.createcraftedbeginning.mixin.client.accessor;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@FunctionalInterface
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccessor {
    @Accessor("selectedTab")
    static CreativeModeTab ccb$getSelectedTab() {
        throw new AssertionError();
    }

    @Accessor("scrollOffs")
    float ccb$getScrollOffs();
}
