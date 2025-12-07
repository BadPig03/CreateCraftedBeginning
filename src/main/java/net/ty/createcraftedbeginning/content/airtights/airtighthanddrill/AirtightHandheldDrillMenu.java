package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.simibubi.create.foundation.gui.menu.MenuBase;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.client.HotbarManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

public class AirtightHandheldDrillMenu extends MenuBase<ItemStack> {
    public static final int FILTER_SLOT_INDEX = SlotType.FILTER.getIndex();
    public static final int UPGRADE_SLOT_INDEX = SlotType.UPGRADE.getIndex();

    public static final int FILTER_DISABLED_FLAG = OptionFlags.FILTER_DISABLED.getFlag();
    public static final int CONTAINER_DISABLED_FLAG = OptionFlags.CONTAINER_DISABLED.getFlag();
    public static final int DRILL_ATTACK_DISABLED_FLAG = OptionFlags.DRILL_ATTACK_DISABLED.getFlag();
    public static final int OUTLINE_DISABLED_FLAG = OptionFlags.OUTLINE_DISABLED.getFlag();
    public static final int SILK_TOUCH_ENABLED_FLAG = OptionFlags.SILK_TOUCH_ENABLED.getFlag();
    public static final int MAGNET_ENABLED_FLAG = OptionFlags.MAGNET_ENABLED.getFlag();
    public static final int CONVERSION_ENABLED_FLAG = OptionFlags.CONVERSION_ENABLED.getFlag();
    public static final int LIQUID_REPLACEMENT_ENABLED_FLAG = OptionFlags.LIQUID_REPLACEMENT_ENABLED.getFlag();
    public static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

    private static final int PLAYER_SLOT_X = 41;
    private static final int PLAYER_SLOT_Y = 160;
    private static final int FILTER_SLOT_X = 17;
    private static final int FILTER_SLOT_Y = 115;
    private static final int UPGRADE_SLOT_X = 152;
    private static final int UPGRADE_SLOT_Y = 36;

    protected DrillItemHandler drillInventory;
    protected boolean filterDisabled;
    protected boolean containerDisabled;
    protected boolean drillAttackDisabled;
    protected boolean outlineDisabled;

    protected boolean silkTouchInstalled;
    protected boolean magnetInstalled;
    protected boolean conversionInstalled;
    protected boolean liquidReplacementInstalled;

    protected boolean silkTouchEnabled;
    protected boolean magnetEnabled;
    protected boolean conversionEnabled;
    protected boolean liquidReplacementEnabled;

