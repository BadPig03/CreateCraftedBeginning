package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import net.minecraft.client.HotbarManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.AirtightHandheldDrillUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class AirtightHandheldDrillMenu extends AirtightUpgradableMenu {
    public static final int UPGRADE_SLOT_INDEX = 0;
    public static final int FILTER_SLOT_INDEX = 1;
    public static final int MAX_SLOTS = 2;

    public AirtightHandheldDrillMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public AirtightHandheldDrillMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder) {
        super(type, id, inv, contentHolder);
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(41, 160);
        addSlot(new SlotItemHandler(menuInventory, UPGRADE_SLOT_INDEX, 152, 36) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isValidUpgrade(stack);
            }

            @Override
            public int getMaxStackSize(@NotNull ItemStack stack) {
                return 1;
            }
        });
        addSlot(new SlotItemHandler(menuInventory, FILTER_SLOT_INDEX, 17, 115) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return AirtightHandheldDrillUtils.isValidFilter(stack);
            }

            @Override
            public void set(@NotNull ItemStack stack) {
                if (!stack.isEmpty()) {
                    stack = stack.copyWithCount(1);
                }
                super.set(stack);
            }

            @Override
            public int getMaxStackSize(@NotNull ItemStack stack) {
                return 1;
            }
        });
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        if (slotIndex >= PLAYER_INVENTORY_SLOTS) {
            int menuSlotIndex = slotIndex - PLAYER_INVENTORY_SLOTS;
            if (menuSlotIndex == FILTER_SLOT_INDEX) {
                menuInventory.setStackInSlot(FILTER_SLOT_INDEX, ItemStack.EMPTY);
                slot.setChanged();
                return ItemStack.EMPTY;
            }
            else if (!moveItemStackTo(slotStack, 0, PLAYER_INVENTORY_SLOTS, true)) {
                slot.setChanged();
                return ItemStack.EMPTY;
            }
        }
        else {
            if (menuInventory.getStackInSlot(FILTER_SLOT_INDEX).isEmpty() && AirtightHandheldDrillUtils.isValidFilter(slotStack)) {
                menuInventory.setStackInSlot(FILTER_SLOT_INDEX, slotStack.copyWithCount(1));
                slot.setChanged();
                return ItemStack.EMPTY;
            }

            if (menuInventory.getStackInSlot(UPGRADE_SLOT_INDEX).isEmpty() && isValidUpgrade(slotStack)) {
                if (moveItemStackTo(slotStack, PLAYER_INVENTORY_SLOTS + UPGRADE_SLOT_INDEX, PLAYER_INVENTORY_SLOTS + UPGRADE_SLOT_INDEX + 1, false)) {
                    slot.setChanged();
                    return slotStack;
                }
                return ItemStack.EMPTY;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotIndex, int dragType, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotIndex == playerInventory.selected + PLAYER_INVENTORY_SLOTS - HotbarManager.NUM_HOTBAR_GROUPS && clickType != ClickType.THROW) {
            return;
        }

        if (slotIndex - PLAYER_INVENTORY_SLOTS != FILTER_SLOT_INDEX) {
            super.clicked(slotIndex, dragType, clickType, player);
            return;
        }

        ItemStack carried = getCarried();
        ItemStack filterItem = menuInventory.getStackInSlot(FILTER_SLOT_INDEX);
        switch (clickType) {
            case CLONE -> {
                if (player.hasInfiniteMaterials() && carried.isEmpty() && !filterItem.isEmpty()) {
                    setCarried(filterItem.copyWithCount(filterItem.getOrDefault(DataComponents.MAX_STACK_SIZE, 64)));
                }
            }
            case PICKUP -> {
                Slot filterSlot = getSlot(slotIndex);
                if (!carried.isEmpty() && filterSlot.mayPlace(carried)) {
                    ItemStack insert = carried.copyWithCount(1);
                    menuInventory.setStackInSlot(FILTER_SLOT_INDEX, insert);
                    getSlot(slotIndex).setChanged();
                }
                else if (carried.isEmpty()) {
                    menuInventory.setStackInSlot(FILTER_SLOT_INDEX, ItemStack.EMPTY);
                    getSlot(slotIndex).setChanged();
                }
            }
            case QUICK_MOVE -> {
                if (!filterItem.isEmpty()) {
                    menuInventory.setStackInSlot(FILTER_SLOT_INDEX, ItemStack.EMPTY);
                    getSlot(slotIndex).setChanged();
                }
            }
        }
    }

    @Override
    protected int getMaxSlots() {
        return MAX_SLOTS;
    }

    @Override
    protected boolean isValidUpgrade(@NotNull ItemStack stack) {
        AirtightUpgrade upgrade = AirtightHandheldDrillUpgradeRegistry.getByItem(stack.getItem());
        return upgrade != null && !currentStatusList.get(upgrade.getIndex()).isInstalled();
    }

    @Override
    public void updateStatus(@NotNull ItemStack stack) {
        currentStatusList = new ArrayList<>(stack.getOrDefault(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, AirtightHandheldDrillUpgradeRegistry.getDefaultUpgradeList()));
    }
}
