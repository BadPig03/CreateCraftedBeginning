package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant;
import net.ty.createcraftedbeginning.recipe.trie.ReactorKettleRecipeTrie;
import net.ty.createcraftedbeginning.recipe.trie.ReactorKettleRecipeTrieFinder;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBDamageSources;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class AirtightReactorKettleUtils {
    private AirtightReactorKettleUtils() {
    }

    public static @NotNull BlockPos getMaster(@NotNull BlockPos pos, @NotNull BlockState state) {
        if (state.is(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_BLOCK)) {
            return pos.offset(state.getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION).getPosition());
        }
        else if (state.is(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG_BLOCK)) {
            return pos.offset(state.getValue(AirtightReactorKettleStructuralCogBlock.STRUCTURAL_POSITION).getPosition());
        }
        return pos;
    }

    public static float getMinSpeedRequired() {
        return SpeedLevel.FAST.getSpeedValue();
    }

    public static @NotNull List<ReactorKettleRecipe> getMatchingRecipes(@NotNull AirtightReactorKettleBlockEntity kettle, Object cacheObject) {
        List<ReactorKettleRecipe> list = new ArrayList<>();
        if (kettle.isEmpty()) {
            return list;
        }

        Level level = kettle.getLevel();
        try {
            IItemHandler availableItems = kettle.getItemCapability();
            IFluidHandler availableFluids = kettle.getFluidCapability();
            IGasHandler availableGases = kettle.getGasCapability();
            if (availableItems == null && availableFluids == null && availableGases == null) {
                return list;
            }

            ReactorKettleRecipeTrie<?> trie = ReactorKettleRecipeTrieFinder.get(cacheObject, level, $ -> true);
            Set<AbstractVariant> availableVariants = ReactorKettleRecipeTrie.getVariants(availableItems, availableFluids, availableGases);
            for (Recipe<?> r : trie.lookup(availableVariants)) {
                if (!(r instanceof ReactorKettleRecipe kettleRecipe) || !ReactorKettleRecipe.match(kettle, kettleRecipe)) {
                    continue;
                }

                list.add(kettleRecipe);
            }
        } catch (Exception e) {
            CreateCraftedBeginning.LOGGER.error("Failed to get recipe trie, falling back to slow logic");
            list.clear();
            for (RecipeHolder<? extends Recipe<?>> r : RecipeFinder.get(cacheObject, level, $ -> true)) {
                if (!(r.value() instanceof ReactorKettleRecipe kettleRecipe) || !ReactorKettleRecipe.match(kettle, kettleRecipe)) {
                    continue;
                }

                list.add(kettleRecipe);
            }
        }
        return list;
    }

    public static float getTotalFluidUnits(@NotNull Couple<SmartFluidTankBehaviour> fluidTanks, float partialTicks) {
        int fluids = 0;
        float totalUnits = 0;
        for (SmartFluidTankBehaviour behaviour : fluidTanks) {
            if (behaviour == null) {
                continue;
            }

            for (TankSegment tankSegment : behaviour.getTanks()) {
                if (tankSegment.getRenderedFluid().isEmpty()) {
                    continue;
                }

                float units = tankSegment.getTotalUnits(partialTicks);
                if (units < 1) {
                    continue;
                }

                totalUnits += units;
                fluids++;
            }
        }

        if (fluids == 0 || totalUnits < 1) {
            return 0;
        }
        return totalUnits;
    }

    public static void insertItemEntity(@NotNull AirtightReactorKettleStructuralBlockEntity structure, ItemEntity itemEntity) {
        AirtightReactorKettleBlockEntity master = structure.getMasterBlockEntity();
        if (master == null) {
            return;
        }

        ItemStack insertItem = ItemHandlerHelper.insertItem(master.getInventories().getFirst(), itemEntity.getItem().copy(), false);
        if (insertItem.isEmpty()) {
            itemEntity.discard();
            return;
        }

        itemEntity.setItem(insertItem);
    }

    public static void hurtInsideLivingEntities(@NotNull AirtightReactorKettleStructuralBlockEntity structure, LivingEntity livingEntity) {
        AirtightReactorKettleBlockEntity master = structure.getMasterBlockEntity();
        if (master == null) {
            return;
        }

        float damage = master.getDamage();
        if (damage == 0) {
            return;
        }

        livingEntity.hurt(CCBDamageSources.reactorKettleMixer(master.getLevel()), damage);
    }

    public static ItemInteractionResult getUseItemOnResult(@NotNull AirtightReactorKettleStructuralBlockEntity structural, Level level, Player player, BlockPos pos, InteractionHand hand, ItemStack stack) {
        AirtightReactorKettleBlockEntity master = structural.getMasterBlockEntity();
        if (master == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!stack.isEmpty()) {
            if (FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, master) || GenericItemEmptying.canItemBeEmptied(level, stack)) {
                return ItemInteractionResult.SUCCESS;
            }
            if (FluidHelper.tryFillItemFromBE(level, player, hand, stack, master) || GenericItemFilling.canItemBeFilled(level, stack)) {
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        IItemHandlerModifiable inventory = master.getItemCapability();
        if (inventory == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        boolean success = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stackInSlot = inventory.getStackInSlot(slot);
            if (stackInSlot.isEmpty()) {
                continue;
            }

            player.getInventory().placeItemBackInInventory(stackInSlot);
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            success = true;
        }
        if (success) {
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f + level.getRandom().nextFloat());
        }
        return ItemInteractionResult.SUCCESS;
    }

    public static void refreshOtherFilters(@NotNull AirtightReactorKettleStructuralBlockEntity structural, ItemStack stack) {
        AirtightReactorKettleBlockEntity master = structural.getMasterBlockEntity();
        if (master == null || master.isFilterChanged()) {
            return;
        }

        Level level = structural.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        master.notifyFiltersChanged();
        master.notifyContentsChanged();
        BlockPos centerPos = master.getBlockPos().below();
        for (Direction direction : Iterate.horizontalDirections) {
            BlockPos filterPos = centerPos.relative(direction);
            if (filterPos.equals(structural.getBlockPos())) {
                continue;
            }
            if (!(level.getBlockEntity(filterPos) instanceof AirtightReactorKettleStructuralBlockEntity structuralBlockEntity)) {
                continue;
            }

            structuralBlockEntity.getFilteringBehaviour().setFilter(stack);
        }
    }

    public static boolean canModifyFilter(@NotNull AirtightReactorKettleStructuralBlockEntity structural) {
        AirtightReactorKettleBlockEntity master = structural.getMasterBlockEntity();
        return master != null && !master.isFilterChanged();
    }
}
