package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.CreativeChamberState;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingData;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeChamberController {
    private static final int WIND_STATE_SYNC_INTERVAL = 20;
    private final BreezeChamberBlockEntity chamber;
    private boolean controllerActiveInitialized;
    private boolean lastControllerActive;
    private int lastWindLevel = -1;

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

        BlockState state = chamber.getBlockState();
        if (!(state.getBlock() instanceof BreezeChamberBlock block) || block.canSurvive(state, level, chamber.getBlockPos())) {
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

        WindChargingData data = WindChargingRecipe.getWindChargingData(level, stack);
        InteractionResult result = chamber.getChamberStateInternal().onItemInsert(chamber, stack, data, forceOverflow, simulate);
        return result == InteractionResult.SUCCESS ? InteractionResultHolder.success(data.recipeResult().copy()) : InteractionResultHolder.fail(ItemStack.EMPTY);
    }

    void syncWindProgress() {
        Level level = chamber.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        chamber.setChanged();
        long phase = level.getGameTime() + chamber.getBlockPos().asLong();
        if (Math.floorMod(phase, WIND_STATE_SYNC_INTERVAL) != 0) {
            return;
        }

        chamber.notifyUpdate();
    }

    void loadFromItem(ItemStack stack) {
        int maxWindCapacity = BreezeChamberBlockEntity.getMaxWindCapacity();
        int time = Mth.clamp(stack.getOrDefault(CCBDataComponents.BREEZE_TIME, 0), -maxWindCapacity, maxWindCapacity);
        boolean creative = stack.getOrDefault(CCBDataComponents.BREEZE_CREATIVE, false);
        chamber.setChamberState(BreezeChamberSerialization.stateForItem(time, creative));
        if (time == 0) {
            return;
        }

        chamber.playSound(time < 0);
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

        BlockState state = chamber.getBlockState();
        WindLevel windLevel = chamber.getChamberStateInternal().getWindLevel();
        if (state.getValue(BreezeChamberBlock.WIND_LEVEL) == windLevel) {
            return;
        }

        level.setBlockAndUpdate(chamber.getBlockPos(), state.setValue(BreezeChamberBlock.WIND_LEVEL, windLevel));
    }

    private void updateGasCapabilityState() {
        Level level = chamber.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        boolean controllerActive = chamber.getGasProcessorInternal().isControllerActive();
        if (!controllerActiveInitialized) {
            controllerActiveInitialized = true;
            lastControllerActive = controllerActive;
            return;
        }

        if (controllerActive == lastControllerActive) {
            return;
        }

        lastControllerActive = controllerActive;
        level.invalidateCapabilities(chamber.getBlockPos());
    }

    private void updateAirtightAssemblyDriver() {
        Level level = chamber.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        int newLevel = chamber.getWindRemainingLevel();
        if (newLevel == lastWindLevel) {
            return;
        }

        lastWindLevel = newLevel;
        AirtightTankBlock.updateTankState(level, chamber.getBlockPos().below());
    }
}
