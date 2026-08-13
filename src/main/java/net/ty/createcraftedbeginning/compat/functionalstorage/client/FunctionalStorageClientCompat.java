package net.ty.createcraftedbeginning.compat.functionalstorage.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.ty.createcraftedbeginning.compat.functionalstorage.registry.CCBFunctionalStorageBlockEntities;
import net.ty.createcraftedbeginning.compat.functionalstorage.registry.CCBFunctionalStorageBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FunctionalStorageClientCompat {
    private FunctionalStorageClientCompat() {
    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(renderer(GasDrawerISTER.SLOT_1), CCBFunctionalStorageBlocks.GAS_DRAWER_1_BLOCK.asItem());
        event.registerItem(renderer(GasDrawerISTER.SLOT_2), CCBFunctionalStorageBlocks.GAS_DRAWER_2_BLOCK.asItem());
        event.registerItem(renderer(GasDrawerISTER.SLOT_4), CCBFunctionalStorageBlocks.GAS_DRAWER_4_BLOCK.asItem());
    }

    public static void registerRenderers(RegisterRenderers event) {
        event.registerBlockEntityRenderer(CCBFunctionalStorageBlockEntities.GAS_DRAWER_1.get(), GasDrawerRenderer::new);
        event.registerBlockEntityRenderer(CCBFunctionalStorageBlockEntities.GAS_DRAWER_2.get(), GasDrawerRenderer::new);
        event.registerBlockEntityRenderer(CCBFunctionalStorageBlockEntities.GAS_DRAWER_4.get(), GasDrawerRenderer::new);
    }

    private static IClientItemExtensions renderer(BlockEntityWithoutLevelRenderer renderer) {
        return new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        };
    }
}
