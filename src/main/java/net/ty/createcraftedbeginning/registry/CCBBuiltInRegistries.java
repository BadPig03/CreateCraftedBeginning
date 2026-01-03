package net.ty.createcraftedbeginning.registry;

import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.ty.createcraftedbeginning.api.gas.gases.MountedGasStorageType;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

public class CCBBuiltInRegistries {
    public static final Registry<MountedGasStorageType<?>> MOUNTED_GAS_STORAGE_TYPE = register(CCBRegistries.MOUNTED_GAS_STORAGE_TYPE, () -> {});

    @SuppressWarnings({"unchecked", "rawtypes", "SameParameterValue"})
    private static <T> @NotNull Registry<T> register(ResourceKey<Registry<T>> key, Runnable onBakeCallback) {
        RegistryBuilder<T> builder = new RegistryBuilder<>(key).sync(true);
        builder.onBake(registry -> onBakeCallback.run());
        Registry<T> registry = builder.create();
        ((WritableRegistry) BuiltInRegistries.REGISTRY).register(key, registry, RegistrationInfo.BUILT_IN);
        return registry;
    }

    @Internal
    public static void init() {
    }
}
