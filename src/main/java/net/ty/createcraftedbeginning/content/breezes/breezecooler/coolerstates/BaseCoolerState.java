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

    public BaseCoolerState(int remainingTime, boolean isCreative) {
        this.remainingTime = remainingTime;
        this.isCreative = isCreative;
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

        SmartFluidTank tank = cooler.getTankInventory();
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) {
            return true;
        }

        if (isCreative) {
            return true;
        }

        BlockPos pos = cooler.getBlockPos();
        if (fluid.getFluidType().getTemperature() >= BreezeCoolerBlockEntity.getDangerousFluidTemperature()) {
            ItemStack emptyCooler = new ItemStack(CCBBlocks.EMPTY_BREEZE_COOLER_BLOCK.get());
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, emptyCooler);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25f, 1);
            level.playSound(null, pos, SoundEvents.BREEZE_DEATH, SoundSource.BLOCKS, 0.25f, 1);
            cooler.getAdvancementBehaviour().awardPlayer(CCBAdvancements.A_MURDER);
            level.destroyBlock(pos, false);
            return false;
        }

        CoolingData data = cooler.getFluidCoolingData(fluid);
        int time = data.time();
        int amount = data.amount();
        if (time <= 0 || amount <= 0) {
            return true;
        }

        int maxCapacity = BreezeCoolerBlockEntity.getMaxCoolantCapacity();
        int creditedTime = Math.min(time, maxCapacity);
        int availableCapacity = Math.max(0, maxCapacity - remainingTime);
        int batchesByFluid = fluid.getAmount() / amount;
        int batchesByCapacity = availableCapacity / creditedTime;
        int batches = Math.min(batchesByFluid, batchesByCapacity);
        if (batches <= 0) {
            return true;
        }

        remainingTime += batches * creditedTime;
        tank.drain(batches * amount, FluidAction.EXECUTE);
        if (getFrostLevel() == FrostLevel.RIMING) {
            cooler.setCoolerState(new ChilledCoolerState(remainingTime, false));
        }
        else {
            cooler.markCoolingChanged();
        }
        cooler.playCoolingEffects();
        return true;
    }

    public abstract FrostLevel getFrostLevel();

    public abstract CoolantType getCoolantType();

    public InteractionResult onItemInsert(BreezeCoolerBlockEntity cooler, ItemStack stack, boolean forceOverflow, boolean simulate) {
        return InteractionResult.PASS;
    }

    public abstract boolean onSnowballImpact(BreezeCoolerBlockEntity cooler);
}
