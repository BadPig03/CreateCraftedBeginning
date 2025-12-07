package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
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

    public static boolean isTag(@NotNull GasStack gas, TagKey<Gas> tag) {
        return isTag(gas.getGas(), tag);
    }

    public static boolean isTag(@NotNull Gas gas, TagKey<Gas> tag) {
        return gas.is(tag);
    }
}
