package net.ty.createcraftedbeginning.api;

import com.mojang.logging.LogUtils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Stable entry point for constants and helpers that are safe for API consumers.
 *
 * <p>API code must depend on this class instead of the mod bootstrap class so the
 * public surface does not acquire a dependency on CCB's internal registration order.</p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBAPI {
    public static final String MOD_ID = "createcraftedbeginning";
    public static final String NAME = "Create Crafted Beginning";
    public static final Logger LOGGER = LogUtils.getLogger();

    private CCBAPI() {
    }

    /**
     * Creates a resource location in the Create Crafted Beginning namespace.
     *
     * @param path the path component of the resource location
     * @return a new resource location using {@link #MOD_ID} as its namespace
     */
    @Contract("_ -> new")
    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
