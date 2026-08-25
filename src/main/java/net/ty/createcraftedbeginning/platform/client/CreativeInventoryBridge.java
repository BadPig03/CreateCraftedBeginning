package net.ty.createcraftedbeginning.platform.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.platform.access.client.CreativeModeInventoryScreenAccess;
import net.ty.createcraftedbeginning.platform.access.client.ItemPickerMenuAccess;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class CreativeInventoryBridge {
    private CreativeInventoryBridge() {
    }

    public static @Nullable View getView(CreativeModeInventoryScreen screen) {
        if (!(screen instanceof CreativeModeInventoryScreenAccess screenAccess) || !(screen.getMenu() instanceof ItemPickerMenuAccess menuAccess)) {
            return null;
        }

        float scrollOffset = screenAccess.ccb$getScrollOffs();
        return new View(screenAccess.ccb$getSelectedTab(), menuAccess.ccb$getRowIndexForScroll(scrollOffset));
    }

    public record View(CreativeModeTab selectedTab, int firstVisibleRow) {}
}
