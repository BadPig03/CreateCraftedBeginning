package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import com.simibubi.create.foundation.gui.menu.MenuBase;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.client.HotbarManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class AirtightUpgradableMenu extends MenuBase<ItemStack> {
    public static final int UPGRADE_SLOT_INDEX = 0;
    protected static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

    protected InventoryHandler menuInventory;
    protected List<AirtightUpgradeStatus> currentStatusList;

    public AirtightUpgradableMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public AirtightUpgradableMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder) {
        super(type, id, inv, contentHolder);
    }

    public static @NotNull InventoryHandler getInventoryHandler(@NotNull ItemStack itemStack, int maxSlot) {
        ItemContainerContents inventory = itemStack.get(CCBDataComponents.AIRTIGHT_UPGRADABLE_INVENTORY);
        InventoryHandler handler = new InventoryHandler(maxSlot);
        if (inventory == null) {
            return handler;
        }

        ItemHelper.fillItemStackHandler(inventory, handler);
        return handler;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected ItemStack createOnClient(RegistryFriendlyByteBuf extraData) {
        return ItemStack.STREAM_CODEC.decode(extraData);
    }

    @Override
    protected void initAndReadInventory(@NotNull ItemStack stack) {
        menuInventory = getInventoryHandler(stack, getMaxSlots());
        updateStatus(stack);
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(16, 160);
        addSlot(new SlotItemHandler(menuInventory, UPGRADE_SLOT_INDEX, 85, 77) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isValidUpgrade(stack);
            }

            @Override
            public int getMaxStackSize(@NotNull ItemStack stack) {
                return 1;
            }
        });
    }

    @Override
    protected void saveData(@NotNull ItemStack stack) {
        stack.set(CCBDataComponents.AIRTIGHT_UPGRADABLE_INVENTORY, ItemHelper.containerContentsFromHandler(menuInventory));
        stack.set(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, new ArrayList<>(currentStatusList));
    }

    @Override
    public boolean stillValid(Player player) {
        return ItemStack.isSameItem(playerInventory.getSelected(), contentHolder);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        if (slotIndex >= PLAYER_INVENTORY_SLOTS) {
            if (!moveItemStackTo(slotStack, 0, PLAYER_INVENTORY_SLOTS, true)) {
                slot.setChanged();
                return ItemStack.EMPTY;
            }
        }
        else {
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

        super.clicked(slotIndex, dragType, clickType, player);
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, @NotNull Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    @Override
    public boolean canDragTo(@NotNull Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    protected int getMaxSlots() {
        return 1;
    }

    protected void installUpgrade(@NotNull AirtightUpgrade upgrade) {
        currentStatusList.set(upgrade.getIndex(), new AirtightUpgradeStatus(upgrade.getID(), true, true));
    }

    protected void toggleUpgrade(@NotNull AirtightUpgrade upgrade) {
        int index = upgrade.getIndex();
        currentStatusList.set(index, new AirtightUpgradeStatus(upgrade.getID(), !currentStatusList.get(index).isEnabled(), true));
    }

    public InventoryHandler getMenuInventory() {
        return menuInventory;
    }

    public List<AirtightUpgradeStatus> getCurrentStatusList() {
        return currentStatusList;
    }

    public AirtightUpgradeStatus getStatus(@NotNull AirtightUpgrade upgrade) {
        return currentStatusList.get(upgrade.getIndex());
    }

    protected abstract boolean isValidUpgrade(@NotNull ItemStack stack);

    public abstract void updateStatus(@NotNull ItemStack stack);

    public static class InventoryHandler extends ItemStackHandler {
        public InventoryHandler(int maxSlot) {
            super(maxSlot);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }
}
