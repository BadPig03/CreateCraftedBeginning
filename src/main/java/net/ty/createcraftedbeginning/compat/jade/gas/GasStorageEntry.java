package net.ty.createcraftedbeginning.compat.jade.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentPatch;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasRegistries;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record GasStorageEntry(Gas gasType, long amount, DataComponentPatch components) {
    static final Codec<GasStorageEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(GasRegistries.GAS_REGISTRY.byNameCodec().fieldOf("type").forGetter(GasStorageEntry::gasType), Codec.LONG.fieldOf("amount").forGetter(GasStorageEntry::amount), DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(GasStorageEntry::components)).apply(instance, GasStorageEntry::of));

    @Contract(" -> new")
    static GasStorageEntry empty() {
        return of(Gas.EMPTY_GAS_HOLDER.value());
    }

    static GasStorageEntry of(Gas gasType, long amount, DataComponentPatch components) {
        return new GasStorageEntry(gasType, amount, components);
    }

    @Contract("_ -> new")
    static GasStorageEntry of(Gas gasType) {
        return new GasStorageEntry(gasType, 0, DataComponentPatch.EMPTY);
    }

    boolean isEmpty() {
        return gasType() == Gas.EMPTY_GAS_HOLDER.value() || amount() == 0;
    }
}
