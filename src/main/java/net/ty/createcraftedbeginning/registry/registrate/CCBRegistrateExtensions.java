package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.api.registry.registrate.SimpleBuilder;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageType;
import net.ty.createcraftedbeginning.registry.CCBRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBRegistrateExtensions {
    private CCBRegistrateExtensions() {
    }

    public static <T extends MountedGasStorageType<?>> SimpleBuilder<MountedGasStorageType<?>, T, CreateRegistrate> mountedGasStorage(CreateRegistrate registrate, String name, Supplier<T> supplier) {
        return registrate.entry(name, callback -> new SimpleBuilder<>(registrate, registrate, name, callback, CCBRegistries.MOUNTED_GAS_STORAGE_TYPE, supplier).byBlock(MountedGasStorageType.REGISTRY));
    }
}
