package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import com.simibubi.create.foundation.gui.menu.MenuBase;
import com.simibubi.create.foundation.item.ItemHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AirtightUpgradableMenu extends MenuBase<ItemStack> {
    public static final int UPGRADE_SLOT_INDEX = 0;
    protected static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;
    private final InteractionHand sourceHand;
    protected InventoryHandler menuInventory;
    protected List<AirtightUpgradeStatus> currentStatusList;
    private int serverStateRevision;

    public AirtightUpgradableMenu(MenuType<?> type, int id, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        super(type, id, inventory, extraData);
        sourceHand = extraData.readEnum(InteractionHand.class);
    }

    public AirtightUpgradableMenu(MenuType<?> type, int id, Inventory inventory, ItemStack contentHolder) {
        this(type, id, inventory, contentHolder, InteractionHand.MAIN_HAND);
    }

    public AirtightUpgradableMenu(MenuType<?> type, int id, Inventory inventory, ItemStack contentHolder, InteractionHand sourceHand) {
        super(type, id, inventory, contentHolder);
        this.sourceHand = sourceHand;
    }

    public static void writeOpeningData(RegistryFriendlyByteBuf buffer, ItemStack contentHolder, InteractionHand sourceHand) {
        ItemStack.STREAM_CODEC.encode(buffer, contentHolder);
        buffer.writeEnum(sourceHand);
    }

    public static InventoryHandler getInventoryHandler(ItemStack stack, int slotCount) {
        ItemContainerContents contents = stack.get(CCBDataComponents.AIRTIGHT_UPGRADABLE_INVENTORY);
        InventoryHandler handler = new InventoryHandler(slotCount);
        if (contents == null) {
            return handler;
        }

        ItemHelper.fillItemStackHandler(contents, handler);
        return handler;
    }

    protected static List<AirtightUpgradeStatus> normalizeStatusList(List<AirtightUpgradeStatus> saved, List<AirtightUpgrade> upgrades) {
        Map<ResourceLocation, AirtightUpgradeStatus> byId = new HashMap<>();
        for (AirtightUpgradeStatus status : saved) {
            byId.put(status.id(), status);
        }

        List<AirtightUpgradeStatus> normalized = new ArrayList<>(upgrades.size());
        for (AirtightUpgrade upgrade : upgrades) {
            AirtightUpgradeStatus status = byId.get(upgrade.getID());
            if (status == null) {
                normalized.add(new AirtightUpgradeStatus(upgrade.getID(), upgrade.startsEnabled(), upgrade.startsInstalled()));
                continue;
            }

            boolean installed = status.isInstalled();
            boolean enabled = installed && status.isEnabled();
            normalized.add(new AirtightUpgradeStatus(upgrade.getID(), enabled, installed));
        }

        return normalized;
    }

    private int findStatusIndex(ResourceLocation id) {
        for (int i = 0; i < currentStatusList.size(); i++) {
            if (!currentStatusList.get(i).id().equals(id)) {
                continue;
            }

            return i;
        }
        return -1;
    }

    private boolean setStatus(AirtightUpgradeStatus status) {
        int index = findStatusIndex(status.id());
        if (index < 0) {
            return false;
        }

        currentStatusList.set(index, status);
        return true;
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
        ItemStack sourceStack = player.getItemInHand(sourceHand);
        if (player.level().isClientSide) {
            return ItemStack.isSameItem(sourceStack, contentHolder);
        }
        return sourceStack == contentHolder || ItemStack.isSameItemSameComponents(sourceStack, contentHolder);
    }

    @Nullable
    protected abstract AirtightUpgrade getUpgradeById(ResourceLocation id);

    protected abstract boolean isValidUpgrade(ItemStack stack);

    protected int getMaxSlots() {
        return 1;
    }

    public abstract void updateStatus(ItemStack stack);

    public AirtightUpgradeStatus getStatus(AirtightUpgrade upgrade) {
        int index = findStatusIndex(upgrade.getID());
        if (index < 0) {
            return new AirtightUpgradeStatus(upgrade.getID(), upgrade.startsEnabled(), upgrade.startsInstalled());
        }
        return currentStatusList.get(index);
    }

    public boolean tryInstallUpgrade(ResourceLocation id) {
        AirtightUpgrade upgrade = getUpgradeById(id);
        if (upgrade == null) {
            return false;
        }

        AirtightUpgradeStatus status = getStatus(upgrade);
        if (status.isInstalled() || findStatusIndex(id) < 0) {
            return false;
        }

        ItemStack stackInSlot = menuInventory.getStackInSlot(UPGRADE_SLOT_INDEX);
        if (stackInSlot.isEmpty() || !upgrade.testUpgradeItem(stackInSlot)) {
            return false;
        }

        menuInventory.extractItem(UPGRADE_SLOT_INDEX, 1, false);
        return setStatus(new AirtightUpgradeStatus(id, true, true));
    }

    public boolean tryToggleUpgrade(ResourceLocation id) {
        AirtightUpgrade upgrade = getUpgradeById(id);
        if (upgrade == null) {
            return false;
        }

        AirtightUpgradeStatus status = getStatus(upgrade);
        return status.isInstalled() && findStatusIndex(id) >= 0 && setStatus(new AirtightUpgradeStatus(id, !status.isEnabled(), true));
    }

    public void syncToClient(ServerPlayer player) {
        ItemStack upgradeStack = menuInventory.getStackInSlot(UPGRADE_SLOT_INDEX).copy();
        AirtightUpgradeMenuSyncPacket packet = new AirtightUpgradeMenuSyncPacket(containerId, List.copyOf(currentStatusList), upgradeStack);
        CatnipServices.NETWORK.sendToClient(player, packet);
    }

    public void applyServerState(List<AirtightUpgradeStatus> statuses, ItemStack upgradeStack) {
        currentStatusList = new ArrayList<>(statuses);
        menuInventory.setStackInSlot(UPGRADE_SLOT_INDEX, upgradeStack.copy());
        serverStateRevision++;
    }

    public int getServerStateRevision() {
        return serverStateRevision;
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
        ItemStack originalStack = slotStack.copy();
        if (slotIndex >= PLAYER_INVENTORY_SLOTS) {
            if (!moveItemStackTo(slotStack, 0, PLAYER_INVENTORY_SLOTS, true)) {
                return ItemStack.EMPTY;
            }
        }
        else {
            if (!menuInventory.getStackInSlot(UPGRADE_SLOT_INDEX).isEmpty() || !isValidUpgrade(slotStack)) {
                return ItemStack.EMPTY;
            }

            int upgradeSlot = PLAYER_INVENTORY_SLOTS + UPGRADE_SLOT_INDEX;
            if (!moveItemStackTo(slotStack, upgradeSlot, upgradeSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        }
        else {
            slot.setChanged();
        }

        if (slotStack.getCount() == originalStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, slotStack);
        return originalStack;
    }

    @Override
    public void clicked(int slotIndex, int dragType, ClickType clickType, Player player) {
        int selectedSlot = playerInventory.selected + PLAYER_INVENTORY_SLOTS - 9;
        if (slotIndex == selectedSlot && clickType != ClickType.THROW) {
            return;
        }

        super.clicked(slotIndex, dragType, clickType, player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container == playerInventory;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.container == playerInventory;
    }

    public List<AirtightUpgradeStatus> getCurrentStatusList() {
        return List.copyOf(currentStatusList);
    }

    public static class InventoryHandler extends ItemStackHandler {
        public InventoryHandler(int slotCount) {
            super(slotCount);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }
}
