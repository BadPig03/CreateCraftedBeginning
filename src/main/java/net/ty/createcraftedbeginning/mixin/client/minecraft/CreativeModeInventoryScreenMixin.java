package net.ty.createcraftedbeginning.mixin.client.minecraft;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Shadow
    private static CreativeModeTab selectedTab;

    @Unique
    private static Collection<ItemStack> ccb$layoutForDisplay(Collection<ItemStack> items) {
        if (selectedTab != CCBCreativeTabs.CREATIVE_TAB.get()) {
            return items;
        }

        return CCBCreativeTabLayout.rebuildDisplayItems(items);
    }

    @SuppressWarnings("MethodMayBeStatic")
    @ModifyExpressionValue(method = "selectTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;getDisplayItems()Ljava/util/Collection;"))
    private Collection<ItemStack> ccb$selectTab(Collection<ItemStack> items) {
        return ccb$layoutForDisplay(items);
    }

    @SuppressWarnings("MethodMayBeStatic")
    @ModifyVariable(method = "refreshCurrentTabContents", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Collection<ItemStack> ccb$refreshCurrentTabContents(Collection<ItemStack> items) {
        return ccb$layoutForDisplay(items);
    }
}
