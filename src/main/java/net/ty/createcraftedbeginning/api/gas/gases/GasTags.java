package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class GasTags {
    private GasTags() {
    }

    private static @NotNull TagKey<Gas> create(String name) {
        return TagKey.create(CCBRegistries.GAS_REGISTRY_KEY, ResourceLocation.withDefaultNamespace(name));
    }

    public static @NotNull TagKey<Gas> create(ResourceLocation name) {
        return TagKey.create(CCBRegistries.GAS_REGISTRY_KEY, name);
    }

    public static boolean isTag(@NotNull GasStack gasStack, TagKey<Gas> tag) {
        return isTag(gasStack.getGasType(), tag);
    }

    public static boolean isTag(@NotNull Gas gasType, TagKey<Gas> tag) {
        return gasType.is(tag);
    }
}
