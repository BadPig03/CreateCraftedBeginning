package net.ty.createcraftedbeginning.mixin.client.accessor;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.platform.access.ItemInHandRendererAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor extends ItemInHandRendererAccess {
    @Override
    @Accessor("mainHandItem")
    ItemStack getMainHandItem();

    @Override
    @Accessor("offHandItem")
    ItemStack getOffHandItem();
}
