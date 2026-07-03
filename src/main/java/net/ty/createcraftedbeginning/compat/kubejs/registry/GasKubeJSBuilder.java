package net.ty.createcraftedbeginning.compat.kubejs.registry;

import dev.latvian.mods.kubejs.registry.BuilderBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasBuilder;
import net.ty.createcraftedbeginning.registry.CCBRegistries;

public class GasKubeJSBuilder extends BuilderBase<Gas> {

    private final GasBuilder internalBuilder = GasBuilder.builder();

    public GasKubeJSBuilder(ResourceLocation id) {
        super(id);
    }

    // 色调
    public GasKubeJSBuilder tint(int tint) {
        internalBuilder.tint(tint);
        return this;
    }

    // 不知道啥
    public GasKubeJSBuilder inflation(int inflation) {
        internalBuilder.inflation(inflation);
        return this;
    }

    // 引擎效率
    public GasKubeJSBuilder engineEfficiency(int efficiency) {
        internalBuilder.engineEfficiency(efficiency);
        return this;
    }

    // 不知道啥
    public GasKubeJSBuilder teslaEfficiency(int efficiency) {
        internalBuilder.teslaEfficiency(efficiency);
        return this;
    }

    // 标签
    public GasKubeJSBuilder tag(String tag) {
        TagKey<Gas> tagKey = TagKey.create(CCBRegistries.GAS_REGISTRY_KEY, ResourceLocation.parse(tag));
        internalBuilder.tag(tagKey);
        return this;
    }

    @Override
    public Gas createObject() {
        return new Gas(internalBuilder);
    }
}