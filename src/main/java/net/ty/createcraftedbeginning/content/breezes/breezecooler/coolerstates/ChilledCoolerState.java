package net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity.CoolantType;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe.CoolingData;
import org.jetbrains.annotations.NotNull;

public class ChilledCoolerState extends BaseCoolerState {
    public ChilledCoolerState(int remainingTime, boolean isCreative) {
        super(remainingTime, isCreative);
    }

    @Override
    public void tick(BreezeCoolerBlockEntity cooler) {
        super.tick(cooler);
        Level level = cooler.getLevel();
        if (level == null || isCreative || remainingTime <= 0) {
            return;
        }

        remainingTime--;
        if (remainingTime <= 0) {
            cooler.setCoolerState(new InactiveCoolerState());
            return;
        }
        if (level.getGameTime() % NOTIFY_INTERVAL != 0) {
            return;
        }

        cooler.notifyUpdate();
    }

    @Override
    public FrostLevel getFrostLevel() {
        return FrostLevel.CHILLED;
    }

    @Override
    public InteractionResult onItemInsert(@NotNull BreezeCoolerBlockEntity cooler, ItemStack stack, boolean forceOverflow, boolean simulate) {
        Level level = cooler.getLevel();
        if (level == null) {
            return InteractionResult.FAIL;
        }

        CoolingData data = CoolingRecipe.getResultingCoolingTime(level, stack, null);
        int time = data.time();
        if (time == 0) {
            return InteractionResult.FAIL;
        }

        int newTime = remainingTime + time;
        if (!forceOverflow && Mth.abs(newTime) > OVERFLOW_THRESHOLD) {
            return InteractionResult.FAIL;
        }

        if (!simulate) {
            remainingTime = Mth.clamp(newTime, 0, BreezeCoolerBlockEntity.MAX_COOLANT_CAPACITY);
            if (remainingTime == 0) {
                cooler.setCoolerState(new InactiveCoolerState());
            }
            if (time > 0) {
                cooler.playSound();
                cooler.spawnParticleBurst();
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public CoolantType getCoolantType() {
        return CoolantType.NORMAL;
    }
}
