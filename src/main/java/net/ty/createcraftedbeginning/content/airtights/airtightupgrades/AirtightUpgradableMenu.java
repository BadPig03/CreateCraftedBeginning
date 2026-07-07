package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import com.simibubi.create.foundation.gui.menu.MenuBase;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.HotbarManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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

    public static InventoryHandler getInventoryHandler(ItemStack itemStack, int maxSlot) {
        ItemContainerContents inventory = itemStack.get(CCBDataComponents.AIRTIGHT_UPGRADABLE_INVENTORY);
        InventoryHandler handler = new InventoryHandler(maxSlot);
        if (inventory == null) {
            return handler;
        }

        ItemHelper.fillItemStackHandler(inventory, handler);
        return handler;
    }

    protected static List<AirtightUpgradeStatus> normalizeStatusList(List<AirtightUpgradeStatus> saved, List<AirtightUpgrade> upgrades) {
        Map<ResourceLocation, AirtightUpgradeStatus> byId = saved.stream().collect(Collectors.toMap(AirtightUpgradeStatus::id, status -> status, (a, b) -> b));
        List<AirtightUpgradeStatus> normalized = new ArrayList<>();
        for (AirtightUpgrade upgrade : upgrades) {
            AirtightUpgradeStatus old = byId.get(upgrade.getID());
            if (old == null) {
                normalized.add(new AirtightUpgradeStatus(upgrade.getID(), upgrade.startsEnabled(), upgrade.startsInstalled()));
                continue;
            }

            boolean installed = old.isInstalled();
            boolean enabled = installed && old.isEnabled();
            normalized.add(new AirtightUpgradeStatus(upgrade.getID(), enabled, installed));
        }

        return normalized;
    }

    private boolean isValidStatusSlot(AirtightUpgrade upgrade) {
        int index = upgrade.getIndex();
        if (index < 0 || index >= currentStatusList.size()) {
            return false;
        }

        AirtightUpgradeStatus status = currentStatusList.get(index);
        return status.id().equals(upgrade.getID());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected ItemStack createOnClient(RegistryFriendlyByteBuf extraData) {
        return ItemStack.STREAM_CODEC.decode(extraData);
    }

    @Override
    protected void initAndReadInventory(ItemStack stack) {
        menuInventory = getInventoryHandler(stack, getMaxSlots());
        updateStatus(stack);
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(16, 160);
        addSlot(new SlotItemHandler(menuInventory, UPGRADE_SLOT_INDEX, 85, 77) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isValidUpgrade(stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });
    }

    @Override
    protected void saveData(ItemStack stack) {
        stack.set(CCBDataComponents.AIRTIGHT_UPGRADABLE_INVENTORY, ItemHelper.containerContentsFromHandler(menuInventory));
        stack.set(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, new ArrayList<>(currentStatusList));
    }

    @Override
    public boolean stillValid(Player player) {
        return ItemStack.isSameItem(playerInventory.getSelected(), contentHolder);
    }

    @Nullable
    protected abstract AirtightUpgrade getUpgradeById(ResourceLocation id);

    protected abstract boolean isValidUpgrade(ItemStack stack);

    protected int getMaxSlots() {
        return 1;
    }

    protected void installUpgrade(AirtightUpgrade upgrade) {
        currentStatusList.set(upgrade.getIndex(), new AirtightUpgradeStatus(upgrade.getID(), true, true));
    }

    protected void toggleUpgrade(AirtightUpgrade upgrade) {
        int index = upgrade.getIndex();
        if (index < 0 || index >= currentStatusList.size()) {
            return;
        }

        AirtightUpgradeStatus status = currentStatusList.get(index);
        if (!status.isInstalled()) {
            return;
        }

        currentStatusList.set(index, new AirtightUpgradeStatus(upgrade.getID(), !status.isEnabled(), true));
    }

    public abstract void updateStatus(ItemStack stack);

    public AirtightUpgradeStatus getStatus(AirtightUpgrade upgrade) {
        return currentStatusList.get(upgrade.getIndex());
    }

    public boolean tryInstallUpgrade(ResourceLocation id) {
        AirtightUpgrade upgrade = getUpgradeById(id);
        if (upgrade == null || !isValidStatusSlot(upgrade)) {
            return false;
        }

        int index = upgrade.getIndex();
        AirtightUpgradeStatus status = currentStatusList.get(index);
        if (status.isInstalled()) {
            return false;
        }

        ItemStack stackInSlot = menuInventory.getStackInSlot(UPGRADE_SLOT_INDEX);
        if (stackInSlot.isEmpty() || !stackInSlot.is(upgrade.getUpgradeItem()) || !upgrade.testUpgradeItem(stackInSlot)) {
            return false;
        }

        menuInventory.extractItem(UPGRADE_SLOT_INDEX, 1, false);
        currentStatusList.set(index, new AirtightUpgradeStatus(upgrade.getID(), true, true));
        return true;
    }

    public boolean tryToggleUpgrade(ResourceLocation id) {
        AirtightUpgrade upgrade = getUpgradeById(id);
        if (upgrade == null || !isValidStatusSlot(upgrade)) {
            return false;
        }

        int index = upgrade.getIndex();
        AirtightUpgradeStatus status = currentStatusList.get(index);
        if (!status.isInstalled()) {
            return false;
        }

        currentStatusList.set(index, new AirtightUpgradeStatus(upgrade.getID(), !status.isEnabled(), true));
        return true;
    }

    public InventoryHandler getMenuInventory() {
        return menuInventory;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
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
    public void clicked(int slotIndex, int dragType, ClickType clickType, Player player) {
        if (slotIndex == playerInventory.selected + PLAYER_INVENTORY_SLOTS - HotbarManager.NUM_HOTBAR_GROUPS && clickType != ClickType.THROW) {
            return;
        }

        super.clicked(slotIndex, dragType, clickType, player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    @Override
    public boolean canDragTo(Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    public List<AirtightUpgradeStatus> getCurrentStatusList() {
        return currentStatusList;
    }

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
