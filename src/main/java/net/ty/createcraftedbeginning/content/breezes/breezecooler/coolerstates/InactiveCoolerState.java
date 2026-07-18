package net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
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

        CoolingData data = CoolingRecipe.getCoolingTime(level, stack, null);
        int time = data.time();
        if (time <= 0) {
            return InteractionResult.FAIL;
        }

        if (simulate) {
            return InteractionResult.SUCCESS;
        }

        cooler.setCoolerState(new ChilledCoolerState(Mth.clamp(time, 1, BreezeCoolerBlockEntity.getMaxCoolantCapacity()), false));
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
        if (snowballCoolingTime <= 0) {
            return false;
        }

        cooler.setCoolerState(new ChilledCoolerState(Math.min(snowballCoolingTime, BreezeCoolerBlockEntity.getMaxCoolantCapacity()), false));
        cooler.playSound();
        cooler.spawnParticleBurst();
        return true;
    }
}
