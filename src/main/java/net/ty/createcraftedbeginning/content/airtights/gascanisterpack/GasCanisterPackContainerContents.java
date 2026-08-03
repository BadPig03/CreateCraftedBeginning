package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.createmod.catnip.data.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.GasStackLinkedSet;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class GasCanisterPackContainerContents implements IGasCanisterContainer {
    public static final int MAX_COUNT = 4;

    protected final ItemStack pack;
    protected final List<GasStack> gases;
    protected final List<Long> capacities;
    protected final List<CompoundTag> compoundTags;
    protected final List<Boolean> creatives;

    public GasCanisterPackContainerContents(ItemStack pack) {
        this.pack = pack;
        gases = new ArrayList<>(pack.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new ArrayList<>(List.of(GasStack.EMPTY, GasStack.EMPTY, GasStack.EMPTY, GasStack.EMPTY))));
        capacities = new ArrayList<>(pack.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES, new ArrayList<>(List.of(0L, 0L, 0L, 0L))));
        compoundTags = new ArrayList<>(pack.getOrDefault(CCBDataComponents.CANISTER_PACK_CONTAINER_COMPOUNDS, new ArrayList<>(List.of(new CompoundTag(), new CompoundTag(), new CompoundTag(), new CompoundTag()))));
        creatives = new ArrayList<>(pack.getOrDefault(CCBDataComponents.CANISTER_PACK_CONTAINER_CREATIVES, new ArrayList<>(List.of(false, false, false, false))));
    }

    private static boolean isInvalidTank(int tank) {
        return tank < 0 || tank >= MAX_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return IntStream.range(0, MAX_COUNT).allMatch(tank -> getGasInTank(tank).isEmpty());
    }

    @Override
    public boolean isFull() {
        return IntStream.range(0, MAX_COUNT).noneMatch(tank -> getGasInTank(tank).getAmount() < getTankCapacity(tank));
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return true;
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

        GasStack gas = getGasInTank(tank);
        if (gas.isEmpty()) {
            return GasStack.EMPTY;
        }

        if (creatives.get(tank)) {
            return gas.copyWithAmount(maxDrain);
        }

        long drained = Math.min(maxDrain, gas.getAmount());
        GasStack drainedGas = gas.copyWithAmount(drained);
        if (!action.execute() || drained <= 0) {
            return drainedGas;
        }

        gases.get(tank).shrink(drained);
        save();
        return drainedGas;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        if (isInvalidTank(tank)) {
            return GasStack.EMPTY;
        }

        GasStack gas = gases.get(tank);
        long capacity = capacities.get(tank);
        gas.setAmount(Mth.clamp(gas.getAmount(), 0, capacity));
        return gas;
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
    public @Unmodifiable List<ItemStack> getVirtualItems() {
        if (isEmpty()) {
            return List.of(ItemStack.EMPTY);
        }

        Set<GasStack> uniqueGases = GasStackLinkedSet.createTypeAndComponentsSet();
        List<ItemStack> items = new ArrayList<>();
        for (int tank = 0; tank < MAX_COUNT; tank++) {
            GasStack gas = getGasInTank(tank).copyWithAmount(1);
            if (gas.isEmpty() || !uniqueGases.add(gas)) {
                continue;
            }

            items.add(GasVirtualUtils.createVirtualItem(gas));
        }

        return items.stream().toList();
    }

    @Override
    public long fill(int tank, GasStack resource, GasAction action) {
        if (resource.isEmpty() || isInvalidTank(tank)) {
            return 0;
        }

        GasStack gas = getGasInTank(tank);
        long capacity = getTankCapacity(tank);
        if (action.simulate()) {
            if (gas.isEmpty()) {
                return Math.min(capacity, resource.getAmount());
            }
            return GasStack.isSameGasSameComponents(gas, resource) ? Math.min(capacity - gas.getAmount(), resource.getAmount()) : 0;
        }

        if (gas.isEmpty()) {
            long filled = Math.min(capacity, resource.getAmount());
            gases.set(tank, resource.copyWithAmount(filled));
            save();
            return filled;
        }

        if (!GasStack.isSameGasSameComponents(gas, resource)) {
            return 0;
        }

        long space = capacity - gas.getAmount();
        long filled = Math.min(space, resource.getAmount());
        gases.get(tank).grow(filled);
        if (filled <= 0) {
            return filled;
        }

        save();
        return filled;
    }

    @Override
    public long getTankCapacity(int tank) {
        if (isInvalidTank(tank)) {
            return 0;
        }
        return capacities.get(tank);
    }

    @Override
    public void save() {
        saveContents();
        saveCapacities();
        saveCompounds();
        saveCreatives();
    }

    @Override
    public void setCapacity(int tank, long capacity) {
        capacities.set(tank, capacity);
        saveCapacities();
    }

    public boolean isEmpty(int tank) {
        return getGasInTank(tank).isEmpty();
    }

    public boolean isFull(int tank) {
        return getGasInTank(tank).getAmount() >= getTankCapacity(tank);
    }

    public CompoundTag getCompoundTag(int tank) {
        if (isInvalidTank(tank)) {
            return new CompoundTag();
        }
        return compoundTags.get(tank);
    }

    public void setCompoundTag(int tank, CompoundTag compoundTag) {
        if (isInvalidTank(tank)) {
            return;
        }

        compoundTags.set(tank, compoundTag);
        saveCompounds();
    }

    public boolean getCreatives(int tank) {
        return !isInvalidTank(tank) && creatives.get(tank);
    }

    public void setCreatives(int tank, boolean creative) {
        if (isInvalidTank(tank)) {
            return;
        }

        creatives.set(tank, creative);
        saveCreatives();
    }

    public void saveContents() {
        pack.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, gases);
    }

    public void saveCapacities() {
        pack.set(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES, capacities);
    }

    public void saveCompounds() {
        pack.set(CCBDataComponents.CANISTER_PACK_CONTAINER_COMPOUNDS, compoundTags);
    }

    public void saveCreatives() {
        pack.set(CCBDataComponents.CANISTER_PACK_CONTAINER_CREATIVES, creatives);
    }

    public Pair<GasStack, Pair<Long, Boolean>> getFirstNonEmptyPair() {
        for (int tank = 0; tank < MAX_COUNT; tank++) {
            GasStack gas = getGasInTank(tank);
            if (gas.isEmpty()) {
                continue;
            }

            return Pair.of(gas, Pair.of(getTankCapacity(tank), getCreatives(tank)));
        }
        return Pair.of(GasStack.EMPTY, Pair.of(0L, false));
    }
}
