package net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates;

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

public class InactiveChamberState extends BaseChamberState {
    public InactiveChamberState() {
        super(0, false);
    }

    @Override
    public void tick(BreezeChamberBlockEntity chamber) {
    }

    @Override
    public InteractionResult onItemInsert(@NotNull BreezeChamberBlockEntity chamber, ItemStack stack, boolean forceOverflow, boolean simulate) {
        Level level = chamber.getLevel();
        if (level == null) {
            return InteractionResult.FAIL;
        }

        WindChargingData data = WindChargingRecipe.getWindChargingTime(level, stack);
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

        if (!simulate) {
            if (time > 0) {
                chamber.setChamberState(new GaleChamberState(time, false));
                chamber.playSound(false);
                chamber.spawnParticleBurst(false);
            }
            else {
                chamber.setChamberState(new IllChamberState(time, false));
                chamber.playSound(true);
                chamber.spawnParticleBurst(true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public WindLevel getWindLevel() {
        return WindLevel.CALM;
    }

    @Override
    public ChargerType getChargerType() {
        return ChargerType.NONE;
    }
}
