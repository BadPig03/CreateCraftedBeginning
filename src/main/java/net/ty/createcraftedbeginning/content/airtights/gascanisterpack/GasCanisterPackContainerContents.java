package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.createmod.catnip.data.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.cansiters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualItem;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class GasCanisterPackContainerContents implements IGasCanisterContainer {
    public static final int MAX_COUNT = 4;

    protected final ItemStack pack;
    protected final List<GasStack> gases;
    protected final List<Long> capacities;
    protected final List<CompoundTag> compoundTags;
    protected final List<Boolean> creatives;

    public GasCanisterPackContainerContents(@NotNull ItemStack pack) {
        this.pack = pack;
        gases = new ArrayList<>(pack.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new ArrayList<>(List.of(GasStack.EMPTY, GasStack.EMPTY, GasStack.EMPTY, GasStack.EMPTY))));
        capacities = new ArrayList<>(pack.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES, new ArrayList<>(List.of(0L, 0L, 0L, 0L))));
        compoundTags = new ArrayList<>(pack.getOrDefault(CCBDataComponents.CANISTER_PACK_CONTAINER_COMPOUNDS, new ArrayList<>(List.of(new CompoundTag(), new CompoundTag(), new CompoundTag(), new CompoundTag()))));
        creatives = new ArrayList<>(pack.getOrDefault(CCBDataComponents.CANISTER_PACK_CONTAINER_CREATIVES, new ArrayList<>(List.of(false, false, false, false))));
    }

    private static boolean validateSlot(int tank) {
        return tank < 0 || tank > MAX_COUNT;
    }

    @Override
    public int getTanks() {
        return MAX_COUNT;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        if (validateSlot(tank)) {
            return GasStack.EMPTY;
        }

        GasStack gas = gases.get(tank);
        long capacity = capacities.get(tank);
        gas.setAmount(Mth.clamp(gas.getAmount(), 0, capacity));
        return gas;
    }

    @Override
    public long getTankCapacity(int tank) {
        if (validateSlot(tank)) {
            return 0;
        }

        return capacities.get(tank);
    }

    @Override
    public void setCapacity(int tank, long capacity) {
        capacities.set(tank, capacity);
        saveCapacities();
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return true;
    }

    @Override
    public long fill(int tank, @NotNull GasStack resource, GasAction action) {
        if (resource.isEmpty() || validateSlot(tank)) {
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
            long amountToAdd = Math.min(capacity, resource.getAmount());
            gases.set(tank, resource.copyWithAmount(amountToAdd));
            save();
            return amountToAdd;
        }

        if (!GasStack.isSameGasSameComponents(gas, resource)) {
            return 0;
        }

        long remainingSpace = capacity - gas.getAmount();
        long amountToTransfer = Math.min(remainingSpace, resource.getAmount());
        gases.get(tank).grow(amountToTransfer);
        if (amountToTransfer > 0) {
            save();
        }
        return amountToTransfer;
    }

    @Override
    public GasStack drain(int tank, @NotNull GasStack resource, GasAction action) {
        if (validateSlot(tank) || resource.isEmpty() || !GasStack.isSameGasSameComponents(resource, getGasInTank(tank))) {
            return GasStack.EMPTY;
        }

        return drain(tank, resource.getAmount(), action);
    }

    @Override
    public GasStack drain(int tank, long maxDrain, @NotNull GasAction action) {
        if (validateSlot(tank)) {
            return GasStack.EMPTY;
        }

        GasStack gas = getGasInTank(tank);
        long drained = Math.min(maxDrain, gas.getAmount());
        GasStack copied = gas.copyWithAmount(drained);
        if (action.execute() && drained > 0) {
            gases.get(tank).shrink(drained);
            save();
        }
        return copied;
    }

    @Override
    public ItemStack getContainer() {
        return pack;
    }

    @Override
    public @NotNull @Unmodifiable List<ItemStack> getVirtualItems() {
        if (isEmpty()) {
            return List.of(ItemStack.EMPTY);
        }

        Set<Gas> existingGasTypes = new HashSet<>();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < MAX_COUNT; i++) {
            GasStack gasContent = getGasInTank(i);
            if (gasContent.isEmpty()) {
                continue;
            }

            Gas gasType = gasContent.getGasType();
            if (existingGasTypes.contains(gasType)) {
                continue;
            }

            existingGasTypes.add(gasType);
            items.add(GasVirtualItem.getVirtualItem(gasContent));
        }

        return items.stream().toList();
    }

    @Override
    public void save() {
        saveContents();
        saveCapacities();
        saveCompounds();
        saveCreatives();
    }

    @Override
    public boolean isEmpty() {
        return IntStream.range(0, MAX_COUNT).allMatch(i -> getGasInTank(i).isEmpty());
    }

    @Override
    public boolean isFull() {
        return IntStream.rangeClosed(0, MAX_COUNT).noneMatch(i -> getGasInTank(i).getAmount() < getTankCapacity(i));
    }

    @Override
    public int getPriority() {
        if (isEmpty()) {
            return EMPTY_PACK;
        }

        return NON_EMPTY_PACK;
    }

    public boolean isEmpty(int tank) {
        return getGasInTank(tank).isEmpty();
    }

    public boolean isFull(int tank) {
        return getGasInTank(tank).getAmount() >= getTankCapacity(tank);
    }

    public CompoundTag getCompoundTag(int tank) {
        if (validateSlot(tank)) {
            return new CompoundTag();
        }

        return compoundTags.get(tank);
    }

    public void setCompoundTag(int tank, CompoundTag compoundTag) {
        if (validateSlot(tank)) {
            return;
        }

        compoundTags.set(tank, compoundTag);
        saveCompounds();
    }

    public boolean getCreatives(int tank) {
        return !validateSlot(tank) && creatives.get(tank);
    }

    public void setCreatives(int tank, boolean creative) {
        if (validateSlot(tank)) {
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

    public @NotNull Pair<GasStack, Long> getFirstNonEmptyPair() {
        for (int i = 0; i < MAX_COUNT; i++) {
            GasStack gasContent = getGasInTank(i);
            if (gasContent.isEmpty()) {
                continue;
            }

            return Pair.of(gasContent, getTankCapacity(i));
        }
        return Pair.of(GasStack.EMPTY, 0L);
    }
}
