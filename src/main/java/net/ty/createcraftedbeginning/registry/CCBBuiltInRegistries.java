package net.ty.createcraftedbeginning.registry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageType;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBBuiltInRegistries {
    @Nullable
    private static Registry<MountedGasStorageType<?>> mountedGasStorageType;

    private CCBBuiltInRegistries() {
    }

    @Internal
    public static void bootstrap() {
        if (mountedGasStorageType != null) {
            return;
        }

        mountedGasStorageType = register(CCBRegistries.MOUNTED_GAS_STORAGE_TYPE);
    }

    public static Registry<MountedGasStorageType<?>> mountedGasStorageType() {
        Registry<MountedGasStorageType<?>> registry = mountedGasStorageType;
        if (registry == null) {
            throw new IllegalStateException("CCB built-in registries have not been bootstrapped yet.");
        }

        return registry;
    }

    @SuppressWarnings({"unchecked", "rawtypes", "SameParameterValue"})
    private static <T> @NotNull Registry<T> register(ResourceKey<Registry<T>> key) {
        RegistryBuilder<T> builder = new RegistryBuilder<>(key).sync(true);
        Registry<T> registry = builder.create();
        ((WritableRegistry) BuiltInRegistries.REGISTRY).register(key, registry, RegistrationInfo.BUILT_IN);
        return registry;
    }
}
