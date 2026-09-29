package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.RecipeValidationContext;
import dev.latvian.mods.kubejs.recipe.component.SimpleRecipeComponent;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.recipe.match.ReplacementMatchInfo;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class ProcessingOutputComponent extends SimpleRecipeComponent<ProcessingOutput> {
    ProcessingOutputComponent(RecipeComponentType<?> type) {
        super(type, ProcessingOutput.CODEC_NEW, TypeInfo.of(ItemRecipeOutput.class).or(ItemStackComponent.ITEM_STACK.instance().typeInfo()));
    }

    @Override
    public ProcessingOutput wrap(RecipeScriptContext cx, Object from) {
        if (from instanceof ItemRecipeOutput(ProcessingOutput output)) {
            return output;
        }

        if (from instanceof ProcessingOutput output) {
            return output;
        }

        return new ProcessingOutput(ItemStackComponent.ITEM_STACK.instance().wrap(cx, from), 1);
    }

    @Override
    public boolean matches(RecipeMatchContext cx, ProcessingOutput value, ReplacementMatchInfo match) {
        return ItemStackComponent.ITEM_STACK.instance().matches(cx, value.getStack(), match);
    }

    @Override
    public ProcessingOutput replace(RecipeScriptContext cx, ProcessingOutput original, ReplacementMatchInfo match, Object with) {
        if (!matches(cx, original, match)) {
            return original;
        }

        return new ProcessingOutput(ItemStackComponent.ITEM_STACK.instance().wrap(cx, with), original.getChance());
    }

    @Override
    public void validate(RecipeValidationContext cx, ProcessingOutput value) {
        ItemStackComponent.ITEM_STACK.instance().validate(cx, value.getStack());
        float chance = value.getChance();
        if (Float.isFinite(chance) && chance > 0 && chance <= 1) {
            return;
        }

        throw new IllegalArgumentException("Output chance must be finite and in (0, 1]; got " + chance + '.');
    }
}
