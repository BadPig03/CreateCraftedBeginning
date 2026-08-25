package net.ty.createcraftedbeginning.platform.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.platform.access.client.ItemInHandRendererAccess;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class FirstPersonItemRenderBridge {
    private FirstPersonItemRenderBridge() {
    }

    public static @Nullable HandItems getHandItems(Minecraft minecraft) {
        ItemInHandRenderer renderer = minecraft.getEntityRenderDispatcher().getItemInHandRenderer();
        if (!(renderer instanceof ItemInHandRendererAccess access)) {
            return null;
        }

        return new HandItems(access.ccb$getMainHandItem(), access.ccb$getOffHandItem());
    }

    public record HandItems(ItemStack mainHandItem, ItemStack offHandItem) {}
}
