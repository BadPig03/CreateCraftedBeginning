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
        GasFilterData data = GasFilterItem.getFilterData(filter);
        filterInventory = GasFilterItem.createFilterInventory(data);
        blacklist = data.blacklist();
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
        List<GasStack> gases = new ArrayList<>(filterInventory.getSlots());
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            GasStack gas = GasVirtualUtils.getGasType(filterInventory.getStackInSlot(i));
            if (gas.isEmpty()) {
                continue;
            }

            gases.add(gas);
        }

        GasFilterItem.setFilterData(filter, new GasFilterData(blacklist, gases));
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
    public ItemStack quickMoveStack(Player player, int index) {
        if (isHeldFilterSlot(index)) {
            return ItemStack.EMPTY;
        }

        if (index >= PLAYER_INVENTORY_SLOTS) {
            filterInventory.extractItem(index - PLAYER_INVENTORY_SLOTS, 1, false);
            getSlot(index).setChanged();
            return ItemStack.EMPTY;
        }

        ItemStack stack = slots.get(index).getItem();
        if (!mayPlace(stack)) {
            return ItemStack.EMPTY;
        }

        tryToInsert(stack);
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

        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            filterInventory.setStackInSlot(slotId - PLAYER_INVENTORY_SLOTS, ItemStack.EMPTY);
            getSlot(slotId).setChanged();
            return;
        }

        tryToInsert(carried);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return super.canTakeItemForPickAll(stack, slot) && !isHeldFilterSlot(slot.index);
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        return false;
    }

    @Override
    public boolean canDragTo(Slot slotIn) {
        return slotIn.container == playerInventory && !isHeldFilterSlot(slotIn.index);
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

    protected void tryToInsert(ItemStack stack) {
        if (!CanisterContainerSuppliers.isValidCanisterContainer(stack)) {
            return;
        }

        IGasCanisterContainer container = stack.getCapability(GasHandler.ITEM);
        if (container == null) {
            return;
        }

        for (ItemStack virtualItem : container.getVirtualItems()) {
            GasStack gas = GasVirtualUtils.getGasType(virtualItem);
            if (gas.isEmpty()) {
                continue;
            }

            int emptySlot = findFirstEmptySlot();
            if (emptySlot < 0) {
                return;
            }

            setGas(emptySlot, gas);
        }
    }

    protected boolean isHeldFilterSlot(int index) {
        return index >= 27 && index - 27 == playerInventory.selected;
    }

    protected boolean mayPlace(ItemStack stack) {
        return CanisterContainerSuppliers.isValidCanisterContainer(stack);
    }

    private int findFirstEmptySlot() {
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            if (!filterInventory.getStackInSlot(i).isEmpty()) {
                continue;
            }

            return i;
        }
        return -1;
    }

    private boolean containsGas(GasStack gas, int ignoredSlot) {
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            if (i == ignoredSlot) {
                continue;
            }

            GasStack existing = GasVirtualUtils.getGasType(filterInventory.getStackInSlot(i));
            if (existing.isEmpty() || !GasStack.isSameGasSameComponents(existing, gas)) {
                continue;
            }

            return true;
        }
        return false;
    }
}
