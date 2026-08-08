package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public final class GasTags {
    private GasTags() {
    }

    private static TagKey<Gas> create(String name) {
        return TagKey.create(GasRegistries.GAS_REGISTRY_KEY, ResourceLocation.withDefaultNamespace(name));
    }

    /**
     * Creates a new value from the supplied arguments.
     *
     * @param name the name of the target value
     * @return the created value
     */
    public static TagKey<Gas> create(ResourceLocation name) {
        return TagKey.create(GasRegistries.GAS_REGISTRY_KEY, name);
    }

    /**
     * Checks whether this value is tag.
     *
     * @param gasStack the gas stack to inspect or process
     * @param tag      the tag to inspect or process
     * @return {@code true} if this value is tag; otherwise {@code false}
     */
    public static boolean isTag(GasStack gasStack, TagKey<Gas> tag) {
        return isTag(gasStack.getGasType(), tag);
    }

    /**
     * Checks whether this value is tag.
     *
     * @param gasType the gas type to inspect or process
     * @param tag     the tag to inspect or process
     * @return {@code true} if this value is tag; otherwise {@code false}
     */
    public static boolean isTag(Gas gasType, TagKey<Gas> tag) {
        return gasType.is(tag);
    }
}
