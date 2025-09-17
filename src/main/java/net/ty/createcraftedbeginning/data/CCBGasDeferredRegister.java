package net.ty.createcraftedbeginning.data;

import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasBuilder;
import net.ty.createcraftedbeginning.api.gas.GasType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class CCBGasDeferredRegister extends CCBDeferredRegister<Gas> {
    public CCBGasDeferredRegister(String modId) {
        super(CCBGasRegistry.GAS_REGISTRY_NAME, modId, GasType::new);
    }

    public GasType<Gas> register(String name, GasBuilder builder) {
        return register(name, () -> new Gas(builder));
    }

    public GasType<Gas> register(String name, int color, @Nullable String pressurizedGasName, @Nullable String depressurizedGasName, @Nullable String vortexedGasName) {
        return register(name, () -> new Gas(GasBuilder.builder().tint(color).pressurizedGas(pressurizedGasName).depressurizedGas(depressurizedGasName).vortexedGas(vortexedGasName)));
    }

    public GasType<Gas> register(String name, String texture, int color, @Nullable String pressurizedGasName, @Nullable String depressurizedGasName, @Nullable String vortexedGasName) {
        return register(name, () -> new Gas(GasBuilder.builder(CreateCraftedBeginning.asResource(texture)).tint(color).pressurizedGas(pressurizedGasName).depressurizedGas(depressurizedGasName).vortexedGas(vortexedGasName)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Gas> @NotNull GasType<T> register(@NotNull String name, @NotNull Supplier<? extends T> sup) {
        return (GasType<T>) super.register(name, sup);
    }
}
