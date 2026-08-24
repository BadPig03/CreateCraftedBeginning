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

    private static void applyRemainingTime(BreezeChamberBlockEntity chamber, long updatedTime) {
        int maxWindCapacity = BreezeChamberBlockEntity.getMaxWindCapacity();
        int clampedTime = Math.clamp(updatedTime, -maxWindCapacity, maxWindCapacity);
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

    private static boolean shouldRejectAutomaticOverflow(int remainingTime, long updatedTime) {
        long currentMagnitude = Math.abs((long) remainingTime);
        long updatedMagnitude = Math.abs(updatedTime);
        if (updatedMagnitude <= BreezeChamberBlockEntity.getOverflowThreshold()) {
            return false;
        }

        if (updatedMagnitude < currentMagnitude) {
            return false;
        }

        int effectiveThreshold = BreezeChamberBlockEntity.getMaxEffectiveThreshold();
        return remainingTime < 0 || remainingTime >= effectiveThreshold || updatedTime < effectiveThreshold;
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

    public InteractionResult onItemInsert(BreezeChamberBlockEntity chamber, ItemStack stack, WindChargingData chargingData, boolean forceOverflow, boolean simulate) {
        return insertWindCharge(chamber, stack, chargingData, forceOverflow, simulate);
    }

    protected InteractionResult insertWindCharge(BreezeChamberBlockEntity chamber, ItemStack stack, WindChargingData chargingData, boolean forceOverflow, boolean simulate) {
        if (chargingData.amount() <= 0) {
            return InteractionResult.FAIL;
        }

        if (isCreative && chargingData.action() != WindChargingAction.CYCLE_CREATIVE) {
            return InteractionResult.PASS;
        }
        return switch (chargingData.action()) {
            case CHARGE -> insertCharge(chamber, stack, chargingData.time(), forceOverflow, simulate);
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

        long updatedTime = (long) remainingTime + chargingTime;
        if (!forceOverflow && shouldRejectAutomaticOverflow(remainingTime, updatedTime)) {
            return InteractionResult.FAIL;
        }

        if (simulate) {
            return InteractionResult.SUCCESS;
        }

        awardFeedingAdvancements(chamber, stack, chargingTime);
        applyRemainingTime(chamber, updatedTime);
        boolean isIllCharge = chargingTime < 0;
        chamber.playSound(isIllCharge);
        chamber.spawnParticleBurst(isIllCharge);
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

        ChargerType nextChargerType = CreativeChamberState.getNextChargeType(getChargerType());
        chamber.setChamberState(nextChargerType == ChargerType.NONE ? new InactiveChamberState() : new CreativeChamberState(nextChargerType));
        boolean isIllCharge = nextChargerType == ChargerType.BAD;
        chamber.spawnParticleBurst(isIllCharge);
        chamber.playSound(isIllCharge);
    }
}
