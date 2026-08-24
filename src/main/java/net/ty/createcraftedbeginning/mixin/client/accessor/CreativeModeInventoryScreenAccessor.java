package net.ty.createcraftedbeginning.mixin.client.accessor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.ty.createcraftedbeginning.platform.access.CreativeModeInventoryScreenAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenAccessor implements CreativeModeInventoryScreenAccess {
    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    private float scrollOffs;

    @Override
    public CreativeModeTab ccb$getSelectedTab() {
        return selectedTab;
    }

    @Override
    public float ccb$getScrollOffs() {
        return scrollOffs;
    }
}
