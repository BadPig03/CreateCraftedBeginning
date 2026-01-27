package net.ty.createcraftedbeginning.compat.jade.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import snownee.jade.util.CommonProxy;

import java.util.Objects;

public record GasObject(Gas gasType, long amount, DataComponentPatch components) {
    public static final Codec<GasObject> CODEC = RecordCodecBuilder.create(instance -> instance.group(CCBGasRegistries.GAS_REGISTRY.byNameCodec().fieldOf("type").forGetter(GasObject::gasType), Codec.LONG.fieldOf("amount").forGetter(GasObject::amount), DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(GasObject::components)).apply(instance, GasObject::of));

    public static long bucketVolume() {
        return CommonProxy.bucketVolume();
    }

    @Contract(" -> new")
    public static @NotNull GasObject empty() {
        return of(Gas.EMPTY_GAS_HOLDER.value(), 0);
    }

    public static @NotNull GasObject of(Gas gasType, long amount, DataComponentPatch components) {
        return new GasObject(gasType, amount, components);
    }

    @Contract("_, _ -> new")
    public static @NotNull GasObject of(Gas gasType, long amount) {
        return new GasObject(gasType, amount, DataComponentPatch.EMPTY);
    }

    @Contract("_ -> new")
    public static @NotNull GasObject of(Gas gasType) {
        return of(gasType, blockVolume());
    }

    public static long blockVolume() {
        return CommonProxy.blockVolume();
    }

    public boolean isEmpty() {
        return gasType() == Gas.EMPTY_GAS_HOLDER.value() || amount() == 0;
    }

    public static boolean isSameGasSameComponents(@NotNull GasObject first, @NotNull GasObject second) {
        return first.gasType == second.gasType && (first.isEmpty() && second.isEmpty() || Objects.equals(first.components, second.components));
    }
}
