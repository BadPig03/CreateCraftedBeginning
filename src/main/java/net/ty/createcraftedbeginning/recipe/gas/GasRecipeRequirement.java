package net.ty.createcraftedbeginning.recipe.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.recipe.gas.ingredient.GasIngredient;
import net.ty.createcraftedbeginning.recipe.gas.ingredient.SizedGasIngredient;
import net.ty.createcraftedbeginning.recipe.pressure.PressureRequirement;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasRecipeRequirement(SizedGasIngredient sizedIngredient, PressureRequirement pressure) {
    public static final Codec<GasRecipeRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(GasIngredient.CODEC_NON_EMPTY.fieldOf("ingredient").forGetter(GasRecipeRequirement::ingredient), NeoForgeExtraCodecs.optionalFieldAlwaysWrite(Codec.LONG, "amount", GasUnits.GU_PER_KGU).forGetter(GasRecipeRequirement::amount), PressureRequirement.MAP_CODEC.forGetter(GasRecipeRequirement::pressure)).apply(instance, GasRecipeRequirement::of));
    public static final StreamCodec<RegistryFriendlyByteBuf, GasRecipeRequirement> STREAM_CODEC = StreamCodec.composite(GasIngredient.STREAM_CODEC, GasRecipeRequirement::ingredient, ByteBufCodecs.VAR_LONG, GasRecipeRequirement::amount, PressureRequirement.STREAM_CODEC, GasRecipeRequirement::pressure, GasRecipeRequirement::of);

    public GasRecipeRequirement {
        if (sizedIngredient.ingredient().isEmpty()) {
            throw new IllegalArgumentException("Gas recipe requirement ingredient cannot be empty.");
        }
    }

    @Contract("_ -> new")
    public static GasRecipeRequirement of(SizedGasIngredient ingredient) {
        return new GasRecipeRequirement(ingredient, PressureRequirement.NONE);
    }

    @Contract("_, _ -> new")
    public static GasRecipeRequirement of(SizedGasIngredient ingredient, PressureRequirement pressure) {
        return new GasRecipeRequirement(ingredient, pressure);
    }

    @Contract("_, _ -> new")
    public static GasRecipeRequirement of(GasIngredient ingredient, long amount) {
        return of(ingredient, amount, PressureRequirement.NONE);
    }

    @Contract("_, _, _ -> new")
    public static GasRecipeRequirement of(GasIngredient ingredient, long amount, PressureRequirement pressure) {
        return new GasRecipeRequirement(new SizedGasIngredient(ingredient, amount), pressure);
    }

    @Contract("_, _ -> new")
    public static GasRecipeRequirement of(Gas gasType, long amount) {
        return of(gasType, amount, PressureRequirement.NONE);
    }

    @Contract("_, _, _ -> new")
    public static GasRecipeRequirement of(Gas gasType, long amount, PressureRequirement pressure) {
        return new GasRecipeRequirement(SizedGasIngredient.of(gasType, amount), pressure);
    }

    @Contract("_, _ -> new")
    public static GasRecipeRequirement of(TagKey<Gas> tag, long amount) {
        return of(tag, amount, PressureRequirement.NONE);
    }

    @Contract("_, _, _ -> new")
    public static GasRecipeRequirement of(TagKey<Gas> tag, long amount, PressureRequirement pressure) {
        return new GasRecipeRequirement(SizedGasIngredient.of(tag, amount), pressure);
    }

    @Contract("_ -> new")
    public static GasRecipeRequirement of(GasStack stack) {
        return of(stack, PressureRequirement.NONE);
    }

    @Contract("_, _ -> new")
    public static GasRecipeRequirement of(GasStack stack, PressureRequirement pressure) {
        return new GasRecipeRequirement(SizedGasIngredient.of(stack), pressure);
    }

    public GasIngredient ingredient() {
        return sizedIngredient.ingredient();
    }

    public long amount() {
        return sizedIngredient.amount();
    }

    public boolean test(GasStack stack) {
        return sizedIngredient.test(stack);
    }

    public boolean test(GasStack stack, long pressurePa) {
        return sizedIngredient.test(stack) && pressure.allowsPressure(pressurePa);
    }

    public boolean test(Gas gasType) {
        return sizedIngredient.test(gasType);
    }

    public GasStack[] getGases() {
        return sizedIngredient.getGases();
    }

    @SuppressWarnings("unused")
    public GasStack getFirstGas() {
        return sizedIngredient.getFirstGas();
    }
}
