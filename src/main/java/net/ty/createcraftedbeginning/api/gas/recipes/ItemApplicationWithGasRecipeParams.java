package net.ty.createcraftedbeginning.api.gas.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Function;

public class ItemApplicationWithGasRecipeParams extends ProcessingWithGasRecipeParams {
    public static MapCodec<ItemApplicationWithGasRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(codec(ItemApplicationWithGasRecipeParams::new).forGetter(Function.identity()), Codec.BOOL.optionalFieldOf("keep_held_item", false).forGetter(ItemApplicationWithGasRecipeParams::keepHeldItem)).apply(instance, (params, keepHeldItem) -> {
        params.keepHeldItem = keepHeldItem;
        return params;
    }));

    public static StreamCodec<RegistryFriendlyByteBuf, ItemApplicationWithGasRecipeParams> STREAM_CODEC = streamCodec(ItemApplicationWithGasRecipeParams::new);

    protected boolean keepHeldItem;

    protected final boolean keepHeldItem() {
        return keepHeldItem;
    }

    @Override
    protected void encode(RegistryFriendlyByteBuf buffer) {
        super.encode(buffer);
        ByteBufCodecs.BOOL.encode(buffer, keepHeldItem);
    }

    @Override
    protected void decode(RegistryFriendlyByteBuf buffer) {
        super.decode(buffer);
        keepHeldItem = ByteBufCodecs.BOOL.decode(buffer);
    }
}