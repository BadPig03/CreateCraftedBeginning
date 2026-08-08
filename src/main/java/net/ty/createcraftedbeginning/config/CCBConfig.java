package net.ty.createcraftedbeginning.config;

import com.simibubi.create.api.stress.BlockStressValues;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.ty.createcraftedbeginning.api.CCBAPI;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public class CCBConfig {
    private static final Map<Type, ConfigBase> CONFIGS = new EnumMap<>(Type.class);

    private static CCBCommon common;
    private static CCBServer server;
    private static CCBClient client;

    public static void register(ModContainer container) {
        common = register(CCBCommon::new, Type.COMMON);
        server = register(CCBServer::new, Type.SERVER);
        client = register(CCBClient::new, Type.CLIENT);
        CONFIGS.forEach((type, config) -> container.registerConfig(type, config.specification));
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

    private static <T extends ConfigBase> @NotNull T register(Supplier<T> factory, Type type) {
        Pair<T, ModConfigSpec> pair = new Builder().configure(builder -> {
            T config = factory.get();
            config.registerAll(builder);
            return config;
        });

        T config = pair.getLeft();
        config.specification = pair.getRight();
        CONFIGS.put(type, config);
        return config;
    }

    @SubscribeEvent
    public static void onLoad(Loading event) {
        CONFIGS.values().stream().filter(config -> config.specification == event.getConfig().getSpec()).forEach(ConfigBase::onLoad);
        BlockStressValues.RPM.invalidate();
    }

    @SubscribeEvent
    public static void onReload(Reloading event) {
        CONFIGS.values().stream().filter(config -> config.specification == event.getConfig().getSpec()).forEach(ConfigBase::onReload);
        BlockStressValues.RPM.invalidate();
    }
}
