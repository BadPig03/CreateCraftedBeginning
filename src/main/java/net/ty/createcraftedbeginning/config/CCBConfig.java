package net.ty.createcraftedbeginning.config;

import com.simibubi.create.api.stress.BlockStressValues;
import net.createmod.catnip.config.ConfigBase;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class CCBConfig {
    private static final Map<Type, ConfigBase> CONFIGS = new EnumMap<>(Type.class);

    private static CCBCommon common;
    private static CCBServer server;
    private static CCBClient client;

    public static void register(ModContainer container) {
        common = register(CCBCommon::new, Type.COMMON);
        server = register(CCBServer::new, Type.SERVER);
        client = register(CCBClient::new, Type.CLIENT);

        for (Entry<Type, ConfigBase> pair : CONFIGS.entrySet()) {
            container.registerConfig(pair.getKey(), pair.getValue().specification);
        }

        CCBStress stress = server().stressValues;
        BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
        BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);
    }

    public static CCBCommon common() {
        return common;
    }

    public static CCBServer server() {
        return server;
    }

    public static CCBClient client() {
        return client;
    }

    private static <T extends ConfigBase> @NotNull T register(Supplier<T> factory, Type side) {
        Pair<T, ModConfigSpec> specPair = new Builder().configure(builder -> {
            T config = factory.get();
            config.registerAll(builder);
            return config;
        });

        T config = specPair.getLeft();
        config.specification = specPair.getRight();
        CONFIGS.put(side, config);
        return config;
    }

    @SubscribeEvent
    public static void onLoad(Loading event) {
        for (ConfigBase config : CONFIGS.values()) {
            if (config.specification != event.getConfig().getSpec()) {
                continue;
            }

            config.onLoad();
        }
    }

    @SubscribeEvent
    public static void onReload(Reloading event) {
        for (ConfigBase config : CONFIGS.values()) {
            if (config.specification != event.getConfig().getSpec()) {
                continue;
            }

            config.onReload();
        }
    }
}
