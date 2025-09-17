package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.ty.createcraftedbeginning.data.CCBGasRegistry;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class GasTags {
    private GasTags() {
    }

    private static @NotNull TagKey<Gas> create(String name) {
        return TagKey.create(CCBGasRegistry.GAS_REGISTRY_NAME, ResourceLocation.withDefaultNamespace(name));
    }

    public static @NotNull TagKey<Gas> create(ResourceLocation name) {
        return TagKey.create(CCBGasRegistry.GAS_REGISTRY_NAME, name);
    }
}
