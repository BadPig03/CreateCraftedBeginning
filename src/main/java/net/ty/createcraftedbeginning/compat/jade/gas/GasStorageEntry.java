package net.ty.createcraftedbeginning.compat.jade.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentPatch;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasRegistries;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasStorageEntry(Gas gasType, long amount, DataComponentPatch components) {
    public static final Codec<GasStorageEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(GasRegistries.GAS_REGISTRY.byNameCodec().fieldOf("type").forGetter(GasStorageEntry::gasType), Codec.LONG.fieldOf("amount").forGetter(GasStorageEntry::amount), DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(GasStorageEntry::components)).apply(instance, GasStorageEntry::of));

    @Contract(" -> new")
    public static GasStorageEntry empty() {
        return of(Gas.EMPTY_GAS_HOLDER.value(), 0);
    }

    public static GasStorageEntry of(Gas gasType, long amount, DataComponentPatch components) {
        return new GasStorageEntry(gasType, amount, components);
    }

    @Contract("_, _ -> new")
    public static GasStorageEntry of(Gas gasType, long amount) {
        return new GasStorageEntry(gasType, amount, DataComponentPatch.EMPTY);
    }

    @Contract("_ -> new")
    public static GasStorageEntry of(Gas gasType) {
        return of(gasType, GasUnits.GU_PER_KGU);
    }

    public static boolean isSameGasSameComponents(GasStorageEntry first, GasStorageEntry second) {
        return first.gasType == second.gasType && (first.isEmpty() && second.isEmpty() || Objects.equals(first.components, second.components));
    }

    public boolean isEmpty() {
        return gasType() == Gas.EMPTY_GAS_HOLDER.value() || amount() == 0;
    }
}
