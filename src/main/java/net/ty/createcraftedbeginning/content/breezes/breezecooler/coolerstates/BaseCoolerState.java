package net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates;

import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity.CoolantType;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerController.CoolingSyncMode;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe.CoolingData;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BaseCoolerState {
    protected static final String COMPOUND_KEY_REMAINING_TIME = "RemainingTime";
    protected static final String COMPOUND_KEY_IS_CREATIVE = "isCreative";

    protected int remainingTime;
    protected boolean isCreative;

    protected BaseCoolerState(int remainingTime, boolean isCreative) {
        this.remainingTime = remainingTime;
        this.isCreative = isCreative;
    }

    protected static boolean shouldRejectAutomaticOverflow(int remainingTime, long newTime) {
        long currentTimeMagnitude = Math.abs((long) remainingTime);
        long newTimeMagnitude = Math.abs(newTime);
        return newTimeMagnitude > BreezeCoolerBlockEntity.getOverflowThreshold() && newTimeMagnitude >= currentTimeMagnitude;
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

    public boolean tick(BreezeCoolerBlockEntity cooler) {
        return tickFluid(cooler);
    }

    protected boolean tickFluid(BreezeCoolerBlockEntity cooler) {
        Level level = cooler.getLevel();
        if (level == null || level.isClientSide) {
            return true;
        }

        SmartFluidTank fluidTank = cooler.getTankInventory();
        FluidStack storedFluid = fluidTank.getFluid();
        if (storedFluid.isEmpty()) {
            return true;
        }

        if (isCreative) {
            return true;
        }

        BlockPos coolerPos = cooler.getBlockPos();
        if (storedFluid.getFluidType().getTemperature() >= BreezeCoolerBlockEntity.getDangerousFluidTemperature()) {
            ItemStack emptyCooler = new ItemStack(CCBBlocks.EMPTY_BREEZE_COOLER_BLOCK.get());
            Containers.dropItemStack(level, coolerPos.getX() + 0.5, coolerPos.getY() + 0.5, coolerPos.getZ() + 0.5, emptyCooler);
            level.playSound(null, coolerPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25f, 1);
            level.playSound(null, coolerPos, SoundEvents.BREEZE_DEATH, SoundSource.BLOCKS, 0.25f, 1);
            cooler.getAdvancementBehaviour().awardPlayer(CCBAdvancements.A_MURDER);
            level.destroyBlock(coolerPos, false);
            return false;
        }

        CoolingData coolingData = cooler.getFluidCoolingData(storedFluid);
        int coolingTime = coolingData.time();
        int fluidAmount = coolingData.amount();
        if (coolingTime <= 0 || fluidAmount <= 0) {
            return true;
        }

        int maxCoolantCapacity = BreezeCoolerBlockEntity.getMaxCoolantCapacity();
        int creditedTime = Math.min(coolingTime, maxCoolantCapacity);
        int availableCapacity = Math.max(0, maxCoolantCapacity - remainingTime);
        int batchesByFluid = storedFluid.getAmount() / fluidAmount;
        int batchesByCapacity = availableCapacity / creditedTime;
        int batches = Math.min(batchesByFluid, batchesByCapacity);
        if (batches <= 0) {
            return true;
        }

        int consumedFluidAmount = batches * fluidAmount;
        FluidStack drainRequest = storedFluid.copyWithAmount(consumedFluidAmount);
        ResourceTransaction drainTransaction = new ResourceTransaction().add(ResourceTransaction.participant(() -> {
            FluidStack simulatedDrain = fluidTank.drain(drainRequest, FluidAction.SIMULATE);
            return simulatedDrain.getAmount() == consumedFluidAmount && FluidStack.isSameFluidSameComponents(simulatedDrain, drainRequest);
        }, () -> fluidTank.getFluid().copy(), () -> {
            FluidStack executedDrain = fluidTank.drain(drainRequest, FluidAction.EXECUTE);
            return executedDrain.getAmount() == consumedFluidAmount && FluidStack.isSameFluidSameComponents(executedDrain, drainRequest);
        }, tankSnapshot -> fluidTank.setFluid(tankSnapshot.copy())));
        if (!drainTransaction.commit()) {
            return true;
        }

        updateRemainingTime(cooler, remainingTime + (long) batches * creditedTime, CoolingSyncMode.IMMEDIATE);
        cooler.playCoolingEffects();
        return true;
    }

    protected final void updateRemainingTime(BreezeCoolerBlockEntity cooler, long newRemainingTime, CoolingSyncMode syncMode) {
        Level level = cooler.getLevel();
        if (isCreative || level != null && level.isClientSide && !cooler.isVirtual()) {
            return;
        }

        int maxCoolantCapacity = BreezeCoolerBlockEntity.getMaxCoolantCapacity();
        int clampedRemainingTime = Math.clamp(newRemainingTime, 0, maxCoolantCapacity);
        if (clampedRemainingTime <= 0 && getFrostLevel() == FrostLevel.CHILLED) {
            cooler.setCoolerState(new InactiveCoolerState());
            return;
        }

        if (clampedRemainingTime > 0 && getFrostLevel() == FrostLevel.RIMING) {
            cooler.setCoolerState(new ChilledCoolerState(clampedRemainingTime, false));
            return;
        }

        if (clampedRemainingTime == remainingTime) {
            return;
        }

        remainingTime = clampedRemainingTime;
        cooler.onCoolingTimeChanged(syncMode);
    }

    public abstract FrostLevel getFrostLevel();

    public abstract CoolantType getCoolantType();

    public InteractionResult onItemInsert(BreezeCoolerBlockEntity cooler, ItemStack stack, boolean forceOverflow, boolean simulate) {
        return InteractionResult.PASS;
    }

    public abstract boolean onSnowballImpact(BreezeCoolerBlockEntity cooler);
}
