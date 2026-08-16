package net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingAction;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingData;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BaseChamberState {
    protected static final String COMPOUND_KEY_REMAINING_TIME = "RemainingTime";
    protected static final String COMPOUND_KEY_IS_CREATIVE = "isCreative";
    protected final boolean isCreative;
    protected int remainingTime;

    protected BaseChamberState(int remainingTime, boolean isCreative) {
        this.remainingTime = remainingTime;
        this.isCreative = isCreative;
    }

    private static void awardFeedingAdvancements(BreezeChamberBlockEntity chamber, ItemStack stack, int chargingTime) {
        Level level = chamber.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        CCBAdvancementBehaviour advancementBehaviour = chamber.getAdvancementBehaviour();
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            advancementBehaviour.awardPlayer(CCBAdvancements.LUXURY_TREAT);
        }
        if (chargingTime >= 0) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.BAD_APPLE);
    }

    private static void applyRemainingTime(BreezeChamberBlockEntity chamber, long newTime) {
        int maxWindCapacity = BreezeChamberBlockEntity.getMaxWindCapacity();
        int clampedTime = Math.clamp(newTime, -maxWindCapacity, maxWindCapacity);
        if (clampedTime > 0) {
            chamber.setChamberState(new GaleChamberState(clampedTime, false));
        }
        else if (clampedTime < 0) {
            chamber.setChamberState(new IllChamberState(clampedTime, false));
        }
        else {
            chamber.setChamberState(new InactiveChamberState());
        }
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public boolean isCreative() {
        return isCreative;
    }

    public void save(CompoundTag compoundTag) {
        compoundTag.putInt(COMPOUND_KEY_REMAINING_TIME, remainingTime);
        compoundTag.putBoolean(COMPOUND_KEY_IS_CREATIVE, isCreative);
    }

    public void tick(BreezeChamberBlockEntity chamber) {
    }

    public abstract WindLevel getWindLevel();

    public abstract ChargerType getChargerType();

    public InteractionResult onItemInsert(BreezeChamberBlockEntity chamber, ItemStack stack, WindChargingData data, boolean forceOverflow, boolean simulate) {
        return insertWindCharge(chamber, stack, data, forceOverflow, simulate);
    }

    protected InteractionResult insertWindCharge(BreezeChamberBlockEntity chamber, ItemStack stack, WindChargingData data, boolean forceOverflow, boolean simulate) {
        if (data.amount() <= 0) {
            return InteractionResult.FAIL;
        }

        if (isCreative && data.action() != WindChargingAction.CYCLE_CREATIVE) {
            return InteractionResult.PASS;
        }
        return switch (data.action()) {
            case CHARGE -> insertCharge(chamber, stack, data.time(), forceOverflow, simulate);
            case CLEAR_ILL -> clearIll(chamber, stack, simulate);
            case CYCLE_CREATIVE -> {
                cycleCreative(chamber, simulate);
                yield InteractionResult.SUCCESS;
            }
        };
    }

    protected InteractionResult insertCharge(BreezeChamberBlockEntity chamber, ItemStack stack, int chargingTime, boolean forceOverflow, boolean simulate) {
        if (chargingTime == 0) {
            return InteractionResult.FAIL;
        }

        long newTime = (long) remainingTime + chargingTime;
        if (remainingTime != 0 && !forceOverflow && Math.abs(newTime) > BreezeChamberBlockEntity.getOverflowThreshold()) {
            return InteractionResult.FAIL;
        }

        if (simulate) {
            return InteractionResult.SUCCESS;
        }

        awardFeedingAdvancements(chamber, stack, chargingTime);
        applyRemainingTime(chamber, newTime);
        boolean bad = chargingTime < 0;
        chamber.playSound(bad);
        chamber.spawnParticleBurst(bad);
        return InteractionResult.SUCCESS;
    }

    protected InteractionResult clearIll(BreezeChamberBlockEntity chamber, ItemStack stack, boolean simulate) {
        if (getChargerType() != ChargerType.BAD) {
            return InteractionResult.PASS;
        }

        if (simulate) {
            return InteractionResult.SUCCESS;
        }

        Level level = chamber.getLevel();
        chamber.setChamberState(new InactiveChamberState());
        chamber.playSound(false);
        chamber.spawnParticleBurst(false);
        if (level == null || level.isClientSide || !stack.is(Items.MILK_BUCKET)) {
            return InteractionResult.SUCCESS;
        }

        chamber.getAdvancementBehaviour().awardPlayer(CCBAdvancements.UNIVERSAL_ANTIDOTE);
        return InteractionResult.SUCCESS;
    }

    protected void cycleCreative(BreezeChamberBlockEntity chamber, boolean simulate) {
        if (simulate) {
            return;
        }

        ChargerType chargerType = CreativeChamberState.getNextChargeType(getChargerType());
        chamber.setChamberState(chargerType == ChargerType.NONE ? new InactiveChamberState() : new CreativeChamberState(chargerType));
        boolean bad = chargerType == ChargerType.BAD;
        chamber.spawnParticleBurst(bad);
        chamber.playSound(bad);
    }
}
