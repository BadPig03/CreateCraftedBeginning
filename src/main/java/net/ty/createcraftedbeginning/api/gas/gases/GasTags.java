package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.ty.createcraftedbeginning.registry.CCBRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasTags {
    private GasTags() {
    }

    private static TagKey<Gas> create(String name) {
        return TagKey.create(CCBRegistries.GAS_REGISTRY_KEY, ResourceLocation.withDefaultNamespace(name));
    }

    public static TagKey<Gas> create(ResourceLocation name) {
        return TagKey.create(CCBRegistries.GAS_REGISTRY_KEY, name);
    }

    public static boolean isTag(GasStack gasStack, TagKey<Gas> tag) {
        return isTag(gasStack.getGasType(), tag);
    }

    public static boolean isTag(Gas gasType, TagKey<Gas> tag) {
        return gasType.is(tag);
    }
}
