package net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity.CoolantType;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe.CoolingData;
import org.jetbrains.annotations.NotNull;

public class InactiveCoolerState extends BaseCoolerState {
    public InactiveCoolerState() {
        super(0, false);
    }

    @Override
    public void tick(BreezeCoolerBlockEntity cooler) {
        super.tick(cooler);

        Level level = cooler.getLevel();
        if (level == null || level.getGameTime() % NOTIFY_INTERVAL != 0) {
            return;
        }

        cooler.notifyUpdate();
    }

    @Override
    public FrostLevel getFrostLevel() {
        return FrostLevel.RIMING;
    }

    @Override
    public InteractionResult onItemInsert(@NotNull BreezeCoolerBlockEntity cooler, ItemStack stack, boolean forceOverflow, boolean simulate) {
        Level level = cooler.getLevel();
        if (level == null) {
            return InteractionResult.FAIL;
        }

        CoolingData data = CoolingRecipe.getCoolingTime(level, stack, null);
        int time = data.time();
        if (time <= 0) {
            return InteractionResult.FAIL;
        }

        if (!simulate) {
            cooler.setCoolerState(new ChilledCoolerState(time, false));
            cooler.playSound();
            cooler.spawnParticleBurst();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onSnowballImpact(@NotNull BreezeCoolerBlockEntity cooler) {
        Level level = cooler.getLevel();
        if (level == null || level.isClientSide) {
            return false;
        }

        cooler.setCoolerState(new ChilledCoolerState(20, false));
        cooler.playSound();
        cooler.spawnParticleBurst();
        return true;
    }

    @Override
    public CoolantType getCoolantType() {
        return CoolantType.NONE;
    }
}
