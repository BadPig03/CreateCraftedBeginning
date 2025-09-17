package net.ty.createcraftedbeginning.compat.jade.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.data.CCBGasRegistry;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import snownee.jade.util.CommonProxy;

import java.util.Objects;

public record JadeGasObject(Gas type, long amount) {
	public static final Codec<JadeGasObject> CODEC = RecordCodecBuilder.create(instance -> instance.group(CCBGasRegistry.GAS_REGISTRY.byNameCodec().fieldOf("type").forGetter(JadeGasObject::type), Codec.LONG.fieldOf("amount").forGetter(JadeGasObject::amount)).apply(instance, JadeGasObject::of));

	public static long bucketVolume() {
        return CommonProxy.bucketVolume();
    }

	public static long blockVolume() {
		return CommonProxy.blockVolume();
	}

	@Contract(" -> new")
	public static @NotNull JadeGasObject empty() {
		return of(Gas.EMPTY_GAS_HOLDER.value(), 0);
	}

	@Contract("_ -> new")
	public static @NotNull JadeGasObject of(Gas gas) {
		return of(gas, blockVolume());
	}

	@Contract("_, _ -> new")
	public static @NotNull JadeGasObject of(Gas gas, long amount) {
		return new JadeGasObject(gas, amount);
	}

	public JadeGasObject(Gas type, long amount) {
		this.type = type;
		this.amount = amount;
		Objects.requireNonNull(type);
	}

	public boolean isEmpty() {
		return type() == Gas.EMPTY_GAS_HOLDER.value() || amount() == 0;
	}
}
