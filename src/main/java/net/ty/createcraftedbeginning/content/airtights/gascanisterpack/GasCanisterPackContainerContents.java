package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.createmod.catnip.data.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.GasStackLinkedSet;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
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
public class GasCanisterPackContainerContents implements IGasCanisterContainer {
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
        if (canister.isEmpty() || !(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents)) {
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
        return isEmpty() ? EMPTY_PACK : NON_EMPTY_PACK;
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
    public @Unmodifiable List<ItemStack> getVirtualItems() {
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

            virtualItems.add(GasVirtualUtils.createVirtualItem(gasType));
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
    public long getTankCapacity(int tank) {
        if (isInvalidTank(tank)) {
            return 0;
        }

        GasCanisterContainerContents canisterContents = getCanisterContents(tank);
        if (canisterContents == null) {
            return 0;
        }
        return canisterContents.getTankCapacity(0);
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

    public Pair<GasStack, Pair<Long, Boolean>> getFirstNonEmptyPair() {
        for (int tankIndex = 0; tankIndex < MAX_COUNT; tankIndex++) {
            GasStack gasContent = getGasInTank(tankIndex);
            if (gasContent.isEmpty()) {
                continue;
            }

            return Pair.of(gasContent, Pair.of(getTankCapacity(tankIndex), isCreative(tankIndex)));
        }
        return Pair.of(GasStack.EMPTY, Pair.of(0L, false));
    }

    void replaceCanisters(List<ItemStack> storedCanisters) {
        for (int tankIndex = 0; tankIndex < MAX_COUNT; tankIndex++) {
            ItemStack canister = tankIndex < storedCanisters.size() ? storedCanisters.get(tankIndex) : ItemStack.EMPTY;
            canisters.set(tankIndex, normalizeCanister(canister));
        }
        save();
    }

    private @Nullable GasCanisterContainerContents getCanisterContents(int tankIndex) {
        if (isInvalidTank(tankIndex)) {
            return null;
        }

        return canisters.get(tankIndex).getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents ? canisterContents : null;
    }

    private @Unmodifiable List<ItemStack> copyCanisters() {
        return canisters.stream().map(ItemStack::copy).toList();
    }
}
