package net.ty.createcraftedbeginning.compat.kubejs.registry;

import dev.latvian.mods.kubejs.registry.BuilderBase;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasBuilder;
import net.ty.createcraftedbeginning.registry.CCBRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class GasKubeJSBuilder extends BuilderBase<Gas> {
    private final GasBuilder builder = GasBuilder.builder();

    /**
     * Creates a KubeJS builder for a gas registry entry.
     *
     * @param id the resource location of the gas being built
     */
    public GasKubeJSBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Restores the default gas texture derived from the gas id.
     *
     * @return this builder
     */
    public GasKubeJSBuilder defaultTexture() {
        builder.texture(null);
        return this;
    }

    /**
     * Sets the texture used to render the gas.
     *
     * @param location the gas texture location
     * @return this builder
     */
    public GasKubeJSBuilder texture(ResourceLocation location) {
        builder.texture(location);
        return this;
    }

    /**
     * Sets the RGB tint applied to the gas texture.
     *
     * @param tint the gas tint value
     * @return this builder
     */
    public GasKubeJSBuilder tint(int tint) {
        builder.tint(tint);
        return this;
    }

    /**
     * Sets the alpha component applied to the gas texture.
     *
     * @param alpha the gas alpha value
     * @return this builder
     */
    public GasKubeJSBuilder alpha(int alpha) {
        builder.alpha(alpha);
        return this;
    }

    /**
     * Adds the gas to the supplied gas tag.
     *
     * @param location the resource location of the gas tag
     * @return this builder
     */
    public GasKubeJSBuilder tag(ResourceLocation location) {
        builder.tag(TagKey.create(CCBRegistries.GAS_REGISTRY_KEY, location));
        return this;
    }

    /**
     * Creates the gas represented by the current builder state.
     *
     * @return the constructed gas
     */
    @Override
    public Gas createObject() {
        return new Gas(builder);
    }
}