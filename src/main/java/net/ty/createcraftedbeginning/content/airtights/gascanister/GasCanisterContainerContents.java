package net.ty.createcraftedbeginning.content.airtights.gascanister;

import com.simibubi.create.AllEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.ty.createcraftedbeginning.api.canister.AirtightHatchCanister;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBEnchantments;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCanisterContainerContents implements AirtightHatchCanister {
    public static final int ECONOMIZE_MAX_LEVEL = 3;
    private static final int ECONOMIZE_MINIMUM_DRAIN_LEVEL = 5;
    private final ItemStack canister;

    protected GasStack gas;

    protected GasCanisterContainerContents(ItemStack canister) {
        this.canister = canister;
        GasStack storedGas = canister.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, GasStack.EMPTY).copy();
        gas = canContain(storedGas) ? storedGas : GasStack.EMPTY;
    }

    public static long getEconomizedDrainAmount(long logicalAmount, ItemStack itemStack) {
        if (logicalAmount <= 0) {
            return 0;
        }

        return Math.max(1, (logicalAmount * getEconomizeCostPercent(itemStack) + 99) / 100);
    }

    public static long getLogicalAmountFromEconomizedDrain(long physicalDrain, ItemStack itemStack) {
        if (physicalDrain <= 0) {
            return 0;
        }

        int costPercent = getEconomizeCostPercent(itemStack);
        if (costPercent == 0) {
            return Long.MAX_VALUE;
        }

        return physicalDrain * 100 / costPercent;
    }

    public static long getDefaultVolume() {
        return CCBConfig.server().equipment.gasCanister.gasVolume.get() * GasUnits.LITERS_PER_KILOLITER;
    }

    public static long getDefaultMaxPressurePa() {
        return GasPressure.pascals(CCBConfig.server().equipment.gasCanister.maxPressure.getF());
    }

    protected static boolean isInvalidTank(int tankIndex) {
        return tankIndex != 0;
    }

    private static long getEnchantedVolume(ItemStack itemStack) {
        long capacityLevel = 0;
        for (Entry<Holder<Enchantment>> entry : itemStack.getTagEnchantments().entrySet()) {
            if (!entry.getKey().is(AllEnchantments.CAPACITY)) {
                continue;
            }

            capacityLevel = entry.getIntValue();
            break;
        }

        return getDefaultVolume() * (1 + capacityLevel);
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

        return 100 - Mth.clamp(economizeLevel, 0, ECONOMIZE_MINIMUM_DRAIN_LEVEL) * 20;
    }

    @Override
    public boolean isEmpty() {
        return getGasInTank(0).isEmpty();
    }

    @Override
    public boolean isFull() {
        return getGasInTank(0).getAmount() >= getTankMaxAmount(0);
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
    public @Unmodifiable List<ItemStack> createVirtualItems() {
        GasStack storedGas = getGasInTank(0);
        if (storedGas.isEmpty()) {
            return List.of(ItemStack.EMPTY);
        }

        return List.of(VirtualGasItems.createVirtualItem(storedGas));
    }

    @Override
    public long fill(int tankIndex, GasStack incomingGas, GasAction action) {
        if (isInvalidTank(tankIndex) || incomingGas.isEmpty()) {
            return 0;
        }

        GasStack storedGas = getGasInTank(tankIndex);
        long maxAmount = getTankMaxAmount(tankIndex);
        if (action.simulate()) {
            if (storedGas.isEmpty()) {
                return Math.min(maxAmount, incomingGas.getAmount());
            }

            long remainingSpace = Math.max(0, maxAmount - storedGas.getAmount());
            if (!GasStack.isSameGasSameComponents(storedGas, incomingGas)) {
                return 0;
            }

            return Math.min(remainingSpace, incomingGas.getAmount());
        }

        if (storedGas.isEmpty()) {
            return fillEmpty(incomingGas, maxAmount);
        }

        if (!GasStack.isSameGasSameComponents(storedGas, incomingGas)) {
            return 0;
        }

        return fillExisting(storedGas, incomingGas, maxAmount);
    }

    @Override
    public boolean supportsExactDrainRecovery(int tankIndex) {
        return !isInvalidTank(tankIndex);
    }

    @Override
    public long getTankVolume(int tankIndex) {
        if (isInvalidTank(tankIndex)) {
            return 0;
        }

        return Math.max(0, getEnchantedVolume(canister));
    }

    @Override
    public long getTankMaxPressurePa(int tankIndex) {
        if (isInvalidTank(tankIndex)) {
            return 0;
        }

        return Math.max(0, getDefaultMaxPressurePa());
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
    public boolean setAirtightHatchContents(GasStack newContents) {
        if (!canContain(newContents)) {
            return false;
        }

        gas = newContents.copy();
        return true;
    }

    private boolean canContain(GasStack contents) {
        return contents.isEmpty() || isGasValid(0, contents) && contents.getAmount() <= getTankMaxAmount(0);
    }

    private long fillEmpty(GasStack incomingGas, long maxAmount) {
        long fillAmount = Math.min(maxAmount, incomingGas.getAmount());
        if (fillAmount <= 0) {
            return 0;
        }

        gas = incomingGas.copyWithAmount(fillAmount);
        saveContents();
        return fillAmount;
    }

    private long fillExisting(GasStack storedGas, GasStack incomingGas, long maxAmount) {
        long remainingSpace = Math.max(0, maxAmount - storedGas.getAmount());
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
