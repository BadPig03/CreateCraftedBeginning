package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.createmod.catnip.data.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCanisterPackContainerContents implements IGasCanisterContainer {
    public static final int MAX_COUNT = 4;

    protected final ItemStack pack;
    protected final List<GasStack> gases;
    protected final List<Long> capacities;
    protected final List<CompoundTag> compoundTags;
    protected final List<Boolean> creatives;

    public GasCanisterPackContainerContents(ItemStack pack) {
        this.pack = pack;
        capacities = normalizeCapacities(pack.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES, List.of()));
        gases = normalizeGases(pack.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, List.of()), capacities);
        compoundTags = normalizeCompounds(pack.getOrDefault(CCBDataComponents.CANISTER_PACK_CONTAINER_COMPOUNDS, List.of()));
        creatives = normalizeCreatives(pack.getOrDefault(CCBDataComponents.CANISTER_PACK_CONTAINER_CREATIVES, List.of()));
    }

    private static boolean isInvalidTank(int tank) {
        return tank < 0 || tank >= MAX_COUNT;
    }

    private static GasStack normalizeGas(GasStack gas, long capacity) {
        if (gas.isEmpty() || capacity <= 0) {
            return GasStack.EMPTY;
        }
        return gas.copyWithAmount(Math.min(capacity, gas.getAmount()));
    }

    private static List<GasStack> normalizeGases(List<GasStack> storedGases, List<Long> capacities) {
        List<GasStack> normalized = new ArrayList<>(MAX_COUNT);
        for (int tank = 0; tank < MAX_COUNT; tank++) {
            GasStack gas = tank < storedGases.size() ? storedGases.get(tank) : GasStack.EMPTY;
            normalized.add(normalizeGas(gas, capacities.get(tank)));
        }
        return normalized;
    }

    private static List<Long> normalizeCapacities(List<Long> storedCapacities) {
        List<Long> normalized = new ArrayList<>(MAX_COUNT);
        for (int tank = 0; tank < MAX_COUNT; tank++) {
            long capacity = tank < storedCapacities.size() ? storedCapacities.get(tank) : 0L;
            normalized.add(Math.max(0, capacity));
        }
        return normalized;
    }

    private static List<CompoundTag> normalizeCompounds(List<CompoundTag> storedCompounds) {
        List<CompoundTag> normalized = new ArrayList<>(MAX_COUNT);
        for (int tank = 0; tank < MAX_COUNT; tank++) {
            CompoundTag compoundTag = tank < storedCompounds.size() ? storedCompounds.get(tank) : null;
            normalized.add(compoundTag == null ? new CompoundTag() : compoundTag.copy());
        }
        return normalized;
    }

    private static List<Boolean> normalizeCreatives(List<Boolean> storedCreatives) {
        List<Boolean> normalized = new ArrayList<>(MAX_COUNT);
        for (int tank = 0; tank < MAX_COUNT; tank++) {
            Boolean creative = tank < storedCreatives.size() ? storedCreatives.get(tank) : false;
            normalized.add(creative);
        }
        return normalized;
    }

    private static List<GasStack> copyGases(List<GasStack> gases) {
        return gases.stream().map(GasStack::copy).toList();
    }

    private static List<CompoundTag> copyCompounds(List<CompoundTag> compoundTags) {
        return compoundTags.stream().map(CompoundTag::copy).toList();
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

        gases.set(tank, gas.copyWithAmount(gas.getAmount() - drained));
        save();
        return drainedGas;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        if (isInvalidTank(tank)) {
            return GasStack.EMPTY;
        }
        return normalizeGas(gases.get(tank), capacities.get(tank));
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
        if (resource.isEmpty() || isInvalidTank(tank) || creatives.get(tank)) {
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
        if (filled <= 0) {
            return filled;
        }

        gases.set(tank, gas.copyWithAmount(gas.getAmount() + filled));
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
        if (isInvalidTank(tank)) {
            return;
        }

        long storedCapacity = Math.max(0, capacity);
        GasStack storedGas = normalizeGas(gases.get(tank), storedCapacity);
        boolean capacityChanged = capacities.get(tank) != storedCapacity;
        boolean gasChanged = !gases.get(tank).equals(storedGas);
        if (!capacityChanged && !gasChanged) {
            return;
        }

        capacities.set(tank, storedCapacity);
        gases.set(tank, storedGas);
        if (gasChanged) {
            saveContents();
        }
        if (!capacityChanged) {
            return;
        }

        saveCapacities();
    }

    public boolean isEmpty(int tank) {
        return getGasInTank(tank).isEmpty();
    }

    public CompoundTag getCompoundTag(int tank) {
        if (isInvalidTank(tank)) {
            return new CompoundTag();
        }
        return compoundTags.get(tank).copy();
    }

    public boolean getCreatives(int tank) {
        return !isInvalidTank(tank) && creatives.get(tank);
    }

    public void replaceCanisters(List<CanisterData> canisters) {
        for (int tank = 0; tank < MAX_COUNT; tank++) {
            CanisterData canister = tank < canisters.size() && canisters.get(tank) != null ? canisters.get(tank) : CanisterData.EMPTY;
            setCanister(tank, canister);
        }
        save();
    }

    protected void setCanister(int tank, CanisterData canister) {
        capacities.set(tank, canister.capacity());
        gases.set(tank, canister.gas().copy());
        compoundTags.set(tank, canister.compoundTag().copy());
        creatives.set(tank, canister.creative());
    }

    public void saveContents() {
        setComponentIfChanged(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, copyGases(gases));
    }

    public void saveCapacities() {
        setComponentIfChanged(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES, List.copyOf(capacities));
    }

    public void saveCompounds() {
        setComponentIfChanged(CCBDataComponents.CANISTER_PACK_CONTAINER_COMPOUNDS, copyCompounds(compoundTags));
    }

    public void saveCreatives() {
        setComponentIfChanged(CCBDataComponents.CANISTER_PACK_CONTAINER_CREATIVES, List.copyOf(creatives));
    }

    protected <T> void setComponentIfChanged(DataComponentType<T> component, T value) {
        if (Objects.equals(pack.get(component), value)) {
            return;
        }

        pack.set(component, value);
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

    public record CanisterData(GasStack gas, long capacity, CompoundTag compoundTag, boolean creative) {
        private static final CanisterData EMPTY = new CanisterData(GasStack.EMPTY, 0, new CompoundTag(), false);

        public CanisterData {
            capacity = Math.max(0, capacity);
            gas = normalizeGas(gas, capacity);
            compoundTag = compoundTag.copy();
        }
    }
}
