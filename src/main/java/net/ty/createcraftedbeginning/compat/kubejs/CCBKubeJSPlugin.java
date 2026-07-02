package net.ty.createcraftedbeginning.compat.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.registry.BuilderTypeRegistry;
import net.ty.createcraftedbeginning.compat.kubejs.registry.GasKubeJSBuilder;
import net.ty.createcraftedbeginning.registry.CCBRegistries;

public class CCBKubeJSPlugin implements KubeJSPlugin {

    @Override
    public void init() {
        System.out.println("CCBKubeJSPlugin Plugin initialized!");
    }

    @Override
    public void registerBuilderTypes(BuilderTypeRegistry registry) {
        registry.addDefault(
                CCBRegistries.GAS_REGISTRY_KEY,
                GasKubeJSBuilder.class,
                GasKubeJSBuilder::new
        );
    }
}