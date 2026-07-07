package net.ty.createcraftedbeginning.content.airtights.gascanister;

import com.simibubi.create.AllEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
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
    public static final List<Long> DEFAULT_CAPACITY = List.of(getDefaultCapacity());
    public static final int ECONOMIZE_MAX_LEVEL = 3;
    protected final ItemStack canister;

    protected GasStack gas;
    protected long capacity;

    public GasCanisterContainerContents(ItemStack canister) {
        this.canister = canister;
        gas = canister.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, DEFAULT_CONTENT).getFirst();
        capacity = canister.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES, DEFAULT_CAPACITY).getFirst();
    }

    public static long getDefaultCapacity() {
        return CCBConfig.server().airtights.maxCanisterCapacity.get() * 1000L;
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
        return true;
    }

    @Override
    public GasStack drain(int tank, GasStack resource, GasAction action) {
        return resource.isEmpty() || !GasStack.isSameGasSameComponents(resource, getGasInTank(0)) ? GasStack.EMPTY : drain(0, resource.getAmount(), action);
    }

    @Override
    public GasStack drain(int tank, long maxDrain, GasAction action) {
        GasStack gas = getGasInTank(0);
        long drained = Math.min(maxDrain, gas.getAmount());
        GasStack copied = gas.copyWithAmount(drained);
        if (action.execute() && drained > 0) {
            this.gas.shrink(drained);
            save();
        }
        return copied;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        long amount = Mth.clamp(gas.getAmount(), 0, getEnchantedCapacity(canister));
        gas.setAmount(amount);
        return gas;
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
        GasStack gasContent = getGasInTank(0);
        if (gasContent.isEmpty()) {
            return List.of(ItemStack.EMPTY);
        }

        return List.of(GasVirtualUtils.createVirtualItem(gasContent));
    }

    @Override
    public long fill(int tank, GasStack resource, GasAction action) {
        if (resource.isEmpty()) {
            return 0;
        }

        GasStack gas = getGasInTank(0);
        long capacity = getTankCapacity(0);
        if (action.simulate()) {
            if (gas.isEmpty()) {
                return Math.min(capacity, resource.getAmount());
            }

            return GasStack.isSameGasSameComponents(gas, resource) ? Math.min(capacity - gas.getAmount(), resource.getAmount()) : 0;
        }

        if (gas.isEmpty()) {
            long amountToAdd = Math.min(capacity, resource.getAmount());
            this.gas = resource.copyWithAmount(amountToAdd);
            saveContents();
            return amountToAdd;
        }

        if (!GasStack.isSameGasSameComponents(gas, resource)) {
            return 0;
        }

        long remainingSpace = capacity - gas.getAmount();
        long amountToTransfer = Math.min(remainingSpace, resource.getAmount());
        this.gas.grow(amountToTransfer);
        if (amountToTransfer > 0) {
            saveContents();
        }
        return amountToTransfer;
    }

    @Override
    public long getTankCapacity(int tank) {
        long newCapacity = getEnchantedCapacity(canister);
        if (newCapacity != capacity) {
            capacity = newCapacity;
        }
        return capacity;
    }

    @Override
    public void save() {
        saveContents();
        saveCapacities();
    }

    @Override
    public void setCapacity(int tank, long capacity) {
        this.capacity = capacity;
        saveCapacities();
    }

    public void saveContents() {
        canister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, List.of(gas));
    }

    public void saveCapacities() {
        canister.set(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES, List.of(capacity));
    }
}
