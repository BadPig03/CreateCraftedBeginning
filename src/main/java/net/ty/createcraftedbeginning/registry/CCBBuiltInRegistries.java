package net.ty.createcraftedbeginning.registry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.ty.createcraftedbeginning.content.airtights.gas.mounted.MountedGasStorageType;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBBuiltInRegistries {
    @Nullable
    public static Registry<MountedGasStorageType<?>> MOUNTED_GAS_STORAGE_TYPE;

    private CCBBuiltInRegistries() {
    }

    @Internal
    public static void bootstrap() {
        if (MOUNTED_GAS_STORAGE_TYPE != null) {
            return;
        }

        MOUNTED_GAS_STORAGE_TYPE = register(CCBRegistries.MOUNTED_GAS_STORAGE_TYPE);
    }

    public static Registry<MountedGasStorageType<?>> mountedGasStorageType() {
        Registry<MountedGasStorageType<?>> registry = MOUNTED_GAS_STORAGE_TYPE;
        if (registry == null) {
            throw new IllegalStateException("CCB built-in registries have not been bootstrapped yet");
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
