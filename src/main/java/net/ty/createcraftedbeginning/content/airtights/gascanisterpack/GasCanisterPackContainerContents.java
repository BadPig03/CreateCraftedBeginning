package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.recipe.gas.ingredient.GasStackLinkedSet;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCanisterPackContainerContents implements GasCanisterContainer {
    public static final int MAX_COUNT = 4;

    private final ItemStack pack;
    private final List<ItemStack> canisters;

    GasCanisterPackContainerContents(ItemStack pack) {
        this.pack = pack;
        canisters = normalizeCanisters(pack.getOrDefault(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.EMPTY));
    }

    private static boolean isInvalidTank(int tankIndex) {
        return tankIndex < 0 || tankIndex >= MAX_COUNT;
    }

    private static List<ItemStack> normalizeCanisters(ItemContainerContents storedContents) {
        List<ItemStack> normalizedCanisters = new ArrayList<>(MAX_COUNT);
        for (int tankIndex = 0; tankIndex < MAX_COUNT; tankIndex++) {
            ItemStack canister = tankIndex < storedContents.getSlots() ? storedContents.getStackInSlot(tankIndex) : ItemStack.EMPTY;
            normalizedCanisters.add(normalizeCanister(canister));
        }
        return normalizedCanisters;
    }

    private static ItemStack normalizeCanister(ItemStack canister) {
        if (canister.isEmpty() || !(canister.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents)) {
            return ItemStack.EMPTY;
        }

        return canister.copyWithCount(1);
    }

    @Override
    public boolean isEmpty() {
        return IntStream.range(0, MAX_COUNT).allMatch(this::isEmpty);
    }

    @Override
    public boolean isFull() {
        boolean hasCanister = false;
        for (int tankIndex = 0; tankIndex < MAX_COUNT; tankIndex++) {
            GasCanisterContainerContents canisterContents = getCanisterContents(tankIndex);
            if (canisterContents == null) {
                continue;
            }

            hasCanister = true;
            if (!canisterContents.isFull()) {
                return false;
            }
        }
        return hasCanister;
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        GasCanisterContainerContents canisterContents = getCanisterContents(tank);
        return canisterContents != null && canisterContents.isGasValid(0, stack);
    }

    @Override
    public GasStack drain(int tank, GasStack resource, GasAction action) {
        if (isInvalidTank(tank) || resource.isEmpty() || !GasStack.isSameGasSameComponents(resource, getGasInTank(tank))) {
            return GasStack.EMPTY;
        }

        return drain(tank, resource.getAmount(), action);
    }

    @Override
    public GasStack drain(int tank, long maxDrainAmount, GasAction action) {
        if (isInvalidTank(tank) || maxDrainAmount <= 0) {
            return GasStack.EMPTY;
        }

        GasCanisterContainerContents canisterContents = getCanisterContents(tank);
        if (canisterContents == null) {
            return GasStack.EMPTY;
        }

        GasStack drainedGas = canisterContents.drain(0, maxDrainAmount, action);
        if (action.execute() && !drainedGas.isEmpty() && !(canisterContents instanceof CreativeGasCanisterContainerContents)) {
            save();
        }
        return drainedGas;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        if (isInvalidTank(tank)) {
            return GasStack.EMPTY;
        }

        GasCanisterContainerContents canisterContents = getCanisterContents(tank);
        if (canisterContents == null) {
            return GasStack.EMPTY;
        }

        return canisterContents.getGasInTank(0);
    }

    @Override
    public int getPriority() {
        if (isEmpty()) {
            return EMPTY_PACK;
        }

        return NON_EMPTY_PACK;
    }

    @Override
    public int getTanks() {
        return MAX_COUNT;
    }

    @Override
    public ItemStack getContainer() {
        return pack;
    }

    @Override
    public @Unmodifiable List<ItemStack> createVirtualItems() {
        if (isEmpty()) {
            return List.of(ItemStack.EMPTY);
        }

        Set<GasStack> uniqueGases = GasStackLinkedSet.createTypeAndComponentsSet();
        List<ItemStack> virtualItems = new ArrayList<>();
        for (int tankIndex = 0; tankIndex < MAX_COUNT; tankIndex++) {
            GasStack gasType = getGasInTank(tankIndex).copyWithAmount(1);
            if (gasType.isEmpty() || !uniqueGases.add(gasType)) {
                continue;
            }

            virtualItems.add(VirtualGasItems.createVirtualItem(gasType));
        }
        return List.copyOf(virtualItems);
    }

    @Override
    public InjectionMode getInjectionMode() {
        return InjectionMode.DENY;
    }

    @Override
    public long fill(int tank, GasStack resource, GasAction action) {
        if (resource.isEmpty() || isInvalidTank(tank)) {
            return 0;
        }

        GasCanisterContainerContents canisterContents = getCanisterContents(tank);
        if (canisterContents == null) {
            return 0;
        }

        long filledAmount = canisterContents.fill(0, resource, action);
        if (action.execute() && filledAmount > 0) {
            save();
        }
        return filledAmount;
    }

    @Override
    public boolean supportsExactDrainRecovery(int tank) {
        GasCanisterContainerContents canisterContents = getCanisterContents(tank);
        return canisterContents != null && canisterContents.supportsExactDrainRecovery(0);
    }

    @Override
    public long restoreDrainedGas(int tank, GasStack resource, GasAction action) {
        GasCanisterContainerContents canisterContents = getCanisterContents(tank);
        if (canisterContents == null || resource.isEmpty()) {
            return 0;
        }

        long restoredAmount = canisterContents.restoreDrainedGas(0, resource, action);
        if (action.execute() && restoredAmount > 0 && !(canisterContents instanceof CreativeGasCanisterContainerContents)) {
            save();
        }
        return restoredAmount;
    }

    @Override
    public long getTankVolume(int tank) {
        if (isInvalidTank(tank)) {
            return 0;
        }

        GasCanisterContainerContents canisterContents = getCanisterContents(tank);
        if (canisterContents == null) {
            return 0;
        }

        return canisterContents.getTankVolume(0);
    }

    @Override
    public long getTankMaxPressurePa(int tank) {
        if (isInvalidTank(tank)) {
            return 0;
        }

        GasCanisterContainerContents canisterContents = getCanisterContents(tank);
        if (canisterContents == null) {
            return 0;
        }

        return canisterContents.getTankMaxPressurePa(0);
    }

    @Override
    public PressureModel getTankPressureModel(int tank) {
        if (isInvalidTank(tank)) {
            return PressureModel.VARIABLE;
        }

        GasCanisterContainerContents canisterContents = getCanisterContents(tank);
        if (canisterContents == null) {
            return PressureModel.VARIABLE;
        }

        return canisterContents.getTankPressureModel(0);
    }

    @Override
    public void save() {
        pack.set(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.fromItems(copyCanisters()));
    }

    public boolean isEmpty(int tank) {
        return getGasInTank(tank).isEmpty();
    }

    public boolean isCreative(int tank) {
        return !isInvalidTank(tank) && getCanisterContents(tank) instanceof CreativeGasCanisterContainerContents;
    }

    public ItemStack getCanister(int tank) {
        if (isInvalidTank(tank)) {
            return ItemStack.EMPTY;
        }

        return canisters.get(tank).copy();
    }

    void replaceCanisters(List<ItemStack> storedCanisters) {
        for (int tankIndex = 0; tankIndex < MAX_COUNT; tankIndex++) {
            ItemStack canister = tankIndex < storedCanisters.size() ? storedCanisters.get(tankIndex) : ItemStack.EMPTY;
            canisters.set(tankIndex, normalizeCanister(canister));
        }
        save();
    }

    private @Nullable GasCanisterContainerContents getCanisterContents(int tankIndex) {
        if (isInvalidTank(tankIndex) || !(canisters.get(tankIndex).getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return null;
        }

        return canisterContents;
    }

    private @Unmodifiable List<ItemStack> copyCanisters() {
        return canisters.stream().map(ItemStack::copy).toList();
    }
}
