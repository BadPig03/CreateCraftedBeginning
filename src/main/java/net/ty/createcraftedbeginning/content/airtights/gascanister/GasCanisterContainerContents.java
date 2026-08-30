package net.ty.createcraftedbeginning.content.airtights.gascanister;

import com.simibubi.create.AllEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.IAirtightHatchCanister;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBEnchantments;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCanisterContainerContents implements IAirtightHatchCanister {
    public static final int ECONOMIZE_MAX_LEVEL = 3;
    private final ItemStack canister;

    protected GasStack gas;

    protected GasCanisterContainerContents(ItemStack canister) {
        this.canister = canister;
        gas = canister.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, GasStack.EMPTY).copy();
    }

    public static long getEconomizedDrainAmount(long logicalAmount, ItemStack itemStack) {
        if (logicalAmount <= 0) {
            return 0;
        }
        return (logicalAmount * getEconomizeCostPercent(itemStack) + 99) / 100;
    }

    public static long getLogicalAmountFromEconomizedDrain(long physicalDrain, ItemStack itemStack) {
        if (physicalDrain <= 0) {
            return 0;
        }
        return physicalDrain * 100 / getEconomizeCostPercent(itemStack);
    }

    protected static boolean isInvalidTank(int tankIndex) {
        return tankIndex != 0;
    }

    static long getDefaultCapacity() {
        return CCBConfig.server().airtights.maxCanisterCapacity.get() * GasAmounts.MILLIBUCKETS_PER_BUCKET;
    }

    private static long getEnchantedCapacity(ItemStack itemStack) {
        long capacityLevel = 0;
        for (Entry<Holder<Enchantment>> entry : itemStack.getTagEnchantments().entrySet()) {
            if (!entry.getKey().is(AllEnchantments.CAPACITY)) {
                continue;
            }

            capacityLevel = entry.getIntValue();
            break;
        }

        return getDefaultCapacity() * (1 + capacityLevel);
    }

    private static int getEconomizeCostPercent(ItemStack itemStack) {
        int economizeLevel = 0;
        for (Entry<Holder<Enchantment>> entry : itemStack.getTagEnchantments().entrySet()) {
            if (!entry.getKey().is(CCBEnchantments.ECONOMIZE)) {
                continue;
            }

            economizeLevel = entry.getIntValue();
            break;
        }

        return 100 - CCBMathUtils.clampNonNegative(economizeLevel, ECONOMIZE_MAX_LEVEL) * 20;
    }

    @Override
    public boolean isEmpty() {
        return getGasInTank(0).isEmpty();
    }

    @Override
    public boolean isFull() {
        return getGasInTank(0).getAmount() >= getTankCapacity(0);
    }

    @Override
    public boolean isGasValid(int tankIndex, GasStack ignoredGas) {
        return !isInvalidTank(tankIndex);
    }

    @Override
    public GasStack drain(int tankIndex, GasStack requestedGas, GasAction action) {
        if (isInvalidTank(tankIndex) || requestedGas.isEmpty() || !GasStack.isSameGasSameComponents(requestedGas, getGasInTank(tankIndex))) {
            return GasStack.EMPTY;
        }
        return drain(tankIndex, requestedGas.getAmount(), action);
    }

    @Override
    public GasStack drain(int tankIndex, long maxDrainAmount, GasAction action) {
        if (isInvalidTank(tankIndex) || maxDrainAmount <= 0) {
            return GasStack.EMPTY;
        }

        GasStack storedGas = getGasInTank(tankIndex);
        long drainAmount = Math.min(maxDrainAmount, storedGas.getAmount());
        GasStack drainedGas = storedGas.copyWithAmount(drainAmount);
        if (!action.execute() || drainAmount <= 0) {
            return drainedGas;
        }

        gas.shrink(drainAmount);
        saveContents();
        return drainedGas;
    }

    @Override
    public GasStack getGasInTank(int tankIndex) {
        if (isInvalidTank(tankIndex)) {
            return GasStack.EMPTY;
        }
        return gas.copy();
    }

    @Override
    public int getPriority() {
        if (isEmpty()) {
            return EMPTY_CANISTER;
        }
        return NON_EMPTY_CANISTER;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public ItemStack getContainer() {
        return canister;
    }

    @Override
    public @Unmodifiable List<ItemStack> getVirtualItems() {
        GasStack storedGas = getGasInTank(0);
        if (storedGas.isEmpty()) {
            return List.of(ItemStack.EMPTY);
        }
        return List.of(GasVirtualUtils.createVirtualItem(storedGas));
    }

    @Override
    public long fill(int tankIndex, GasStack incomingGas, GasAction action) {
        if (isInvalidTank(tankIndex) || incomingGas.isEmpty()) {
            return 0;
        }

        GasStack storedGas = getGasInTank(tankIndex);
        long tankCapacity = getTankCapacity(tankIndex);
        if (action.simulate()) {
            if (storedGas.isEmpty()) {
                return Math.min(tankCapacity, incomingGas.getAmount());
            }

            long remainingSpace = Math.max(0, tankCapacity - storedGas.getAmount());
            if (!GasStack.isSameGasSameComponents(storedGas, incomingGas)) {
                return 0;
            }
            return Math.min(remainingSpace, incomingGas.getAmount());
        }

        if (storedGas.isEmpty()) {
            return fillEmpty(incomingGas, tankCapacity);
        }

        if (!GasStack.isSameGasSameComponents(storedGas, incomingGas)) {
            return 0;
        }
        return fillExisting(storedGas, incomingGas, tankCapacity);
    }

    @Override
    public long getTankCapacity(int tankIndex) {
        if (isInvalidTank(tankIndex)) {
            return 0;
        }
        return Math.max(0, getEnchantedCapacity(canister));
    }

    @Override
    public void save() {
        saveContents();
    }

    @Override
    public HatchCanisterType getAirtightHatchType() {
        return HatchCanisterType.NORMAL;
    }

    @Override
    public GasStack getAirtightHatchContents() {
        return getGasInTank(0);
    }

    @Override
    public long getAirtightHatchCapacity(GasStack ignoredContents) {
        return getTankCapacity(0);
    }

    @Override
    public boolean setAirtightHatchContents(GasStack newContents) {
        gas = newContents.copy();
        return true;
    }

    private long fillEmpty(GasStack incomingGas, long tankCapacity) {
        long fillAmount = Math.min(tankCapacity, incomingGas.getAmount());
        if (fillAmount <= 0) {
            return 0;
        }

        gas = incomingGas.copyWithAmount(fillAmount);
        saveContents();
        return fillAmount;
    }

    private long fillExisting(GasStack storedGas, GasStack incomingGas, long tankCapacity) {
        long remainingSpace = Math.max(0, tankCapacity - storedGas.getAmount());
        long fillAmount = Math.min(remainingSpace, incomingGas.getAmount());
        gas.grow(fillAmount);
        if (fillAmount <= 0) {
            return fillAmount;
        }

        saveContents();
        return fillAmount;
    }

    private void saveContents() {
        canister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, gas.copy());
    }
}
