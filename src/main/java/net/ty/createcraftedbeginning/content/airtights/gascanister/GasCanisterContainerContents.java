package net.ty.createcraftedbeginning.content.airtights.gascanister;

import com.simibubi.create.AllEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.ty.createcraftedbeginning.api.gas.cansiters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualItem;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public class GasCanisterContainerContents implements IGasCanisterContainer {
    public static final List<GasStack> DEFAULT_CONTENT = List.of(GasStack.EMPTY);
    public static final List<Long> DEFAULT_CAPACITY = List.of(getDefaultCapacity());
    protected final ItemStack canister;

    protected GasStack gas;
    protected long capacity;

    public GasCanisterContainerContents(@NotNull ItemStack canister) {
        this.canister = canister;
        gas = canister.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, DEFAULT_CONTENT).getFirst();
        capacity = canister.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES, DEFAULT_CAPACITY).getFirst();
    }

    public static long getDefaultCapacity() {
        return CCBConfig.server().airtights.maxCanisterCapacity.get() * 1000L;
    }

    public static long getEnchantedCapacity(@NotNull ItemStack itemStack) {
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

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        long amount = Mth.clamp(gas.getAmount(), 0, getEnchantedCapacity(canister));
        gas.setAmount(amount);
        return gas;
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
    public void setCapacity(int tank, long capacity) {
        this.capacity = capacity;
        saveCapacities();
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return true;
    }

    @Override
    public long fill(int tank, @NotNull GasStack resource, GasAction action) {
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
    public GasStack drain(int tank, @NotNull GasStack resource, GasAction action) {
        return resource.isEmpty() || !GasStack.isSameGasSameComponents(resource, getGasInTank(0)) ? GasStack.EMPTY : drain(0, resource.getAmount(), action);
    }

    @Override
    public GasStack drain(int tank, long maxDrain, @NotNull GasAction action) {
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
    public ItemStack getContainer() {
        return canister;
    }

    @Override
    public @NotNull @Unmodifiable List<ItemStack> getVirtualItems() {
        GasStack gasContent = getGasInTank(0);
        if (gasContent.isEmpty()) {
            return List.of(ItemStack.EMPTY);
        }

        return List.of(GasVirtualItem.getVirtualItem(gasContent));
    }

    @Override
    public void save() {
        saveContents();
        saveCapacities();
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
    public int getPriority() {
        if (isEmpty()) {
            return EMPTY_CANISTER;
        }
        return NON_EMPTY_CANISTER;
    }

    public void saveContents() {
        canister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, List.of(gas));
    }

    public void saveCapacities() {
        canister.set(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES, List.of(capacity));
    }
}
