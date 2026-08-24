package net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity.CoolantType;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerController.CoolingSyncMode;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe.CoolingData;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class InactiveCoolerState extends BaseCoolerState {
    public InactiveCoolerState() {
        super(0, false);
    }

    @Override
    public FrostLevel getFrostLevel() {
        return FrostLevel.RIMING;
    }

    @Override
    public CoolantType getCoolantType() {
        return CoolantType.NONE;
    }

    @Override
    public InteractionResult onItemInsert(BreezeCoolerBlockEntity cooler, ItemStack stack, boolean forceOverflow, boolean simulate) {
        Level level = cooler.getLevel();
        if (level == null) {
            return InteractionResult.FAIL;
        }

        CoolingData coolingData = CoolingRecipe.getCoolingTime(level, stack, null);
        int coolingTime = coolingData.time();
        if (coolingTime <= 0) {
            return InteractionResult.FAIL;
        }

        long newRemainingTime = (long) remainingTime + coolingTime;
        if (!forceOverflow && shouldRejectAutomaticOverflow(remainingTime, newRemainingTime)) {
            return InteractionResult.FAIL;
        }

        if (simulate) {
            return InteractionResult.SUCCESS;
        }

        updateRemainingTime(cooler, newRemainingTime, CoolingSyncMode.IMMEDIATE);
        cooler.playSound();
        cooler.spawnParticleBurst();
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onSnowballImpact(BreezeCoolerBlockEntity cooler) {
        Level level = cooler.getLevel();
        if (level == null || level.isClientSide) {
            return false;
        }

        int snowballCoolingTime = BreezeCoolerBlockEntity.getSnowballCoolingTime();
        if (snowballCoolingTime <= 0 || shouldRejectAutomaticOverflow(remainingTime, snowballCoolingTime)) {
            return false;
        }

        updateRemainingTime(cooler, snowballCoolingTime, CoolingSyncMode.IMMEDIATE);
        cooler.playSound();
        cooler.spawnParticleBurst();
        return true;
    }
}
