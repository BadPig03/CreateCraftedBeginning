package net.ty.createcraftedbeginning.mixin;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.recipes.PressingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Optional;

@Mixin(MechanicalPressBlockEntity.class)
public abstract class MechanicalPressBlockEntityMixin {
    @Shadow
    public PressingBehaviour pressingBehaviour;

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "tryProcessInWorld", at = @At("HEAD"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private void ccb$tryProcessInWorld(@NotNull ItemEntity itemEntity, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        MechanicalPressBlockEntity self = (MechanicalPressBlockEntity) (Object) this;
        ItemStack item = itemEntity.getItem();
        Level level = self.getLevel();
        Optional<RecipeHolder<PressingWithGasRecipe>> recipeWithGas = getRecipeWithGas(item, level);
        if (recipeWithGas.isPresent()) {
            if (simulate) {
                cir.setReturnValue(true);
                return;
            }

            processRecipeInWorld(level, itemEntity, recipeWithGas.get());
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static Optional<RecipeHolder<PressingWithGasRecipe>> getRecipeWithGas(ItemStack item, Level level) {
        return SequencedAssemblyWithGasRecipe.getRecipe(level, item, CCBRecipeTypes.PRESSING_WITH_GAS.getType(), PressingWithGasRecipe.class);
    }

    @Unique
    private void processRecipeInWorld(Level level, @NotNull ItemEntity itemEntity, @NotNull RecipeHolder<PressingWithGasRecipe> recipe) {
        ItemStack item = itemEntity.getItem();
        ItemStack itemCreated = ItemStack.EMPTY;
        pressingBehaviour.particleItems.add(item);
        if (canProcessInBulk() || item.getCount() == 1) {
            RecipeApplier.applyRecipeOn(itemEntity, recipe.value(), true);
            itemCreated = itemEntity.getItem().copy();
        }
        else {
            for (ItemStack result : RecipeApplier.applyRecipeOn(level, item.copyWithCount(1), recipe.value(), true)) {
                if (itemCreated.isEmpty()) {
                    itemCreated = result.copy();
                }
                ItemEntity created = new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), result);
                created.setDefaultPickUpDelay();
                created.setDeltaMovement(VecHelper.offsetRandomly(Vec3.ZERO, level.random, 0.05f));
                level.addFreshEntity(created);
            }
            item.shrink(1);
        }
        if (itemCreated.isEmpty()) {
            return;
        }

        onItemPressed(itemCreated);
    }

    @Shadow
    public abstract void onItemPressed(ItemStack result);

    @Shadow
    public abstract boolean canProcessInBulk();

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "tryProcessOnBelt", at = @At("HEAD"), cancellable = true)
    private void ccb$tryProcessOnBelt(@NotNull TransportedItemStack input, List<ItemStack> outputList, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        MechanicalPressBlockEntity self = (MechanicalPressBlockEntity) (Object) this;
        Level level = self.getLevel();

        Optional<RecipeHolder<PressingWithGasRecipe>> recipeWithGas = getRecipeWithGas(input.stack, level);
        if (recipeWithGas.isPresent()) {
            if (simulate) {
                cir.setReturnValue(true);
                return;
            }

            processRecipeOnBelt(level, input, outputList, recipeWithGas.get());
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void processRecipeOnBelt(Level level, @NotNull TransportedItemStack input, List<ItemStack> outputList, @NotNull RecipeHolder<PressingWithGasRecipe> recipe) {
        pressingBehaviour.particleItems.add(input.stack);
        List<ItemStack> outputs = RecipeApplier.applyRecipeOn(level, canProcessInBulk() ? input.stack : input.stack.copyWithCount(1), recipe.value(), true);

        for (ItemStack created : outputs) {
            if (!created.isEmpty()) {
                onItemPressed(created);
                break;
            }
        }

        outputList.addAll(outputs);
    }
}