package net.ty.createcraftedbeginning.mixin.client.minecraft;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin {
    @Shadow
    private Collection<ItemStack> displayItems;

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "buildContents", at = @At("RETURN"))
    private void ccb$buildContents(ItemDisplayParameters parameters, CallbackInfo ci) {
        CreativeModeTab tab = (CreativeModeTab) (Object) this;
        if (tab != CCBCreativeTabs.CREATIVE_TAB.get()) {
            return;
        }

        displayItems = CCBCreativeTabLayout.rebuildDisplayItems(displayItems);
    }
}
