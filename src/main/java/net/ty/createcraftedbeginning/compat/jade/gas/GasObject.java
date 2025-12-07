package net.ty.createcraftedbeginning.compat.jade.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import snownee.jade.util.CommonProxy;

public record GasObject(Gas type, long amount) {
    public static final Codec<GasObject> CODEC = RecordCodecBuilder.create(instance -> instance.group(CCBGasRegistries.GAS_REGISTRY.byNameCodec().fieldOf("type").forGetter(GasObject::type), Codec.LONG.fieldOf("amount").forGetter(GasObject::amount)).apply(instance, GasObject::of));

    public static long bucketVolume() {
        return CommonProxy.bucketVolume();
    }

    @Contract(" -> new")
    public static @NotNull GasObject empty() {
        return of(Gas.EMPTY_GAS_HOLDER.value(), 0);
    }

    @Contract("_, _ -> new")
    public static @NotNull GasObject of(Gas gas, long amount) {
        return new GasObject(gas, amount);
    }

    @Contract("_ -> new")
    public static @NotNull GasObject of(Gas gas) {
        return of(gas, blockVolume());
    }

    public static long blockVolume() {
        return CommonProxy.blockVolume();
    }

    public boolean isEmpty() {
        return type() == Gas.EMPTY_GAS_HOLDER.value() || amount() == 0;
    }
}
