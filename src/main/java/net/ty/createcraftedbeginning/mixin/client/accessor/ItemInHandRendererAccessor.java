package net.ty.createcraftedbeginning.mixin.client.accessor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.platform.access.client.ItemInHandRendererAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor extends ItemInHandRendererAccess {
    @Override
    @Accessor("mainHandItem")
    ItemStack ccb$getMainHandItem();

    @Override
    @Accessor("offHandItem")
    ItemStack ccb$getOffHandItem();
}
