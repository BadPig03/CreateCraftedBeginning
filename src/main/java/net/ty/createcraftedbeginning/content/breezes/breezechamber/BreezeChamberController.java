package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.CreativeChamberState;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingData;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeChamberController {
    private static final int WIND_STATE_SYNC_INTERVAL = 20;
    private final BreezeChamberBlockEntity chamber;
    private boolean hasControllerActiveState;
    private boolean wasControllerActive;
    private int previousWindLevel = -1;

    BreezeChamberController(BreezeChamberBlockEntity chamber) {
        this.chamber = chamber;
    }

    void tick() {
        Level level = chamber.getLevel();
        if (level == null) {
            return;
        }

        chamber.getChamberStateInternal().tick(chamber);
        if (!level.isClientSide) {
            updateAirtightAssemblyDriver();
            updateGasCapabilityState();
            return;
        }

        chamber.runClientTicker();
    }

    void lazyTick() {
        Level level = chamber.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState blockState = chamber.getBlockState();
        if (!(blockState.getBlock() instanceof BreezeChamberBlock block) || block.canSurvive(blockState, level, chamber.getBlockPos())) {
            return;
        }

        level.destroyBlock(chamber.getBlockPos(), true);
    }

    void onLoad() {
        Level level = chamber.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        syncWindLevelBlockState();
        updateAirtightAssemblyDriver();
        updateGasCapabilityState();
    }

    void onStateChanged() {
        chamber.setChanged();
        Level level = chamber.getLevel();
        if (level == null || level.isClientSide && !chamber.isVirtual()) {
            return;
        }

        syncWindLevelBlockState();
        updateAirtightAssemblyDriver();
        chamber.notifyUpdate();
    }

    InteractionResultHolder<ItemStack> tryUpdateChargerByItem(ItemStack stack, boolean forceOverflow, boolean simulate) {
        Level level = chamber.getLevel();
        if (level == null) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }

        WindChargingData chargingData = WindChargingRecipe.getWindChargingData(level, stack);
        InteractionResult interactionResult = chamber.getChamberStateInternal().onItemInsert(chamber, stack, chargingData, forceOverflow, simulate);
        if (interactionResult != InteractionResult.SUCCESS) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }
        return InteractionResultHolder.success(chargingData.recipeResult().copy());
    }

    void syncWindProgress() {
        Level level = chamber.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        chamber.setChanged();
        long syncPhase = level.getGameTime() + chamber.getBlockPos().asLong();
        if (Math.floorMod(syncPhase, WIND_STATE_SYNC_INTERVAL) != 0) {
            return;
        }

        chamber.notifyUpdate();
    }

    void loadFromItem(ItemStack stack) {
        int maxWindCapacity = BreezeChamberBlockEntity.getMaxWindCapacity();
        int remainingTime = CCBMathUtils.clampMagnitude(stack.getOrDefault(CCBDataComponents.BREEZE_TIME, 0), maxWindCapacity);
        boolean isCreative = stack.getOrDefault(CCBDataComponents.BREEZE_CREATIVE, false);
        chamber.setChamberState(BreezeChamberSerialization.stateForItem(remainingTime, isCreative));
        if (remainingTime == 0) {
            return;
        }

        chamber.playSound(remainingTime < 0);
    }

    void switchToGaleState() {
        if (!(chamber.getLevel() instanceof PonderLevel)) {
            return;
        }

        chamber.setChamberState(new CreativeChamberState(ChargerType.NORMAL));
        chamber.spawnParticleBurst(false);
    }

    void switchToIllState() {
        if (!(chamber.getLevel() instanceof PonderLevel)) {
            return;
        }

        chamber.setChamberState(new CreativeChamberState(ChargerType.BAD));
        chamber.spawnParticleBurst(true);
    }

    private void syncWindLevelBlockState() {
        Level level = chamber.getLevel();
        if (level == null) {
            return;
        }

        BlockState blockState = chamber.getBlockState();
        WindLevel windLevel = chamber.getChamberStateInternal().getWindLevel();
        if (blockState.getValue(BreezeChamberBlock.WIND_LEVEL) == windLevel) {
            return;
        }

        level.setBlockAndUpdate(chamber.getBlockPos(), blockState.setValue(BreezeChamberBlock.WIND_LEVEL, windLevel));
    }

    private void updateGasCapabilityState() {
        Level level = chamber.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        boolean isControllerActive = chamber.getGasProcessorInternal().isControllerActive();
        if (!hasControllerActiveState) {
            hasControllerActiveState = true;
            wasControllerActive = isControllerActive;
            return;
        }

        if (isControllerActive == wasControllerActive) {
            return;
        }

        wasControllerActive = isControllerActive;
        level.invalidateCapabilities(chamber.getBlockPos());
    }

    private void updateAirtightAssemblyDriver() {
        Level level = chamber.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        int windLevel = chamber.getWindRemainingLevel();
        if (windLevel == previousWindLevel) {
            return;
        }

        previousWindLevel = windLevel;
        AirtightTankBlock.updateTankState(level, chamber.getBlockPos().below());
    }
}
