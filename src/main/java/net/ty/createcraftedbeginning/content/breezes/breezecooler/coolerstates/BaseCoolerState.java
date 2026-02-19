package net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates;

import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity.CoolantType;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe.CoolingData;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

import static net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity.MAX_COOLANT_CAPACITY;

public abstract class BaseCoolerState {
    protected static final String COMPOUND_KEY_REMAINING_TIME = "RemainingTime";
    protected static final String COMPOUND_KEY_IS_CREATIVE = "isCreative";
    protected static final int OVERFLOW_THRESHOLD = MAX_COOLANT_CAPACITY / 2;
    protected static final int LAVA_TEMPERATURE = 1300;
    protected static final int NOTIFY_INTERVAL = 5;

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

    public void read(@NotNull CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_REMAINING_TIME)) {
            remainingTime = compoundTag.getInt(COMPOUND_KEY_REMAINING_TIME);
        }
        if (compoundTag.contains(COMPOUND_KEY_IS_CREATIVE)) {
            isCreative = compoundTag.getBoolean(COMPOUND_KEY_IS_CREATIVE);
        }
    }

    public void save(@NotNull CompoundTag compoundTag) {
        compoundTag.putInt(COMPOUND_KEY_REMAINING_TIME, remainingTime);
        compoundTag.putBoolean(COMPOUND_KEY_IS_CREATIVE, isCreative);
    }

    public void tick(BreezeCoolerBlockEntity cooler) {
        tickFluid(cooler);
        validateStates(cooler);
    }

    public void validateStates(@NotNull BreezeCoolerBlockEntity cooler) {
        Level level = cooler.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockPos pos = cooler.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(CCBBlocks.BREEZE_COOLER_BLOCK)) {
            return;
        }

        FrostLevel frostLevel = state.getValue(BreezeCoolerBlock.FROST_LEVEL);
        if (frostLevel == FrostLevel.RIMING || remainingTime != 0 || isCreative) {
            return;
        }

        cooler.setCoolerState(new InactiveCoolerState());
    }

    protected void tickFluid(@NotNull BreezeCoolerBlockEntity cooler) {
        Level level = cooler.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        SmartFluidTank fluidTank = cooler.getTankInventory();
        if (fluidTank.isEmpty()) {
            return;
        }

        FluidStack fluidStack = fluidTank.getFluid();
        if (fluidStack.isEmpty()) {
            return;
        }

        BlockPos pos = cooler.getBlockPos();
        if (fluidStack.getFluidType().getTemperature() >= LAVA_TEMPERATURE) {
            ItemStack emptyCoolerItem = new ItemStack(CCBBlocks.EMPTY_BREEZE_COOLER_BLOCK.get());
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, emptyCoolerItem);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25f, 1);
            level.playSound(null, pos, SoundEvents.BREEZE_DEATH, SoundSource.BLOCKS, 0.25f, 1);
            level.destroyBlock(pos, false);
            cooler.getAdvancementBehaviour().awardPlayer(CCBAdvancements.A_MURDER);
        }
        else {
            if (isCreative) {
                return;
            }

            CoolingData data = CoolingRecipe.getCoolingTime(level, null, fluidStack);
            int time = data.time();
            int amount = data.amount();
            if (time == 0 || amount == 0) {
                return;
            }

            int tankAmount = fluidStack.getAmount();
            int left = tankAmount - amount;
            if (left < 0) {
                return;
            }

            int newTime = remainingTime + time;
            if (newTime > MAX_COOLANT_CAPACITY) {
                return;
            }

            remainingTime = Mth.clamp(newTime, 0, MAX_COOLANT_CAPACITY);
            if (getFrostLevel() == FrostLevel.RIMING) {
                cooler.setCoolerState(new ChilledCoolerState(remainingTime, false));
            }
            fluidTank.drain(amount, FluidAction.EXECUTE);
            cooler.playSound();
            cooler.spawnParticleBurst();
            cooler.notifyUpdate();
        }
    }

    public abstract FrostLevel getFrostLevel();

    public abstract CoolantType getCoolantType();

    public abstract InteractionResult onItemInsert(BreezeCoolerBlockEntity cooler, ItemStack stack, boolean forceOverflow, boolean simulate);

    public abstract boolean onSnowballImpact(BreezeCoolerBlockEntity cooler);
}
