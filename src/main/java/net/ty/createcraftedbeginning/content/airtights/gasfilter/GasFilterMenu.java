package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.gui.menu.IClearableMenu;
import com.simibubi.create.foundation.gui.menu.MenuBase;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.cansiters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GasFilterMenu extends MenuBase<ItemStack> implements IClearableMenu {
    protected ItemStackHandler filterInventory;
    protected boolean blacklist;

    public GasFilterMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public GasFilterMenu(MenuType<?> type, int id, Inventory inv, ItemStack stack) {
        super(type, id, inv, stack);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull GasFilterMenu create(int id, Inventory inv, ItemStack filter) {
        return new GasFilterMenu(CCBMenuTypes.GAS_FILTER_MENU.get(), id, inv, filter);
    }

    @Override
    protected ItemStack createOnClient(RegistryFriendlyByteBuf extraData) {
        return ItemStack.STREAM_CODEC.decode(extraData);
    }

    @Override
    protected void initAndReadInventory(ItemStack filter) {
        filterInventory = GasFilterItem.getGasFilterItemHandler(filter);
        blacklist = GasFilterItem.isBlacklist(filter);
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
    protected void saveData(@NotNull ItemStack filter) {
        filter.set(AllDataComponents.FILTER_ITEMS_BLACKLIST, blacklist);
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            if (!filterInventory.getStackInSlot(i).isEmpty()) {
                filter.set(AllDataComponents.FILTER_ITEMS, ItemHelper.containerContentsFromHandler(filterInventory));
                return;
            }
        }
        filter.remove(AllDataComponents.FILTER_ITEMS);
    }

    @Override
    public boolean stillValid(Player player) {
        return ItemStack.matches(playerInventory.getSelected(), contentHolder);
    }

    @Override
    public void clearContents() {
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            filterInventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index >= 36) {
            filterInventory.extractItem(index - 36, 1, false);
            getSlot(index).setChanged();
            return ItemStack.EMPTY;
        }

        ItemStack insert = slots.get(index).getItem();
        if (!mayPlace(insert)) {
            return ItemStack.EMPTY;
        }

        tryToInsert(insert);
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId < 36) {
            super.clicked(slotId, dragType, clickType, player);
            return;
        }

        if (isInSlot(slotId) && clickType != ClickType.THROW && clickType != ClickType.CLONE) {
            return;
        }
        if (clickType == ClickType.THROW || clickType == ClickType.CLONE) {
            return;
        }

        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            filterInventory.setStackInSlot(slotId - 36, ItemStack.EMPTY);
            getSlot(slotId).setChanged();
            return;
        }

        tryToInsert(carried);
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, @NotNull Slot slot) {
        return super.canTakeItemForPickAll(stack, slot) && !isInSlot(slot.index);
    }

    @Override
    protected boolean moveItemStackTo(@NotNull ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        return false;
    }

    @Override
    public boolean canDragTo(@NotNull Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    protected boolean isInSlot(int index) {
        return index >= 27 && index - 27 == playerInventory.selected;
    }

    protected boolean mayPlace(ItemStack stack) {
        return CanisterContainerSuppliers.isValidCanisterContainer(stack);
    }

    protected void tryToInsert(@NotNull ItemStack stack) {
        if (!CanisterContainerSuppliers.isValidCanisterContainer(stack)) {
            return;
        }

        IGasCanisterContainer container = stack.getCapability(GasHandler.ITEM);
        if (container == null) {
            return;
        }

        List<ItemStack> virtualList = container.getVirtualItems().stream().filter(virtual -> !virtual.isEmpty()).collect(Collectors.toCollection(ArrayList::new));
        if (virtualList.isEmpty()) {
            return;
        }

        List<Gas> existingGasTypes = GasFilterItem.getExistingGasTypes(filterInventory);
        List<ItemStack> toInsert = new ArrayList<>();
        virtualList.forEach(virtual -> {
            Gas virtualGasType = virtual.getOrDefault(CCBDataComponents.GAS_VIRTUAL_ITEM_TYPE, GasStack.EMPTY).getGasType();
            if (virtualGasType.isEmpty() || existingGasTypes.contains(virtualGasType)) {
                return;
            }

            toInsert.add(virtual);
        });
        if (toInsert.isEmpty()) {
            return;
        }

        int slotIndex = 0;
        int slots = filterInventory.getSlots();
        for (ItemStack virtual : toInsert) {
            while (slotIndex < slots && !filterInventory.getStackInSlot(slotIndex).isEmpty()) {
                slotIndex++;
            }
            if (slotIndex >= slots) {
                break;
            }

            filterInventory.insertItem(slotIndex, virtual, false);
            getSlot(slotIndex + 36).setChanged();
        }
    }
}
