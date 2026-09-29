package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasRegistries;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.pressure.PressureRequirement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasRecipeValue(String id, long amount, PressureRequirement pressure) {
    public GasRecipeValue {
        ResourceLocation.parse(id.startsWith("#") ? id.substring(1) : id);
        if (amount <= 0) {
            throw new IllegalArgumentException("Gas amount must be positive; got " + amount + " GU for gas '" + id + "'.");
        }
    }

    public static GasRecipeValue of(String id, long amount) {
        return new GasRecipeValue(id, amount, PressureRequirement.NONE);
    }

    public GasRecipeValue minimumPressure(long pressurePa) {
        return new GasRecipeValue(id, amount, new PressureRequirement(Optional.of(pressurePa), pressure.maximumPressurePa()));
    }

    public GasRecipeValue maximumPressure(long pressurePa) {
        return new GasRecipeValue(id, amount, new PressureRequirement(pressure.minimumPressurePa(), Optional.of(pressurePa)));
    }

    public GasRecipeRequirement asIngredient() {
        if (id.startsWith("#")) {
            return GasRecipeRequirement.of(TagKey.create(GasRegistries.GAS_REGISTRY_KEY, ResourceLocation.parse(id.substring(1))), amount, pressure);
        }

        return GasRecipeRequirement.of(createStack(), pressure);
    }

    public GasStack asStack() {
        if (id.startsWith("#") || !pressure.equals(PressureRequirement.NONE)) {
            throw new IllegalArgumentException("Gas output must use a concrete gas ID without pressure conditions; got ID '" + id + "' and " + pressure + '.');
        }

        return createStack();
    }

    private GasStack createStack() {
        Gas gas = GasRegistries.GAS_REGISTRY.getOptional(ResourceLocation.parse(id)).orElseThrow(() -> new IllegalArgumentException("Gas ID must be registered; got '" + id + "'."));
        GasStack stack = new GasStack(gas, amount);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Gas must not be empty; got '" + id + "'.");
        }

        return stack;
    }
}
