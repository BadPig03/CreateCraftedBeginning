package net.ty.createcraftedbeginning.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record FractionationTowerOutput(int layer, ItemStack item, FluidStack fluid, GasStack gas) {
    public static final int MAX_LAYER_OFFSET = 8;
    public static final Codec<FractionationTowerOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.intRange(1, MAX_LAYER_OFFSET).fieldOf("layer").forGetter(FractionationTowerOutput::layer), ItemStack.CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(FractionationTowerOutput::item), FluidStack.CODEC.optionalFieldOf("fluid", FluidStack.EMPTY).forGetter(FractionationTowerOutput::fluid), GasStack.CODEC.optionalFieldOf("gas", GasStack.EMPTY).forGetter(FractionationTowerOutput::gas)).apply(instance, FractionationTowerOutput::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FractionationTowerOutput> STREAM_CODEC = StreamCodec.of((buffer, output) -> {
        buffer.writeVarInt(output.layer);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, output.item);
        FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, output.fluid);
        GasStack.OPTIONAL_STREAM_CODEC.encode(buffer, output.gas);
    }, buffer -> new FractionationTowerOutput(buffer.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer), FluidStack.OPTIONAL_STREAM_CODEC.decode(buffer), GasStack.OPTIONAL_STREAM_CODEC.decode(buffer)));

    public FractionationTowerOutput {
        item = item.copy();
        fluid = fluid.copy();
        gas = gas.copy();
    }

    @Override
    public ItemStack item() {
        return item.copy();
    }

    @Override
    public FluidStack fluid() {
        return fluid.copy();
    }

    @Override
    public GasStack gas() {
        return gas.copy();
    }

    public boolean isValid() {
        int products = 0;
        if (!item.isEmpty()) {
            products++;
        }
        if (!fluid.isEmpty()) {
            products++;
        }
        if (!gas.isEmpty()) {
            products++;
        }
        return layer > 0 && layer <= MAX_LAYER_OFFSET && products == 1;
    }

    public boolean isSameProduct(FractionationTowerOutput other) {
        return !item.isEmpty() && ItemStack.isSameItemSameComponents(item, other.item) || !fluid.isEmpty() && FluidStack.isSameFluidSameComponents(fluid, other.fluid) || !gas.isEmpty() && GasStack.isSameGasSameComponents(gas, other.gas);
    }
}
