package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import com.simibubi.create.foundation.gui.menu.MenuBase;
import net.minecraft.client.HotbarManager;
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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterQueryUtils;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackOverrides.GasCanisterPackType;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GasCanisterPackMenu extends MenuBase<ItemStack> {
    public static final int I_SLOT_INDEX = SlotType.I.getIndex();
    public static final int II_SLOT_INDEX = SlotType.II.getIndex();
    public static final int III_SLOT_INDEX = SlotType.III.getIndex();
    public static final int IV_SLOT_INDEX = SlotType.IV.getIndex();
    public static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

    private static final int PLAYER_SLOT_X = 20;
    private static final int PLAYER_SLOT_Y = 166;
    private static final int SLOT_Y = 89;
    private static final int I_SLOT_X = 23;
    private static final int II_SLOT_X = 65;
    private static final int III_SLOT_X = 107;
    private static final int IV_SLOT_X = 149;

    protected PackItemHandler packInventory;

    public GasCanisterPackMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public GasCanisterPackMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder) {
        super(type, id, inv, contentHolder);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected ItemStack createOnClient(RegistryFriendlyByteBuf extraData) {
        return ItemStack.STREAM_CODEC.decode(extraData);
    }

    @Override
    protected void initAndReadInventory(ItemStack pack) {
        UUID uuid = GasCanisterPackUtils.getCanisterPackUUID(pack);
        PackItemHandler handler = new PackItemHandler();
        CreateCraftedBeginning.GAS_CANISTER_PACK_CONTENTS_DATA_MANAGER.getContents(uuid).fillItemStackHandler(handler);
        packInventory = handler;
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(PLAYER_SLOT_X, PLAYER_SLOT_Y);
        addSlot(SlotType.getSlot(packInventory, I_SLOT_INDEX, I_SLOT_X));
        addSlot(SlotType.getSlot(packInventory, II_SLOT_INDEX, II_SLOT_X));
        addSlot(SlotType.getSlot(packInventory, III_SLOT_INDEX, III_SLOT_X));
        addSlot(SlotType.getSlot(packInventory, IV_SLOT_INDEX, IV_SLOT_X));
    }

    @Override
    protected void saveData(@NotNull ItemStack pack) {
        pack.set(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, getPackType());
        UUID uuid = GasCanisterPackUtils.getCanisterPackUUID(pack);
        GasCanisterPackContents contents = GasCanisterPackContents.fromItemStackHandler(uuid, packInventory);
        CreateCraftedBeginning.GAS_CANISTER_PACK_CONTENTS_DATA_MANAGER.addContents(uuid, contents);
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
        ItemStack originalStack = slotStack.copy();
        if (slotIndex >= PLAYER_INVENTORY_SLOTS) {
            if (!moveItemStackTo(slotStack, 0, PLAYER_INVENTORY_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }
        else {
            if (!moveItemStackTo(slotStack, PLAYER_INVENTORY_SLOTS, PLAYER_INVENTORY_SLOTS + 4, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        }
        else {
            slot.setChanged();
        }

        if (!player.level().isClientSide) {
            saveData(contentHolder);
        }
        return originalStack;
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickTypeIn, @NotNull Player player) {
        if (slotId == playerInventory.selected + PLAYER_INVENTORY_SLOTS - HotbarManager.NUM_HOTBAR_GROUPS && clickTypeIn != ClickType.THROW) {
            return;
        }

        super.clicked(slotId, dragType, clickTypeIn, player);
        if (player.level().isClientSide) {
            return;
        }

        saveData(contentHolder);
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, @NotNull Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    @Override
    public void setItem(int slotId, int stateId, @NotNull ItemStack stack) {
        super.setItem(slotId, stateId, stack);
        if (playerInventory.player.level().isClientSide) {
            return;
        }

        saveData(contentHolder);
    }

    @Override
    public boolean canDragTo(@NotNull Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    private int getPackType() {
        boolean rightDown = GasCanisterQueryUtils.isValidCanister(packInventory.getStackInSlot(I_SLOT_INDEX));
        boolean leftDown = GasCanisterQueryUtils.isValidCanister(packInventory.getStackInSlot(II_SLOT_INDEX));
        boolean rightUp = GasCanisterQueryUtils.isValidCanister(packInventory.getStackInSlot(III_SLOT_INDEX));
        boolean leftUp = GasCanisterQueryUtils.isValidCanister(packInventory.getStackInSlot(IV_SLOT_INDEX));
        int flags = GasCanisterPackOverrides.calculateFlags(leftUp, rightUp, leftDown, rightDown);
        return GasCanisterPackType.getTypeFromFlags(flags).ordinal();
    }

    protected enum SlotType {
        I(0),
        II(1),
        III(2),
        IV(3);

        private final int index;

        SlotType(int index) {
            this.index = index;
        }

        private static @NotNull SlotItemHandler getSlot(IItemHandler itemHandler, int index, int x) {
            return new SlotItemHandler(itemHandler, index, x, SLOT_Y) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return GasCanisterQueryUtils.isValidCanister(stack);
                }

                @Override
                public int getMaxStackSize(@NotNull ItemStack stack) {
                    return 1;
                }
            };
        }

        private int getIndex() {
            return index;
        }
    }

    public static class PackItemHandler extends ItemStackHandler {
        public PackItemHandler() {
            super(4);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }
}
