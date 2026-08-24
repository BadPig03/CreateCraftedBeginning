package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import com.simibubi.create.foundation.gui.menu.IClearableMenu;
import com.simibubi.create.foundation.gui.menu.MenuBase;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterUtils.GasFilterData;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasFilterMenu extends MenuBase<ItemStack> implements IClearableMenu {
    private static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

    protected ItemStackHandler filterInventory;
    protected boolean blacklist;
    protected boolean respectData;

    public GasFilterMenu(int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(CCBMenuTypes.GAS_FILTER_MENU.get(), id, inv, extraData);
    }

    public GasFilterMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public GasFilterMenu(MenuType<?> type, int id, Inventory inv, ItemStack stack) {
        super(type, id, inv, stack);
    }

    @Contract("_, _, _ -> new")
    public static GasFilterMenu create(int id, Inventory inv, ItemStack filter) {
        return new GasFilterMenu(CCBMenuTypes.GAS_FILTER_MENU.get(), id, inv, filter);
    }

    @Override
    protected ItemStack createOnClient(RegistryFriendlyByteBuf extraData) {
        return ItemStack.STREAM_CODEC.decode(extraData);
    }

    @Override
    protected void initAndReadInventory(ItemStack filter) {
        GasFilterData filterData = GasFilterItem.getFilterData(filter);
        filterInventory = GasFilterItem.createFilterInventory(filterData);
        blacklist = filterData.blacklist();
        respectData = filterData.respectData();
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(38, 121);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(filterInventory, col + row * 9, 23 + col * 18, 25 + row * 18));
            }
        }
    }

    @Override
    protected void saveData(ItemStack filter) {
        List<GasStack> configuredGases = new ArrayList<>(filterInventory.getSlots());
        for (int slotIndex = 0; slotIndex < filterInventory.getSlots(); slotIndex++) {
            GasStack configuredGas = GasVirtualUtils.getGasType(filterInventory.getStackInSlot(slotIndex));
            if (configuredGas.isEmpty()) {
                continue;
            }

            configuredGases.add(configuredGas);
        }

        GasFilterData filterData = new GasFilterData(blacklist, respectData, configuredGases);
        if (filterData.isDefault()) {
            filter.remove(CCBDataComponents.GAS_FILTER_DATA);
            return;
        }

        filter.set(CCBDataComponents.GAS_FILTER_DATA, filterData);
    }

    @Override
    public boolean stillValid(Player player) {
        return ItemStack.matches(playerInventory.getSelected(), contentHolder);
    }

    @Override
    public void clearContents() {
        for (int slotIndex = 0; slotIndex < filterInventory.getSlots(); slotIndex++) {
            filterInventory.setStackInSlot(slotIndex, ItemStack.EMPTY);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (isHeldFilterSlot(index)) {
            return ItemStack.EMPTY;
        }

        if (index >= PLAYER_INVENTORY_SLOTS) {
            filterInventory.extractItem(index - PLAYER_INVENTORY_SLOTS, 1, false);
            getSlot(index).setChanged();
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = slots.get(index).getItem();
        if (!mayPlace(sourceStack)) {
            return ItemStack.EMPTY;
        }

        tryToInsert(sourceStack);
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (isHeldFilterSlot(slotId) || clickType == ClickType.SWAP && dragType == playerInventory.selected) {
            return;
        }

        if (slotId < PLAYER_INVENTORY_SLOTS) {
            super.clicked(slotId, dragType, clickType, player);
            return;
        }

        if (clickType == ClickType.THROW || clickType == ClickType.CLONE) {
            return;
        }

        ItemStack carriedStack = getCarried();
        if (carriedStack.isEmpty()) {
            filterInventory.setStackInSlot(slotId - PLAYER_INVENTORY_SLOTS, ItemStack.EMPTY);
            getSlot(slotId).setChanged();
            return;
        }

        tryToInsert(carriedStack);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return super.canTakeItemForPickAll(stack, slot) && !isHeldFilterSlot(slot);
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        return false;
    }

    @Override
    public boolean canDragTo(Slot slotIn) {
        return slotIn.container == playerInventory && !isHeldFilterSlot(slotIn);
    }

    public void setGas(int slotIndex, GasStack gasStack) {
        if (slotIndex < 0 || slotIndex >= filterInventory.getSlots() || gasStack.isEmpty()) {
            return;
        }

        GasStack normalizedGas = gasStack.copyWithAmount(1);
        if (containsGas(normalizedGas, slotIndex)) {
            return;
        }

        ItemStack virtualItem = GasVirtualUtils.createVirtualItem(normalizedGas);
        if (virtualItem.isEmpty()) {
            return;
        }

        filterInventory.setStackInSlot(slotIndex, virtualItem);
        getSlot(slotIndex + PLAYER_INVENTORY_SLOTS).setChanged();
    }

    protected void tryToInsert(ItemStack canisterStack) {
        if (!CanisterContainerSuppliers.isValidCanisterContainer(canisterStack)) {
            return;
        }

        IGasCanisterContainer canisterContainer = canisterStack.getCapability(GasHandler.ITEM);
        if (canisterContainer == null) {
            return;
        }

        for (ItemStack virtualItem : canisterContainer.getVirtualItems()) {
            GasStack gasType = GasVirtualUtils.getGasType(virtualItem);
            if (gasType.isEmpty()) {
                continue;
            }

            int targetSlot = findFirstEmptySlot();
            if (targetSlot < 0) {
                return;
            }

            setGas(targetSlot, gasType);
        }
    }

    protected boolean isHeldFilterSlot(int slotId) {
        return slotId >= 0 && slotId < slots.size() && isHeldFilterSlot(slots.get(slotId));
    }

    protected boolean isHeldFilterSlot(Slot slot) {
        return slot.container == playerInventory && slot.index == playerInventory.selected;
    }

    protected boolean mayPlace(ItemStack stack) {
        return CanisterContainerSuppliers.isValidCanisterContainer(stack);
    }

    protected int findFirstEmptySlot() {
        for (int slotIndex = 0; slotIndex < filterInventory.getSlots(); slotIndex++) {
            if (!filterInventory.getStackInSlot(slotIndex).isEmpty()) {
                continue;
            }

            return slotIndex;
        }
        return -1;
    }

    protected boolean containsGas(GasStack gasType, int ignoredSlot) {
        for (int slotIndex = 0; slotIndex < filterInventory.getSlots(); slotIndex++) {
            if (slotIndex == ignoredSlot) {
                continue;
            }

            GasStack existingGas = GasVirtualUtils.getGasType(filterInventory.getStackInSlot(slotIndex));
            if (existingGas.isEmpty()) {
                continue;
            }

            boolean matchesGas = respectData ? GasStack.isSameGasSameComponents(existingGas, gasType) : GasStack.isSameGas(existingGas, gasType);
            if (!matchesGas) {
                continue;
            }

            return true;
        }
        return false;
    }
}