    public AirtightHandheldDrillMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public AirtightHandheldDrillMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder) {
        super(type, id, inv, contentHolder);
    }

    public static int getDefaultFlags() {
        return OptionFlags.FILTER_DISABLED.getFlag() | OptionFlags.CONTAINER_DISABLED.getFlag();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected ItemStack createOnClient(RegistryFriendlyByteBuf extraData) {
        return ItemStack.STREAM_CODEC.decode(extraData);
    }

    @Override
    protected void initAndReadInventory(@NotNull ItemStack drill) {
        drillInventory = AirtightHandheldDrillUtils.getInventoryHandler(drill);
        updateFlags(drill.getOrDefault(CCBDataComponents.DRILL_OPTION_FLAGS, getDefaultFlags()));
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(PLAYER_SLOT_X, PLAYER_SLOT_Y);
        addSlot(SlotType.FILTER.getSlot(this, drillInventory, FILTER_SLOT_INDEX, FILTER_SLOT_X, FILTER_SLOT_Y));
        addSlot(SlotType.UPGRADE.getSlot(this, drillInventory, UPGRADE_SLOT_INDEX, UPGRADE_SLOT_X, UPGRADE_SLOT_Y));
    }

    @Override
    protected void saveData(@NotNull ItemStack drill) {
        drill.set(CCBDataComponents.DRILL_INVENTORY, ItemHelper.containerContentsFromHandler(drillInventory));
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
            int drillSlotIndex = slotIndex - PLAYER_INVENTORY_SLOTS;
            if (drillSlotIndex == FILTER_SLOT_INDEX) {
                drillInventory.setStackInSlot(FILTER_SLOT_INDEX, ItemStack.EMPTY);
                slot.setChanged();
                return ItemStack.EMPTY;
            }
            else if (!moveItemStackTo(slotStack, 0, PLAYER_INVENTORY_SLOTS, true)) {
                slot.setChanged();
                return ItemStack.EMPTY;
            }
        }
        else {
            if (drillInventory.getStackInSlot(FILTER_SLOT_INDEX).isEmpty() && AirtightHandheldDrillUtils.isValidFilter(slotStack)) {
                drillInventory.setStackInSlot(FILTER_SLOT_INDEX, slotStack.copyWithCount(1));
                slot.setChanged();
                return ItemStack.EMPTY;
            }

            if (drillInventory.getStackInSlot(UPGRADE_SLOT_INDEX).isEmpty() && isValidUpgrade(slotStack)) {
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
    public void clicked(int slotId, int dragType, @NotNull ClickType clickTypeIn, @NotNull Player player) {
        if (slotId == playerInventory.selected + PLAYER_INVENTORY_SLOTS - HotbarManager.NUM_HOTBAR_GROUPS && clickTypeIn != ClickType.THROW) {
            return;
        }

        if (slotId < PLAYER_INVENTORY_SLOTS) {
            super.clicked(slotId, dragType, clickTypeIn, player);
            return;
        }

        int drillSlotId = slotId - PLAYER_INVENTORY_SLOTS;
        if (drillSlotId != FILTER_SLOT_INDEX) {
            super.clicked(slotId, dragType, clickTypeIn, player);
            return;
        }

        handleFilterSlotClick(clickTypeIn, player, slotId);
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, @NotNull Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    @Override
    public boolean canDragTo(@NotNull Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    public DrillItemHandler getDrillInventory() {
        return drillInventory;
    }

    public void updateFlags(int mask) {
        filterDisabled = OptionFlags.FILTER_DISABLED.getOptionFlag(mask);
        containerDisabled = OptionFlags.CONTAINER_DISABLED.getOptionFlag(mask);
        drillAttackDisabled = OptionFlags.DRILL_ATTACK_DISABLED.getOptionFlag(mask);
        outlineDisabled = OptionFlags.OUTLINE_DISABLED.getOptionFlag(mask);
        silkTouchInstalled = OptionFlags.SILK_TOUCH_INSTALLED.getOptionFlag(mask);
        silkTouchEnabled = OptionFlags.SILK_TOUCH_ENABLED.getOptionFlag(mask);
        magnetInstalled = OptionFlags.MAGNET_INSTALLED.getOptionFlag(mask);
        magnetEnabled = OptionFlags.MAGNET_ENABLED.getOptionFlag(mask);
        conversionInstalled = OptionFlags.CONVERSION_INSTALLED.getOptionFlag(mask);
        conversionEnabled = OptionFlags.CONVERSION_ENABLED.getOptionFlag(mask);
        liquidReplacementInstalled = OptionFlags.LIQUID_REPLACEMENT_INSTALLED.getOptionFlag(mask);
        liquidReplacementEnabled = OptionFlags.LIQUID_REPLACEMENT_ENABLED.getOptionFlag(mask);
    }

    protected int saveFlagsToInt() {
        int mask = 0;
        mask = OptionFlags.FILTER_DISABLED.setOptionFlag(mask, filterDisabled);
        mask = OptionFlags.CONTAINER_DISABLED.setOptionFlag(mask, containerDisabled);
        mask = OptionFlags.DRILL_ATTACK_DISABLED.setOptionFlag(mask, drillAttackDisabled);
        mask = OptionFlags.OUTLINE_DISABLED.setOptionFlag(mask, outlineDisabled);
        mask = OptionFlags.SILK_TOUCH_INSTALLED.setOptionFlag(mask, silkTouchInstalled);
        mask = OptionFlags.SILK_TOUCH_ENABLED.setOptionFlag(mask, silkTouchEnabled);
        mask = OptionFlags.MAGNET_INSTALLED.setOptionFlag(mask, magnetInstalled);
        mask = OptionFlags.MAGNET_ENABLED.setOptionFlag(mask, magnetEnabled);
        mask = OptionFlags.CONVERSION_INSTALLED.setOptionFlag(mask, conversionInstalled);
        mask = OptionFlags.CONVERSION_ENABLED.setOptionFlag(mask, conversionEnabled);
        mask = OptionFlags.LIQUID_REPLACEMENT_INSTALLED.setOptionFlag(mask, liquidReplacementInstalled);
        mask = OptionFlags.LIQUID_REPLACEMENT_ENABLED.setOptionFlag(mask, liquidReplacementEnabled);
        return mask;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isValidUpgrade(@NotNull ItemStack stack) {
        if (stack.is(AirtightHandheldDrillUtils.SILK_TOUCH_UPGRADE_ITEM) && silkTouchInstalled) {
            return false;
        }
        if (stack.is(AirtightHandheldDrillUtils.MAGNET_UPGRADE_ITEM) && magnetInstalled) {
            return false;
        }
        return (!stack.is(AirtightHandheldDrillUtils.CONVERSION_UPGRADE_ITEM) || !conversionInstalled) && (!stack.is(AirtightHandheldDrillUtils.LIQUID_REPLACEMENT_UPGRADE_ITEM) || !liquidReplacementInstalled) && (stack.is(AirtightHandheldDrillUtils.SILK_TOUCH_UPGRADE_ITEM) || stack.is(AirtightHandheldDrillUtils.MAGNET_UPGRADE_ITEM) || stack.is(AirtightHandheldDrillUtils.CONVERSION_UPGRADE_ITEM) || stack.is(AirtightHandheldDrillUtils.LIQUID_REPLACEMENT_UPGRADE_ITEM));
    }

    private void handleFilterSlotClick(@NotNull ClickType clickTypeIn, Player player, int slotId) {
        ItemStack carried = getCarried();
        ItemStack filterSlotItem = drillInventory.getStackInSlot(FILTER_SLOT_INDEX);
        switch (clickTypeIn) {
            case CLONE -> {
                if (player.hasInfiniteMaterials() && carried.isEmpty() && !filterSlotItem.isEmpty()) {
                    setCarried(filterSlotItem.copyWithCount(filterSlotItem.getOrDefault(DataComponents.MAX_STACK_SIZE, 64)));
                }
            }
            case PICKUP -> {
                Slot filterSlot = getSlot(slotId);
                if (!carried.isEmpty() && filterSlot.mayPlace(carried)) {
                    ItemStack insert = carried.copyWithCount(1);
                    drillInventory.setStackInSlot(FILTER_SLOT_INDEX, insert);
                    getSlot(slotId).setChanged();
                }
                else if (carried.isEmpty()) {
                    drillInventory.setStackInSlot(FILTER_SLOT_INDEX, ItemStack.EMPTY);
                    getSlot(slotId).setChanged();
                }
            }
            case QUICK_MOVE -> {
                if (!filterSlotItem.isEmpty()) {
                    drillInventory.setStackInSlot(FILTER_SLOT_INDEX, ItemStack.EMPTY);
                    getSlot(slotId).setChanged();
                }
            }
        }
    }

    protected enum SlotType {
        FILTER(0),
        UPGRADE(1);

        private final int index;

        SlotType(int index) {
            this.index = index;
        }

        private int getIndex() {
            return index;
        }

        private @NotNull SlotItemHandler getSlot(AirtightHandheldDrillMenu menu, IItemHandler itemHandler, int index, int x, int y) {
            return switch (this) {
                case FILTER -> new SlotItemHandler(itemHandler, index, x, y) {
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
                };
                case UPGRADE -> new SlotItemHandler(itemHandler, index, x, y) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return menu.isValidUpgrade(stack);
                    }

                    @Override
                    public int getMaxStackSize(@NotNull ItemStack stack) {
                        return 1;
                    }
                };
            };
        }
    }

    protected enum OptionFlags {
        FILTER_DISABLED(1),
        CONTAINER_DISABLED(2),
        DRILL_ATTACK_DISABLED(4),
        OUTLINE_DISABLED(8),
        SILK_TOUCH_INSTALLED(16),
        MAGNET_INSTALLED(32),
        CONVERSION_INSTALLED(64),
        LIQUID_REPLACEMENT_INSTALLED(128),
        SILK_TOUCH_ENABLED(256),
        MAGNET_ENABLED(512),
        CONVERSION_ENABLED(1024),
        LIQUID_REPLACEMENT_ENABLED(2048);

        private final int flag;

        OptionFlags(int flag) {
            this.flag = flag;
        }

        private int getFlag() {
            return flag;
        }

        private int setOptionFlag(int currentFlags, boolean enabled) {
            return enabled ? currentFlags | flag : currentFlags & ~flag;
        }

        private boolean getOptionFlag(int currentFlags) {
            return (currentFlags & flag) != 0;
        }
    }

    public static class DrillItemHandler extends ItemStackHandler {
        public DrillItemHandler() {
            super(2);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }
}
