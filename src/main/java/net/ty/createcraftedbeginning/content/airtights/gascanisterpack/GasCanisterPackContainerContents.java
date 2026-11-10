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
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
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
    private final GasCanisterContainerContents[] canisters = new GasCanisterContainerContents[MAX_COUNT];

    GasCanisterPackContainerContents(ItemStack pack) {
        this.pack = pack;
        ItemContainerContents storedContents = pack.getOrDefault(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.EMPTY);
        for (int tankIndex = 0; tankIndex < Math.min(MAX_COUNT, storedContents.getSlots()); tankIndex++) {
            canisters[tankIndex] = normalizeCanister(storedContents.getStackInSlot(tankIndex));
        }
    }

    private static boolean isInvalidTank(int tankIndex) {
        return tankIndex < 0 || tankIndex >= MAX_COUNT;
    }

    private static @Nullable GasCanisterContainerContents normalizeCanister(ItemStack canister) {
        ItemStack installedCanister = canister.copyWithCount(1);
        if (installedCanister.isEmpty() || !(installedCanister.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents contents)) {
            return null;
        }

        return contents;
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
        GasCanisterContainerContents contents = getCanisterContents(tank);
        if (contents == null) {
            return ItemStack.EMPTY;
        }

        return contents.getContainer().copy();
    }

    int findEmptySlot() {
        for (int canisterSlot = 0; canisterSlot < MAX_COUNT; canisterSlot++) {
            if (canisters[canisterSlot] != null) {
                continue;
            }

            return canisterSlot;
        }
        return -1;
    }

    int findLastOccupiedSlot() {
        for (int canisterSlot = MAX_COUNT - 1; canisterSlot >= 0; canisterSlot--) {
            if (canisters[canisterSlot] == null) {
                continue;
            }

            return canisterSlot;
        }
        return -1;
    }

    void setCanister(int canisterSlot, ItemStack canister) {
        canisters[canisterSlot] = normalizeCanister(canister);
        saveCanisterLayout();
    }

    void replaceCanisters(List<ItemStack> storedCanisters) {
        for (int tankIndex = 0; tankIndex < MAX_COUNT; tankIndex++) {
            ItemStack canister = tankIndex < storedCanisters.size() ? storedCanisters.get(tankIndex) : ItemStack.EMPTY;
            canisters[tankIndex] = normalizeCanister(canister);
        }
        saveCanisterLayout();
    }

    private void saveCanisterLayout() {
        save();
        boolean rightDown = hasCanister(0);
        boolean leftDown = hasCanister(1);
        boolean rightUp = hasCanister(2);
        boolean leftUp = hasCanister(3);
        int occupancyFlags = GasCanisterPackOverrides.calculateFlags(leftUp, rightUp, leftDown, rightDown);
        if (pack.getOrDefault(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, -1) == occupancyFlags) {
            return;
        }

        pack.set(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, occupancyFlags);
    }

    private boolean hasCanister(int canisterSlot) {
        GasCanisterContainerContents contents = canisters[canisterSlot];
        if (contents == null) {
            return false;
        }

        ItemStack canister = contents.getContainer();
        return CanisterContainerSuppliers.isValidGasCanister(canister) || CanisterContainerSuppliers.isValidCreativeGasCanister(canister);
    }

    private @Nullable GasCanisterContainerContents getCanisterContents(int tankIndex) {
        if (isInvalidTank(tankIndex)) {
            return null;
        }

        return canisters[tankIndex];
    }

    private @Unmodifiable List<ItemStack> copyCanisters() {
        return IntStream.range(0, MAX_COUNT).mapToObj(this::getCanister).toList();
    }
}
