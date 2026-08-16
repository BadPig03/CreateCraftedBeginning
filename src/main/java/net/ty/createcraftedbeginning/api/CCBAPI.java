package net.ty.createcraftedbeginning.api;

import com.mojang.logging.LogUtils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBAPI {
    public static final String MOD_ID = "createcraftedbeginning";
    public static final String NAME = "Create Crafted Beginning";
    public static final Logger LOGGER = LogUtils.getLogger();

    private CCBAPI() {
    }

    @Contract("_ -> new")
    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
