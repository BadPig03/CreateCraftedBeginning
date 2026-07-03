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

    public GasKubeJSBuilder(ResourceLocation id) {
        super(id);
    }

    public GasKubeJSBuilder defaultTexture() {
        builder.texture(null);
        return this;
    }

    public GasKubeJSBuilder texture(ResourceLocation location) {
        builder.texture(location);
        return this;
    }

    public GasKubeJSBuilder tint(int tint) {
        builder.tint(tint);
        return this;
    }

    public GasKubeJSBuilder alpha(int alpha) {
        builder.alpha(alpha);
        return this;
    }

    public GasKubeJSBuilder tag(ResourceLocation location) {
        TagKey<Gas> tagKey = TagKey.create(CCBRegistries.GAS_REGISTRY_KEY, location);
        builder.tag(tagKey);
        return this;
    }

    @Override
    public Gas createObject() {
        return new Gas(builder);
    }
}