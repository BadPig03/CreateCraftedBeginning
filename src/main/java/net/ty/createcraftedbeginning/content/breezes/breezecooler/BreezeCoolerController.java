package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity.CoolantType;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.CreativeCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.InactiveCoolerState;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BreezeCoolerController {
    private static final int COOLING_STATE_SYNC_INTERVAL = 20;
    private static final int COOLING_EFFECT_INTERVAL = 10;
    private final BreezeCoolerBlockEntity cooler;
    private boolean stockKeeper;
    private long lastCoolingEffectTime = Long.MIN_VALUE;

    public BreezeCoolerController(BreezeCoolerBlockEntity cooler) {
        this.cooler = cooler;
    }

    public void tick() {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }

        if (level.isClientSide && !cooler.isVirtual()) {
            cooler.runClientTicker();
            return;
        }

        if (!cooler.getCurrentState().tick(cooler) || !level.isClientSide) {
            return;
        }

        cooler.runClientTicker();
    }

    public void lazyTick() {
        stockKeeper = BlazeBurnerBlockEntity.getStockTicker(cooler.getLevel(), cooler.getBlockPos()) != null;
    }

    public void onLoad() {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            cooler.refreshClientCoolingPredictionBase();
            return;
        }

        syncFrostLevelBlockState();
    }

    public void onStateChanged() {
        cooler.setChanged();
        Level level = cooler.getLevel();
        if (level == null || level.isClientSide && !cooler.isVirtual()) {
            return;
        }

        syncFrostLevelBlockState();
        cooler.notifyUpdate();
    }

    public boolean tryUpdateCoolantByItem(ItemStack stack, boolean forceOverflow, boolean simulate) {
        Level level = cooler.getLevel();
        boolean isClientPrediction = level != null && level.isClientSide && !cooler.isVirtual();
        if (stack.is(CCBItems.CREATIVE_ICE_CREAM)) {
            if (simulate || isClientPrediction) {
                return true;
            }

            CoolantType nextCoolantType = CreativeCoolerState.getNextCoolantType(cooler.getCurrentState().getCoolantType());
            cooler.setCoolerState(nextCoolantType == CoolantType.NONE ? new InactiveCoolerState() : new CreativeCoolerState(nextCoolantType));
            cooler.spawnParticleBurst();
            cooler.playSound();
            return true;
        }

        InteractionResult insertResult = cooler.getCurrentState().onItemInsert(cooler, stack, forceOverflow, simulate || isClientPrediction);
        if (insertResult != InteractionResult.SUCCESS) {
            return false;
        }

        if (simulate || isClientPrediction) {
            return true;
        }

        if (level != null && !level.isClientSide && stack.is(CCBItemTags.ICE_CREAMS.tag)) {
            cooler.getAdvancementBehaviour().awardPlayer(CCBAdvancements.FROZEN_AMBROSIA);
        }
        return true;
    }

    public void onCoolingTimeChanged(CoolingSyncMode syncMode) {
        if (syncMode == CoolingSyncMode.IMMEDIATE) {
            cooler.setChanged();
            cooler.notifyUpdate();
            return;
        }

        syncCoolingProgress();
    }

    private void syncCoolingProgress() {
        Level level = cooler.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        cooler.setChanged();
        long syncPhase = level.getGameTime() + cooler.getBlockPos().asLong();
        if (Math.floorMod(syncPhase, COOLING_STATE_SYNC_INTERVAL) != 0) {
            return;
        }

        cooler.notifyUpdate();
    }

    public void playCoolingEffects() {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }

        long gameTime = level.getGameTime();
        if (lastCoolingEffectTime != Long.MIN_VALUE && gameTime - lastCoolingEffectTime < COOLING_EFFECT_INTERVAL) {
            return;
        }

        lastCoolingEffectTime = gameTime;
        cooler.playSound();
        cooler.spawnParticleBurst();
    }

    public void switchToChilledState() {
        if (!(cooler.getLevel() instanceof PonderLevel)) {
            return;
        }

        cooler.setCoolerState(new CreativeCoolerState(CoolantType.NORMAL));
        cooler.spawnParticleBurst();
    }

    public boolean isStockKeeper() {
        return stockKeeper;
    }

    private void syncFrostLevelBlockState() {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }

        BlockState currentBlockState = cooler.getBlockState();
        FrostLevel frostLevel = cooler.getCurrentState().getFrostLevel();
        if (currentBlockState.getValue(BreezeCoolerBlock.FROST_LEVEL) == frostLevel) {
            return;
        }

        level.setBlockAndUpdate(cooler.getBlockPos(), currentBlockState.setValue(BreezeCoolerBlock.FROST_LEVEL, frostLevel));
    }

    public enum CoolingSyncMode {
        IMMEDIATE,
        PERIODIC
    }
}
