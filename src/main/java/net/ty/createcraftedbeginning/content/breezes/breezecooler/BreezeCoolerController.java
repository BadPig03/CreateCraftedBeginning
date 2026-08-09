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
final class BreezeCoolerController {
    private static final int COOLING_STATE_SYNC_INTERVAL = 20;
    private static final int COOLING_EFFECT_INTERVAL = 10;
    private final BreezeCoolerBlockEntity cooler;
    private boolean stockKeeper;
    private long lastCoolingEffectTime = Long.MIN_VALUE;

    BreezeCoolerController(BreezeCoolerBlockEntity cooler) {
        this.cooler = cooler;
    }

    void tick() {
        Level level = cooler.getLevel();
        if (level == null || !cooler.getCurrentState().tick(cooler)) {
            return;
        }
        if (level.isClientSide) {
            cooler.runClientTicker();
        }
    }

    void lazyTick() {
        stockKeeper = BlazeBurnerBlockEntity.getStockTicker(cooler.getLevel(), cooler.getBlockPos()) != null;
    }

    void onLoad() {
        Level level = cooler.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        syncFrostLevelBlockState();
    }

    void onStateChanged() {
        cooler.setChanged();
        Level level = cooler.getLevel();
        if (level == null || level.isClientSide && !cooler.isVirtual()) {
            return;
        }
        syncFrostLevelBlockState();
        cooler.notifyUpdate();
    }

    boolean tryUpdateCoolantByItem(ItemStack stack, boolean forceOverflow, boolean simulate) {
        if (stack.is(CCBItems.CREATIVE_ICE_CREAM)) {
            if (simulate) {
                return true;
            }
            CoolantType type = CreativeCoolerState.getNextCoolantType(cooler.getCurrentState().getCoolantType());
            cooler.setCoolerState(type == CoolantType.NONE ? new InactiveCoolerState() : new CreativeCoolerState(type));
            cooler.spawnParticleBurst();
            cooler.playSound();
            return true;
        }
        InteractionResult result = cooler.getCurrentState().onItemInsert(cooler, stack, forceOverflow, simulate);
        if (result != InteractionResult.SUCCESS) {
            return false;
        }
        if (simulate) {
            return true;
        }
        if (cooler.getLevel() != null && !cooler.getLevel().isClientSide && stack.is(CCBItemTags.ICE_CREAMS.tag)) {
            cooler.getAdvancementBehaviour().awardPlayer(CCBAdvancements.FROZEN_AMBROSIA);
        }
        cooler.notifyUpdate();
        return true;
    }

    void markCoolingChanged() {
        cooler.setChanged();
    }

    void syncCoolingProgress() {
        Level level = cooler.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        cooler.setChanged();
        long phase = level.getGameTime() + cooler.getBlockPos().asLong();
        if (Math.floorMod(phase, COOLING_STATE_SYNC_INTERVAL) == 0) {
            cooler.notifyUpdate();
        }
    }

    void playCoolingEffects() {
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

    void switchToChilledState() {
        if (!(cooler.getLevel() instanceof PonderLevel)) {
            return;
        }
        cooler.setCoolerState(new CreativeCoolerState(CoolantType.NORMAL));
        cooler.spawnParticleBurst();
    }

    boolean isStockKeeper() {
        return stockKeeper;
    }

    private void syncFrostLevelBlockState() {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = cooler.getBlockState();
        FrostLevel frostLevel = cooler.getCurrentState().getFrostLevel();
        if (state.getValue(BreezeCoolerBlock.FROST_LEVEL) != frostLevel) {
            level.setBlockAndUpdate(cooler.getBlockPos(), state.setValue(BreezeCoolerBlock.FROST_LEVEL, frostLevel));
        }
    }
}
