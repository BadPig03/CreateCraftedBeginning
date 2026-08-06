package net.ty.createcraftedbeginning.content.airtights.gascanister;

import com.simibubi.create.AllEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBEnchantments;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCanisterContainerContents implements IGasCanisterContainer {
    public static final List<GasStack> DEFAULT_CONTENT = List.of(GasStack.EMPTY);
    public static final int ECONOMIZE_MAX_LEVEL = 3;
    protected final ItemStack canister;

    protected GasStack gas;

    public GasCanisterContainerContents(ItemStack canister) {
        this.canister = canister;
        gas = canister.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, DEFAULT_CONTENT).getFirst().copy();
    }

    public static long getDefaultCapacity() {
        return CCBConfig.server().airtights.maxCanisterCapacity.get() * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }

    public static long getEnchantedCapacity(ItemStack itemStack) {
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

    public static int getEconomizeCostPercent(ItemStack itemStack) {
        int economizeLevel = 0;
        for (Entry<Holder<Enchantment>> entry : itemStack.getTagEnchantments().entrySet()) {
            if (!entry.getKey().is(CCBEnchantments.ECONOMIZE)) {
                continue;
            }

            economizeLevel = entry.getIntValue();
            break;
        }

        return 100 - Mth.clamp(economizeLevel, 0, ECONOMIZE_MAX_LEVEL) * 20;
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

    protected static boolean isInvalidTank(int tank) {
        return tank != 0;
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
    public boolean isGasValid(int tank, GasStack stack) {
        return !isInvalidTank(tank);
    }

    @Override
    public GasStack drain(int tank, GasStack resource, GasAction action) {
        if (isInvalidTank(tank) || resource.isEmpty() || !GasStack.isSameGasSameComponents(resource, getGasInTank(tank))) {
            return GasStack.EMPTY;
        }
        return drain(tank, resource.getAmount(), action);
    }

    @Override
    public GasStack drain(int tank, long maxDrain, GasAction action) {
        if (isInvalidTank(tank)) {
            return GasStack.EMPTY;
        }

        GasStack storedGas = getGasInTank(tank);
        long drained = Math.min(maxDrain, storedGas.getAmount());
        GasStack drainedGas = storedGas.copyWithAmount(drained);
        if (!action.execute() || drained <= 0) {
            return drainedGas;
        }

        gas.shrink(drained);
        saveContents();
        return drainedGas;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return isInvalidTank(tank) ? GasStack.EMPTY : gas.copy();
    }

    @Override
    public int getPriority() {
        return isEmpty() ? EMPTY_CANISTER : NON_EMPTY_CANISTER;
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
        GasStack content = getGasInTank(0);
        if (content.isEmpty()) {
            return List.of(ItemStack.EMPTY);
        }
        return List.of(GasVirtualUtils.createVirtualItem(content));
    }

    @Override
    public long fill(int tank, GasStack resource, GasAction action) {
        if (isInvalidTank(tank) || resource.isEmpty()) {
            return 0;
        }

        GasStack storedGas = getGasInTank(tank);
        long tankCapacity = getTankCapacity(tank);
        if (action.simulate()) {
            if (storedGas.isEmpty()) {
                return Math.min(tankCapacity, resource.getAmount());
            }

            long remainingSpace = Math.max(0, tankCapacity - storedGas.getAmount());
            if (!GasStack.isSameGasSameComponents(storedGas, resource)) {
                return 0;
            }
            return Math.min(remainingSpace, resource.getAmount());
        }

        if (storedGas.isEmpty()) {
            return fillEmpty(resource, tankCapacity);
        }

        if (!GasStack.isSameGasSameComponents(storedGas, resource)) {
            return 0;
        }
        return fillExisting(storedGas, resource, tankCapacity);
    }

    @Override
    public long getTankCapacity(int tank) {
        return isInvalidTank(tank) ? 0 : Math.max(0, getEnchantedCapacity(canister));
    }

    @Override
    public void save() {
        saveContents();
    }

    @Override
    public void setCapacity(int tank, long capacity) {
    }

    private long fillEmpty(GasStack resource, long tankCapacity) {
        long amount = Math.min(tankCapacity, resource.getAmount());
        if (amount <= 0) {
            return 0;
        }

        gas = resource.copyWithAmount(amount);
        saveContents();
        return amount;
    }

    private long fillExisting(GasStack storedGas, GasStack resource, long tankCapacity) {
        long remainingSpace = Math.max(0, tankCapacity - storedGas.getAmount());
        long amount = Math.min(remainingSpace, resource.getAmount());
        gas.grow(amount);
        if (amount <= 0) {
            return amount;
        }

        saveContents();
        return amount;
    }

    public void saveContents() {
        canister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, List.of(gas.copy()));
    }
}
