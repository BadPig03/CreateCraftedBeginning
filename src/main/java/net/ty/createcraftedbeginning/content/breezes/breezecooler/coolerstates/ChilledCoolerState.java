package net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity.CoolantType;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe.CoolingData;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ChilledCoolerState extends BaseCoolerState {
    public ChilledCoolerState(int remainingTime, boolean isCreative) {
        super(remainingTime, isCreative);
    }

    @Override
    public boolean tick(BreezeCoolerBlockEntity cooler) {
        if (!super.tick(cooler)) {
            return false;
        }

        Level level = cooler.getLevel();
        if (level == null || cooler.isRemoved() || isCreative || remainingTime <= 0) {
            return true;
        }

        remainingTime--;
        if (remainingTime <= 0) {
            cooler.setCoolerState(new InactiveCoolerState());
            return true;
        }
        if (!level.isClientSide) {
            cooler.syncCoolingProgress();
        }
        return true;
    }

    @Override
    public FrostLevel getFrostLevel() {
        return FrostLevel.CHILLED;
    }

    @Override
    public CoolantType getCoolantType() {
        return CoolantType.NORMAL;
    }

    @Override
    public InteractionResult onItemInsert(BreezeCoolerBlockEntity cooler, ItemStack stack, boolean forceOverflow, boolean simulate) {
        Level level = cooler.getLevel();
        if (level == null) {
            return InteractionResult.FAIL;
        }

        CoolingData data = CoolingRecipe.getCoolingTime(level, stack, null);
        int time = data.time();
        if (time == 0) {
            return InteractionResult.FAIL;
        }

        long newTime = (long) remainingTime + time;
        if (!forceOverflow && Math.abs(newTime) > BreezeCoolerBlockEntity.getOverflowThreshold()) {
            return InteractionResult.FAIL;
        }

        if (simulate) {
            return InteractionResult.SUCCESS;
        }

        remainingTime = Math.clamp(newTime, 0, BreezeCoolerBlockEntity.getMaxCoolantCapacity());
        if (remainingTime == 0) {
            cooler.setCoolerState(new InactiveCoolerState());
        }
        if (time > 0) {
            cooler.playSound();
            cooler.spawnParticleBurst();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onSnowballImpact(BreezeCoolerBlockEntity cooler) {
        Level level = cooler.getLevel();
        if (level == null || level.isClientSide) {
            return false;
        }

        int snowballCoolingTime = BreezeCoolerBlockEntity.getSnowballCoolingTime();
        if (snowballCoolingTime <= 0) {
            return false;
        }

        long newTime = (long) remainingTime + snowballCoolingTime;
        if (Math.abs(newTime) > BreezeCoolerBlockEntity.getOverflowThreshold()) {
            return false;
        }

        remainingTime = Math.clamp(newTime, 0, BreezeCoolerBlockEntity.getMaxCoolantCapacity());
        cooler.playSound();
        cooler.spawnParticleBurst();
        return true;
    }
}
