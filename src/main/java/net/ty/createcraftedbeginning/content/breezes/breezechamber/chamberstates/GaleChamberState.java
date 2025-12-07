package net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingData;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.NotNull;

public class GaleChamberState extends BaseChamberState {
    private int energizationTimer;

    public GaleChamberState(int remainingTime, boolean isCreative) {
        super(remainingTime, isCreative);
    }

    @Override
    public void tick(BreezeChamberBlockEntity chamber) {
        super.tick(chamber);
        Level level = chamber.getLevel();
        if (level == null || isCreative || remainingTime <= 0) {
            return;
        }

        remainingTime--;
        if (remainingTime <= 0) {
            chamber.setChamberState(new InactiveChamberState());
            return;
        }
        if (level.getGameTime() % NOTIFY_INTERVAL != 0) {
            return;
        }

        energizationTick(chamber);
        chamber.notifyUpdate();
    }

    public void energizationTick(@NotNull BreezeChamberBlockEntity chamber) {
        if (chamber.isControllerActive()) {
            return;
        }

        energizationTimer++;
        if (energizationTimer < NOTIFY_INTERVAL) {
            return;
        }

        chamber.doEnergization();
        energizationTimer = 0;
    }

    @Override
    public WindLevel getWindLevel() {
        return WindLevel.GALE;
    }

    @Override
    public ChargerType getChargerType() {
        return ChargerType.NORMAL;
    }

    @Override
    public InteractionResult onItemInsert(@NotNull BreezeChamberBlockEntity chamber, ItemStack stack, boolean forceOverflow, boolean simulate) {
        Level level = chamber.getLevel();
        if (level == null) {
            return InteractionResult.FAIL;
        }

        WindChargingData data = WindChargingRecipe.getResultingWindChargingTime(level, stack);
        boolean isMilky = data.isMilky();
        if (isMilky) {
            return InteractionResult.PASS;
        }

        int time = data.time();
        if (time == 0) {
            return InteractionResult.FAIL;
        }

        CCBAdvancementBehaviour advancementBehaviour = chamber.getAdvancementBehaviour();
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            advancementBehaviour.awardPlayer(CCBAdvancements.A_ROYAL_FEAST);
        }
        if (time < 0) {
            advancementBehaviour.awardPlayer(CCBAdvancements.BAD_APPLE);
        }

        int newTime = remainingTime + time;
        if (!forceOverflow && Mth.abs(newTime) > OVERFLOW_THRESHOLD) {
            return InteractionResult.FAIL;
        }

        if (!simulate) {
            remainingTime = Mth.clamp(newTime, -BreezeChamberBlockEntity.MAX_WIND_CAPACITY, BreezeChamberBlockEntity.MAX_WIND_CAPACITY);
            if (remainingTime == 0) {
                chamber.setChamberState(new InactiveChamberState());
            }
            else if (remainingTime < 0) {
                chamber.setChamberState(new IllChamberState(remainingTime, isCreative));
            }

            if (time > 0) {
                chamber.playSound(false);
                chamber.spawnParticleBurst(false);
            }
            else {
                chamber.playSound(true);
                chamber.spawnParticleBurst(true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
