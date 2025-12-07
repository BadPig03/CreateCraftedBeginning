package net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver;

import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.InternalFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletInventory.InternalStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver.AirtightAssemblyDriverCore.MAX_LEVEL;

public class AirtightAssemblyDriverResidueManager {
    private static final int FAILURE_MAX_COUNTDOWN = 100;
    private static final int MAX_COUNTDOWN = 10;
    private static final float LEVEL_MULTIPLIER = 20.0f;

    private static final String COMPOUND_KEY_RESIDUE_COUNTDOWN = "ResidueCountdown";
    private static final String COMPOUND_KEY_FAILURE_COUNTDOWN = "FailureCountdown";
    private static final String COMPOUND_KEY_SUCCESS_COUNT = "SuccessCount";
    private static final String COMPOUND_KEY_OUTLETS_POSITIONS = "OutletsPositions";

    private final AirtightAssemblyDriverCore driverCore;
    private final Set<BlockPos> outletsPositions = new HashSet<>();

    private int successCount;
    private int residueCountdown = MAX_COUNTDOWN;
    private int failureCountdown = FAILURE_MAX_COUNTDOWN;

    public AirtightAssemblyDriverResidueManager(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    public void tick(Level level) {
        if (failureCountdown > 0) {
            failureCountdown--;
        }

        if (outletsPositions.isEmpty() && driverCore.getLevelCalculator().getResidueLevel() > 0) {
            removeResidueLevel(true);
            return;
        }

        if (residueCountdown > 0) {
            residueCountdown--;
            return;
        }

        scanOutlets(level);
        residueCountdown = MAX_COUNTDOWN;
    }

    public void reset() {
        successCount = 0;
        residueCountdown = MAX_COUNTDOWN;
        failureCountdown = FAILURE_MAX_COUNTDOWN;
        driverCore.getLevelCalculator().update();
    }

    public void updateOutletsPositions(Set<BlockPos> newPositions) {
        outletsPositions.clear();
        outletsPositions.addAll(newPositions);
    }

    public void applyRemovalPenalty(boolean clear) {
        removeResidueLevel(clear);
    }

    public CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt(COMPOUND_KEY_RESIDUE_COUNTDOWN, residueCountdown);
        compoundTag.putInt(COMPOUND_KEY_FAILURE_COUNTDOWN, failureCountdown);
        compoundTag.putInt(COMPOUND_KEY_SUCCESS_COUNT, successCount);
        compoundTag.putLongArray(COMPOUND_KEY_OUTLETS_POSITIONS, outletsPositions.stream().mapToLong(BlockPos::asLong).toArray());
        return compoundTag;
    }

    public void read(@NotNull CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_RESIDUE_COUNTDOWN)) {
            residueCountdown = compoundTag.getInt(COMPOUND_KEY_RESIDUE_COUNTDOWN);
        }
        if (compoundTag.contains(COMPOUND_KEY_FAILURE_COUNTDOWN)) {
            failureCountdown = compoundTag.getInt(COMPOUND_KEY_FAILURE_COUNTDOWN);
        }
        if (compoundTag.contains(COMPOUND_KEY_SUCCESS_COUNT)) {
            successCount = compoundTag.getInt(COMPOUND_KEY_SUCCESS_COUNT);
        }
        if (!compoundTag.contains(COMPOUND_KEY_OUTLETS_POSITIONS)) {
            return;
        }

        outletsPositions.clear();
        for (long posLong : compoundTag.getLongArray(COMPOUND_KEY_OUTLETS_POSITIONS)) {
            outletsPositions.add(BlockPos.of(posLong));
        }
    }

    private void scanOutlets(@NotNull Level level) {
        int attachedOutlets = outletsPositions.size();
        if (attachedOutlets == 0) {
            removeResidueLevel(true);
            return;
        }

        boolean allResidueSuccess = true;
        for (BlockPos outletPos : outletsPositions) {
            if (!tryGenerateResidue(outletPos, level)) {
                allResidueSuccess = false;
            }
        }

        if (allResidueSuccess) {
            successCount++;
            int residueLevel = driverCore.getLevelCalculator().getResidueLevel();
            if (residueLevel == 0) {
                addResidueLevel();
                return;
            }
            if (successCount >= MAX_COUNTDOWN * 2) {
                addResidueLevel();
            }
            return;
        }

        if (failureCountdown != 0) {
            return;
        }

        removeResidueLevel(false);
    }

    private void addResidueLevel() {
        AirtightAssemblyDriverLevelCalculator levelCalculator = driverCore.getLevelCalculator();
        levelCalculator.updateResidueLevel(Mth.clamp(levelCalculator.getResidueLevel() + 1, 0, MAX_LEVEL));
        reset();
    }

    private void removeResidueLevel(boolean clear) {
        AirtightAssemblyDriverLevelCalculator levelCalculator = driverCore.getLevelCalculator();
        levelCalculator.updateResidueLevel(clear ? 0 : Mth.clamp(levelCalculator.getResidueLevel() - 1, 0, MAX_LEVEL));
        reset();
    }

    private boolean tryGenerateResidue(BlockPos blockPos, @NotNull Level level) {
        if (!(level.getBlockEntity(blockPos) instanceof ResidueOutletBlockEntity outlet)) {
            return false;
        }
        if (!(outlet.getFluidTankBehaviour().getCapability() instanceof InternalFluidHandler fluidHandler)) {
            return false;
        }
        if (!(outlet.getInventory().getCapability() instanceof InternalStackHandler itemStackHandler)) {
            return false;
        }

        AirtightAssemblyDriverFlowMeter flowMeter = driverCore.getFlowMeter();
        float gasSupply = flowMeter.getGasSupply();
        if (gasSupply == 0) {
            return false;
        }

        int currentLevel = driverCore.getLevelCalculator().getCurrentLevel();
        if (currentLevel == 0) {
            return fluidHandler.getFluidInTank(0).isEmpty() && itemStackHandler.getStackInSlot(0).isEmpty();
        }

        Gas gas = flowMeter.getGasType().getGas();
        FluidStack outputFluidStack = gas.getOutputFluidStack();
        boolean fluidSuccess;
        if (outputFluidStack.isEmpty()) {
            fluidSuccess = true;
        }
        else {
            int fillAmount = getResidueGenerationRate();
            int filledAmount = fluidHandler.forceFill(outputFluidStack.copyWithAmount(fillAmount), FluidAction.EXECUTE);
            fluidSuccess = filledAmount >= fillAmount;
        }

        ItemStack outputItemStack = gas.getOutputItemStack();
        boolean itemSuccess;
        if (outputItemStack.isEmpty()) {
            itemSuccess = true;
        }
        else {
            float itemCount = getResidueGenerationRate() / LEVEL_MULTIPLIER / MAX_LEVEL;
            itemSuccess = outlet.getInventory().addPartialItemCount(itemCount, outputItemStack);
        }
        return fluidSuccess && itemSuccess;
    }

    private int getResidueGenerationRate() {
        int currentLevel = driverCore.getLevelCalculator().getCurrentLevel();
        int count = outletsPositions.size();
        return currentLevel == 0 || count == 0 ? 0 : (int) (currentLevel * LEVEL_MULTIPLIER / count);
    }
}
