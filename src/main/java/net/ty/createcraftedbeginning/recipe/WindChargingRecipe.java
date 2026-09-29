package net.ty.createcraftedbeginning.recipe;

import com.google.common.base.Joiner;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WindChargingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {

    private final ProcessingRecipeParams recipeParams;
    private final WindChargingAction action;
    private final int priority;

    public WindChargingRecipe(ProcessingRecipeParams params) {
        this(params, WindChargingAction.CHARGE, 0);
    }

    public WindChargingRecipe(ProcessingRecipeParams params, WindChargingAction action, int priority) {
        super(CCBRecipeTypes.WIND_CHARGING, params);
        recipeParams = params;
        this.action = action;
        this.priority = priority;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return !input.isEmpty() && getIngredient().test(input.getItem(0));
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    public List<String> validate() {
        List<String> errors = super.validate();
        if (action == WindChargingAction.CHARGE && processingDuration == 0) {
            errors.add("Wind Charging recipes with action 'charge' must specify a non-zero processing_time.");
        }
        else if (action != WindChargingAction.CHARGE && processingDuration != 0) {
            errors.add("Wind Charging recipes with action '" + action.getSerializedName() + "' must not specify processing_time.");
        }
        return errors;
    }

    public Ingredient getIngredient() {
        return ingredients.getFirst();
    }

    public WindChargingAction getAction() {
        return action;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isBadFood() {
        return action == WindChargingAction.CHARGE && processingDuration < 0;
    }

    public enum WindChargingAction implements StringRepresentable {
        CHARGE,
        CLEAR_ILL,
        CYCLE_CREATIVE;

        private static final Codec<WindChargingAction> CODEC = StringRepresentable.fromEnum(WindChargingAction::values);
        private static final StreamCodec<RegistryFriendlyByteBuf, WindChargingAction> STREAM_CODEC = StreamCodec.of(FriendlyByteBuf::writeEnum, buffer -> buffer.readEnum(WindChargingAction.class));

        @Override
        public String getSerializedName() {
            return Lang.asId(name());
        }
    }

    static class Serializer implements RecipeSerializer<WindChargingRecipe> {
        private static final MapCodec<WindChargingRecipe> CODEC = RecordCodecBuilder.<WindChargingRecipe>mapCodec(instance -> instance.group(ProcessingRecipeParams.CODEC.forGetter(recipe -> recipe.recipeParams), WindChargingAction.CODEC.fieldOf("action").forGetter(WindChargingRecipe::getAction), Codec.INT.optionalFieldOf("priority", 0).forGetter(WindChargingRecipe::getPriority)).apply(instance, WindChargingRecipe::new)).validate(Serializer::validateRecipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, WindChargingRecipe> STREAM_CODEC = StreamCodec.of((buffer, recipe) -> {
            ProcessingRecipeParams.STREAM_CODEC.encode(buffer, recipe.recipeParams);
            WindChargingAction.STREAM_CODEC.encode(buffer, recipe.action);
            buffer.writeInt(recipe.priority);
        }, buffer -> new WindChargingRecipe(ProcessingRecipeParams.STREAM_CODEC.decode(buffer), WindChargingAction.STREAM_CODEC.decode(buffer), buffer.readInt()));

        @Override
        public MapCodec<WindChargingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WindChargingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static DataResult<WindChargingRecipe> validateRecipe(WindChargingRecipe recipe) {
            List<String> errors = recipe.validate();
            if (errors.isEmpty()) {
                return DataResult.success(recipe);
            }

            errors.addFirst(recipe.getClass().getSimpleName() + " failed validation:");
            return DataResult.error(() -> Joiner.on('\n').join(errors), recipe);
        }
    }
}
