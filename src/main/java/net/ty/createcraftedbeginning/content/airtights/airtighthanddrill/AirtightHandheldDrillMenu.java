package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
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
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightHandheldDrillMenu extends AirtightUpgradableMenu {
    public static final int UPGRADE_SLOT_INDEX = 0;
    public static final int MAX_SLOTS = 2;
    static final int FILTER_SLOT_INDEX = 1;

    public AirtightHandheldDrillMenu(int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(CCBMenuTypes.AIRTIGHT_HANDHELD_DRILL_MENU.get(), id, inv, extraData);
    }

    private AirtightHandheldDrillMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    AirtightHandheldDrillMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder, InteractionHand sourceHand) {
        super(type, id, inv, contentHolder, sourceHand);
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(41, 160);
        addSlot(new SlotItemHandler(menuInventory, UPGRADE_SLOT_INDEX, 152, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isValidUpgrade(stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });
        addSlot(new SlotItemHandler(menuInventory, FILTER_SLOT_INDEX, 17, 115) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AirtightHandheldDrillUtils.isValidFilter(stack);
            }

            @Override
            public void set(ItemStack stack) {
                if (!stack.isEmpty()) {
                    stack = stack.copyWithCount(1);
                }
                super.set(stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });
    }

    @Override
    protected @Nullable AirtightUpgrade getUpgradeById(ResourceLocation id) {
        return AirtightHandheldDrillUpgradeRegistry.getByID(id);
    }

    @Override
    protected boolean isValidUpgrade(ItemStack stack) {
        AirtightUpgrade upgrade = AirtightHandheldDrillUpgradeRegistry.getByStack(stack);
        return upgrade != null && !getStatus(upgrade).isInstalled();
    }

    @Override
    protected int getMaxSlots() {
        return MAX_SLOTS;
    }

    @Override
    public void updateStatus(ItemStack stack) {
        currentStatusList = stack.get(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS);
        if (currentStatusList == null) {
            currentStatusList = AirtightHandheldDrillUpgradeRegistry.getDefaultUpgradeList();
        }
        currentStatusList = normalizeStatusList(currentStatusList, AirtightHandheldDrillUpgradeRegistry.getAll());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        if (slotIndex >= PLAYER_INVENTORY_SLOTS) {
            int menuSlotIndex = slotIndex - PLAYER_INVENTORY_SLOTS;
            if (menuSlotIndex == FILTER_SLOT_INDEX) {
                menuInventory.setStackInSlot(FILTER_SLOT_INDEX, ItemStack.EMPTY);
                slot.setChanged();
                return ItemStack.EMPTY;
            }

            if (!moveItemStackTo(stack, 0, PLAYER_INVENTORY_SLOTS, true)) {
                slot.setChanged();
            }
            return ItemStack.EMPTY;
        }

        if (menuInventory.getStackInSlot(FILTER_SLOT_INDEX).isEmpty() && AirtightHandheldDrillUtils.isValidFilter(stack)) {
            menuInventory.setStackInSlot(FILTER_SLOT_INDEX, stack.copyWithCount(1));
            slot.setChanged();
            return ItemStack.EMPTY;
        }

        if (!menuInventory.getStackInSlot(UPGRADE_SLOT_INDEX).isEmpty() || !isValidUpgrade(stack)) {
            return ItemStack.EMPTY;
        }

        int upgradeSlot = PLAYER_INVENTORY_SLOTS + UPGRADE_SLOT_INDEX;
        if (!moveItemStackTo(stack, upgradeSlot, upgradeSlot + 1, false)) {
            return ItemStack.EMPTY;
        }

        slot.setChanged();
        return stack;
    }

    @Override
    public void clicked(int slotIndex, int dragType, ClickType clickType, Player player) {
        int selectedSlot = playerInventory.selected + PLAYER_INVENTORY_SLOTS - 9;
        if (slotIndex == selectedSlot && clickType != ClickType.THROW) {
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
                if (!player.hasInfiniteMaterials() || !carried.isEmpty() || filterItem.isEmpty()) {
                    return;
                }

                setCarried(filterItem.copyWithCount(filterItem.getOrDefault(DataComponents.MAX_STACK_SIZE, 64)));
            }
            case PICKUP -> {
                Slot filterSlot = getSlot(slotIndex);
                if (!carried.isEmpty() && filterSlot.mayPlace(carried)) {
                    menuInventory.setStackInSlot(FILTER_SLOT_INDEX, carried.copyWithCount(1));
                    filterSlot.setChanged();
                    return;
                }

                if (!carried.isEmpty()) {
                    return;
                }

                menuInventory.setStackInSlot(FILTER_SLOT_INDEX, ItemStack.EMPTY);
                filterSlot.setChanged();
            }
            case QUICK_MOVE -> {
                if (filterItem.isEmpty()) {
                    return;
                }

                menuInventory.setStackInSlot(FILTER_SLOT_INDEX, ItemStack.EMPTY);
                getSlot(slotIndex).setChanged();
            }
        }
    }
}
